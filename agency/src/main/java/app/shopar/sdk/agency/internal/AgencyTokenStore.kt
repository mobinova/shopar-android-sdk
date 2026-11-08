package app.shopar.sdk.agency.internal

import android.content.Context
import android.util.AtomicFile
import app.shopar.sdk.agency.AgencyConfiguration
import java.io.File
import java.security.MessageDigest

internal data class SessionTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Long,
    val refreshTokenExpiresAt: Long,
)

/** Platform-protected, no-backup storage. Tokens never enter SDK public APIs. */
internal class AgencyTokenStore(context: Context, configuration: AgencyConfiguration) {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, fileName(configuration)))

    @Synchronized fun read(): SessionTokenPair? = runCatching {
        val fields = file.openRead().bufferedReader().use { it.readLines() }
        if (fields.size != 4 || fields.any(String::isBlank)) null else SessionTokenPair(
            accessToken = fields[0], refreshToken = fields[1],
            accessTokenExpiresAt = fields[2].toLong(), refreshTokenExpiresAt = fields[3].toLong(),
        )
    }.getOrNull()

    @Synchronized fun write(tokens: SessionTokenPair) {
        val stream = file.startWrite()
        try {
            val writer = stream.bufferedWriter()
            writer.append(tokens.accessToken).append('\n')
                .append(tokens.refreshToken).append('\n')
                .append(tokens.accessTokenExpiresAt.toString()).append('\n')
                .append(tokens.refreshTokenExpiresAt.toString()).append('\n')
            writer.flush()
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
    }

    @Synchronized fun clear() { file.delete() }

    private fun fileName(config: AgencyConfiguration): String {
        val scope = "agency|${config.grpcHost}:${config.grpcPort}|${config.useTls}|${config.appId}"
        val hash = MessageDigest.getInstance("SHA-256").digest(scope.toByteArray()).take(12).joinToString("") { "%02x".format(it) }
        return "shopar-agency-session-$hash"
    }
}
