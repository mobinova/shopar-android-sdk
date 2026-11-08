package app.shopar.sdk.agency

import android.content.Context
import app.shopar.sdk.proto.creator.v1.AgencyCreatorLink
import app.shopar.sdk.proto.creator.v1.CreateAgencyCreatorLinkRequest
import app.shopar.sdk.proto.creator.v1.AgencyCreatorProfile
import app.shopar.sdk.proto.creator.v1.AgencyProductOrDeal
import app.shopar.sdk.proto.creator.v1.SearchAgencyCatalogRequest
import app.shopar.sdk.proto.common.v1.PageRequest
import app.shopar.sdk.proto.creator.v1.AgencyCreatorLinkListRequest
import app.shopar.sdk.proto.creator.v1.AgencyCreatorLinkPerformanceRequest
import app.shopar.sdk.proto.creator.v1.RevokeMyAgencySDKSessionRequest
import app.shopar.sdk.proto.creator.v1.AgencySDKSessionList
import app.shopar.sdk.proto.creator.v1.AgencyReferralDisclosure
import app.shopar.sdk.proto.creator.v1.ListMyAgencySDKSessionsRequest
import app.shopar.sdk.proto.creator.v1.UpdateAgencyCreatorProfileRequest
import app.shopar.sdk.proto.creator.v1.SaveAgencyCreatorSocialAccountRequest
import app.shopar.sdk.proto.creator.v1.DeleteAgencyCreatorSocialAccountRequest
import app.shopar.sdk.proto.creator.v1.SetAgencyCreatorProfilePhotoRequest
import app.shopar.sdk.proto.creator.v1.AgencyCreatorSocialAccountList
import app.shopar.sdk.proto.creator.v1.AgencyCreatorSocialAccountResponse
import app.shopar.sdk.proto.creator.v1.AgencyCreatorProfilePhotoResponse
import app.shopar.sdk.proto.creator.v1.AgencyCreatorLinkPerformance
import app.shopar.sdk.proto.creator.v1.OpenAgencyCreatorLinkRequest
import app.shopar.sdk.proto.creator.v1.OpenAgencyCreatorLinkResponse
import app.shopar.sdk.agency.internal.AgencySessionManager
import app.shopar.sdk.agency.internal.AgencyTokenStore
import app.shopar.sdk.agency.internal.AgencyTransport
import app.shopar.sdk.agency.internal.ShoparAgent
import app.shopar.sdk.agency.internal.grpcCall
import app.shopar.sdk.agency.ui.AgencySignInScreen
import app.shopar.sdk.agency.ui.AgencyLinksScreen
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.StateFlow

/** Entry point for the Shopar Agency SDK. */
object ShoparAgency {
    @Volatile private var configuration: AgencyConfiguration? = null
    @Volatile private var applicationContext: Context? = null
    @Volatile private var runtime: Runtime? = null

    fun initialize(context: Context, config: AgencyConfiguration) {
        val appContext = context.applicationContext
        synchronized(this) {
            val existing = configuration
            require(existing == null || existing == config) {
                "ShoparAgency is already initialized with a different app_id"
            }
            applicationContext = appContext
            configuration = config
            if (runtime == null) {
                val tokens = AgencyTokenStore(appContext, config)
                val channel = AgencyTransport(config.grpcHost, config.grpcPort, config.useTls, ShoparAgent(appContext), tokens)
                runtime = Runtime(config, channel, AgencySessionManager(config, channel, tokens))
            }
        }
    }

    internal fun requireConfiguration(): AgencyConfiguration =
        checkNotNull(configuration) { "Call ShoparAgency.initialize before using the SDK" }

    internal fun requireApplicationContext(): Context =
        checkNotNull(applicationContext) { "Call ShoparAgency.initialize before using the SDK" }

    /** Begins the Shopar email-code sign-in flow for this Agency app_id. */
    suspend fun startSignIn(email: String): AgencySignInChallenge {
        val challenge = requireRuntime().sessions.start(email)
        return AgencySignInChallenge(challenge.requestId, challenge.expiresInSeconds)
    }

    /** Confirms the code and returns only an allowlisted creator profile and disclosure. */
    suspend fun confirmSignIn(requestId: String, code: String): AgencyCreatorSession = requireRuntime().sessions.confirm(requestId, code)

    /** Ends the current Agency SDK session and clears its account-scoped local state. */
    suspend fun signOut() = requireRuntime().sessions.end()

    val creatorSession: StateFlow<AgencyCreatorSession?> get() = requireRuntime().sessions.session

    suspend fun refreshSessionIfNeeded(): AgencyCreatorSession? = requireRuntime().sessions.refreshIfNeeded()

    /** Restores the safe profile projection after process restart when a stored session is still active. */
    suspend fun restoreSession(): AgencyCreatorSession? = requireRuntime().sessions.loadCurrentSession()

    suspend fun getMyProfile(): AgencyCreatorProfile = requireRuntime().sessions.withCurrentSession {
        grpcCall { requireRuntime().transport.stub.getMyAgencyProfile(Empty.getDefaultInstance()) }
    }

    suspend fun referralDisclosure(): AgencyReferralDisclosure = requireRuntime().sessions.withCurrentSession {
        grpcCall { requireRuntime().transport.stub.getMyAgencyReferralDisclosure(Empty.getDefaultInstance()) }
    }

