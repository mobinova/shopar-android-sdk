package app.shopar.sdk.agency.internal

import app.shopar.sdk.proto.creator.v1.ConfirmAgencySessionRequest
import app.shopar.sdk.proto.creator.v1.RefreshAgencySessionRequest
import app.shopar.sdk.proto.creator.v1.StartAgencySessionRequest
import app.shopar.sdk.agency.AgencyConfiguration
import app.shopar.sdk.agency.AgencyCreatorSession
import app.shopar.sdk.agency.toPublicSession
import com.google.protobuf.Empty
import io.grpc.Status
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class AgencyLoginChallenge(val requestId: String, val expiresInSeconds: Int)

internal class AgencySessionManager(
    private val configuration: AgencyConfiguration,
    private val transport: AgencyTransport,
    private val tokens: AgencyTokenStore,
) {
    private val refreshMutex = Mutex()
    private val generationLock = Any()
    private var generation = 0L
    private var pendingRequestId = ""
    private var pendingNonce = ""
    private val mutableSession = MutableStateFlow<AgencyCreatorSession?>(null)
    val session: StateFlow<AgencyCreatorSession?> = mutableSession.asStateFlow()

    suspend fun start(email: String): AgencyLoginChallenge {
        val startGeneration = synchronized(generationLock) { generation }
        val request = StartAgencySessionRequest.newBuilder().setEmail(email.trim()).setAppId(configuration.appId).build()
        val result = grpcCall { transport.stub.startAgencySession(request) }
        val nonce = confirmationNonce()
        synchronized(generationLock) {
            if (generation != startGeneration) throw kotlinx.coroutines.CancellationException("Agency sign-in was cancelled")
            pendingRequestId = result.requestId
            pendingNonce = nonce
        }
        return AgencyLoginChallenge(result.requestId, result.expiresInSeconds)
    }

    suspend fun confirm(requestId: String, code: String): AgencyCreatorSession {
        val (startGeneration, nonce) = synchronized(generationLock) {
            require(requestId == pendingRequestId && pendingNonce.isNotEmpty()) { "Start a new Shopar sign-in request" }
            generation to pendingNonce
        }
        val confirmRequest = ConfirmAgencySessionRequest.newBuilder()
                .setRequestId(requestId)
                .setCode(code.trim())
                .setConfirmationNonce(nonce)
                .build()
        val response = grpcCall { transport.stub.confirmAgencySession(confirmRequest) }
        val issued = response.tokens ?: error("Shopar session response is incomplete")
        val profile = response.profile ?: error("Shopar profile response is incomplete")
        val disclosure = response.referralDisclosure ?: error("Shopar referral disclosure is unavailable")
        val pair = SessionTokenPair(issued.accessToken, issued.refreshToken, issued.accessTokenExpiresAt, issued.refreshTokenExpiresAt)
        val safe = profile.toPublicSession(disclosure, response.isNewAccount)
        synchronized(generationLock) {
            if (generation != startGeneration || requestId != pendingRequestId) {
                throw kotlinx.coroutines.CancellationException("Agency sign-in was cancelled")
            }
            tokens.write(pair)
            generation++
            mutableSession.value = safe
            pendingRequestId = ""
            pendingNonce = ""
        }
        return safe
    }

    suspend fun refreshIfNeeded(): AgencyCreatorSession? = refreshForGeneration(synchronized(generationLock) { generation })

    private suspend fun refreshForGeneration(expectedGeneration: Long): AgencyCreatorSession? = refreshMutex.withLock {
        synchronized(generationLock) {
            if (generation != expectedGeneration) throw kotlinx.coroutines.CancellationException("Agency SDK session changed")
        }
        val current = tokens.read() ?: return@withLock null
        if (current.accessTokenExpiresAt > System.currentTimeMillis() / 1000 + 45) {
            return@withLock synchronized(generationLock) {
                if (generation != expectedGeneration) throw kotlinx.coroutines.CancellationException("Agency SDK session changed")
                mutableSession.value
            }
        }
        try {
            val request=RefreshAgencySessionRequest.newBuilder().setRefreshToken(current.refreshToken).build()
            val response = grpcCall { transport.stub.refreshAgencySession(request) }
            val newTokens = response
            synchronized(generationLock) {
                if (generation != expectedGeneration) throw kotlinx.coroutines.CancellationException("Agency SDK session changed")
                tokens.write(SessionTokenPair(newTokens.accessToken, newTokens.refreshToken, newTokens.accessTokenExpiresAt, newTokens.refreshTokenExpiresAt))
            }
            synchronized(generationLock) {
                if (generation != expectedGeneration) throw kotlinx.coroutines.CancellationException("Agency SDK session changed")
                mutableSession.value
            }
        } catch (error: Throwable) {
            if (Status.fromThrowable(error).code == Status.Code.UNAUTHENTICATED) {
                synchronized(generationLock) {
                    if (generation == expectedGeneration) clearLocalSession()
                }
            }
            throw error
        }
    }

    suspend fun <T> withCurrentSession(operation: suspend () -> T): T {
        return withCurrentSession(synchronized(generationLock) { generation }, operation)
    }

    private suspend fun <T> withCurrentSession(expectedGeneration: Long, operation: suspend () -> T): T {
        if (tokens.read() == null) throw Status.UNAUTHENTICATED.withDescription("Shopar login required").asRuntimeException()
        refreshForGeneration(expectedGeneration)
        synchronized(generationLock) {
            if (generation != expectedGeneration || tokens.read() == null) {
                throw kotlinx.coroutines.CancellationException("Agency SDK session ended while the request was starting")
            }
        }
        val value = operation()
        synchronized(generationLock) {
            if (generation != expectedGeneration || tokens.read() == null) {
                throw kotlinx.coroutines.CancellationException("Agency SDK session ended while the request was in flight")
            }
        }
        return value
    }

    suspend fun loadCurrentSession(): AgencyCreatorSession? {
        val expectedGeneration = synchronized(generationLock) { generation }
        if(tokens.read()==null)return null
        return try {
            val loaded = withCurrentSession(expectedGeneration) {
            val profile = grpcCall { transport.stub.getMyAgencyProfile(com.google.protobuf.Empty.getDefaultInstance()) }
            val disclosure = grpcCall { transport.stub.getMyAgencyReferralDisclosure(com.google.protobuf.Empty.getDefaultInstance()) }
                profile.toPublicSession(disclosure, false)
            }
            synchronized(generationLock) {
                if (generation != expectedGeneration || tokens.read() == null) {
                    throw kotlinx.coroutines.CancellationException("Agency SDK session ended while restoring")
                }
                mutableSession.value = loaded
            }
            loaded
        } catch (error: Throwable) {
            if (Status.fromThrowable(error).code == Status.Code.UNAUTHENTICATED) {
                synchronized(generationLock) {
                    if (generation == expectedGeneration) clearLocalSession()
                }
            }
            throw error
        }
    }

    suspend fun end() {
        val previous = synchronized(generationLock) {
            val captured = tokens.read()
            clearLocalSession()
            captured
        }
        if (previous != null) {
            // Clear local account state immediately. Revoke only the session captured
            // before logout, even if a new sign-in begins while the network call runs.
            val accessToken = if (previous.accessTokenExpiresAt > System.currentTimeMillis() / 1000 + 5) {
                previous.accessToken
            } else {
                runCatching {
                    val request = RefreshAgencySessionRequest.newBuilder().setRefreshToken(previous.refreshToken).build()
                    grpcCall { transport.stub.refreshAgencySession(request) }.accessToken
                }.getOrDefault(previous.accessToken)
            }
            runCatching {
                grpcCall { transport.stubWithAccessToken(accessToken).endAgencySession(Empty.getDefaultInstance()) }
            }
        }
    }

    fun restoreLocalSession() {
        if (tokens.read()?.refreshTokenExpiresAt?.let { it > System.currentTimeMillis() / 1000 } == true) {
            // Profile data is reloaded through an authenticated API before it is shown.
        } else clearLocalSession()
    }

    private fun clearLocalSession() {
        synchronized(generationLock) {
            generation++
            tokens.clear()
            mutableSession.value = null
            pendingRequestId = ""
            pendingNonce = ""
        }
    }

    private fun confirmationNonce(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }
}