    suspend fun myLinks(): List<AgencyCreatorLink> = requireRuntime().sessions.withCurrentSession {
        grpcCall { requireRuntime().transport.stub.listMyLinks(AgencyCreatorLinkListRequest.getDefaultInstance()).linksList }
    }

    suspend fun updateMyProfile(fullName:String?=null,publicUsername:String?=null):AgencyCreatorProfile= requireRuntime().sessions.withCurrentSession{
        val builder=UpdateAgencyCreatorProfileRequest.newBuilder();if(fullName!=null)builder.fullName=fullName;if(publicUsername!=null)builder.publicUsername=publicUsername
        grpcCall { requireRuntime().transport.stub.updateMyAgencyProfile(builder.build()) }
    }

    suspend fun listMySocialAccounts():AgencyCreatorSocialAccountList= requireRuntime().sessions.withCurrentSession{
        grpcCall { requireRuntime().transport.stub.listMySocialAccounts(com.google.protobuf.Empty.getDefaultInstance()) }
    }

    suspend fun saveMySocialAccount(platform:String,username:String):AgencyCreatorSocialAccountResponse= requireRuntime().sessions.withCurrentSession{
        grpcCall { requireRuntime().transport.stub.saveMySocialAccount(SaveAgencyCreatorSocialAccountRequest.newBuilder().setPlatform(platform).setUsername(username).build()) }
    }

    suspend fun deleteMySocialAccount(platform:String){requireRuntime().sessions.withCurrentSession{grpcCall{requireRuntime().transport.stub.deleteMySocialAccount(DeleteAgencyCreatorSocialAccountRequest.newBuilder().setPlatform(platform).build())}}}

    suspend fun setMyProfilePhoto(image:ByteArray,contentType:String):AgencyCreatorProfilePhotoResponse= requireRuntime().sessions.withCurrentSession{
        grpcCall { requireRuntime().transport.stub.setMyAgencyProfilePhoto(SetAgencyCreatorProfilePhotoRequest.newBuilder().setImage(com.google.protobuf.ByteString.copyFrom(image)).setContentType(contentType).build()) }
    }

    suspend fun linkPerformance(sharedLinkId:String,period:String="last_30_days"):AgencyCreatorLinkPerformance= requireRuntime().sessions.withCurrentSession{
        grpcCall { requireRuntime().transport.stub.getMyLinkPerformance(AgencyCreatorLinkPerformanceRequest.newBuilder().setSharedLinkId(sharedLinkId).setPeriod(period).build()) }
    }

    suspend fun openLink(sharedLinkId:String,idempotencyKey:String):OpenAgencyCreatorLinkResponse= requireRuntime().sessions.withCurrentSession{
        grpcCall { requireRuntime().transport.stub.openLink(OpenAgencyCreatorLinkRequest.newBuilder().setSharedLinkId(sharedLinkId).setIdempotencyKey(idempotencyKey).build()) }
    }

    suspend fun searchCatalog(query: String, page: Int = 1, pageSize: Int = 30): List<AgencyProductOrDeal> = requireRuntime().sessions.withCurrentSession {
        val request = SearchAgencyCatalogRequest.newBuilder().setQuery(query.trim())
            .setPage(PageRequest.newBuilder().setPage(page).setPageSize(pageSize)).build()
        grpcCall { requireRuntime().transport.stub.searchShareableCatalog(request).itemsList }
    }

    suspend fun createProductLink(productId: String, idempotencyKey: String): AgencyCreatorLink = requireRuntime().sessions.withCurrentSession {
        val request = CreateAgencyCreatorLinkRequest.newBuilder().setProductId(productId).setIdempotencyKey(idempotencyKey).build()
        grpcCall { requireRuntime().transport.stub.createLink(request) }
    }

    suspend fun createDealLink(dealId: String, idempotencyKey: String): AgencyCreatorLink = requireRuntime().sessions.withCurrentSession {
        val request = CreateAgencyCreatorLinkRequest.newBuilder().setDealId(dealId).setIdempotencyKey(idempotencyKey).build()
        grpcCall { requireRuntime().transport.stub.createLink(request) }
    }

    suspend fun agencySdkSessions(): AgencySDKSessionList = requireRuntime().sessions.withCurrentSession {
        grpcCall { requireRuntime().transport.stub.listMySessions(ListMyAgencySDKSessionsRequest.getDefaultInstance()) }
    }

    suspend fun revokeAgencySdkSession(sessionId: String) = requireRuntime().sessions.withCurrentSession {
        grpcCall { requireRuntime().transport.stub.revokeMySession(RevokeMyAgencySDKSessionRequest.newBuilder().setSessionId(sessionId).build()) }
    }

    /** Call when the host application signs out or switches its own account. */
    suspend fun onHostAccountChanged() = signOut()

    @androidx.compose.runtime.Composable
    fun SignInScreen(onSignedIn: (AgencyCreatorSession) -> Unit) = AgencySignInScreen(onSignedIn)

    @androidx.compose.runtime.Composable
    fun LinksScreen(
        onShare: ((String) -> Unit)? = null,
        onOpenAccountSecurity: (() -> Unit)? = null,
    ) = AgencyLinksScreen(onShare, onOpenAccountSecurity = onOpenAccountSecurity)

    private fun requireRuntime() = checkNotNull(runtime) { "Call ShoparAgency.initialize before using the SDK" }

    private data class Runtime(val config: AgencyConfiguration, val transport: AgencyTransport, val sessions: AgencySessionManager)
}

data class AgencySignInChallenge(val requestId: String, val expiresInSeconds: Int)
