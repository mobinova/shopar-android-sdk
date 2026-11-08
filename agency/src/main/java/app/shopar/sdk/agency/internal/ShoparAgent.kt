package app.shopar.sdk.agency.internal

import android.content.Context
import android.os.Build
import app.shopar.sdk.agency.BuildConfig
import ulid.ULID
import java.io.File
import java.util.Locale
import java.util.TimeZone

internal class ShoparAgent(context: Context) {
    private val appContext = context.applicationContext
    val installationId: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { loadOrCreateInstallationId() }

    fun header(): String = "Shopar/${BuildConfig.SHOPAR_SDK_BUILD}/${clean(BuildConfig.SHOPAR_SDK_VERSION)} " +
        "(${clean(Build.MANUFACTURER)}; ${clean(Build.MODEL)}; Android ${clean(Build.VERSION.RELEASE)}; " +
        "${clean(Build.DEVICE)}; $installationId; ${clean(Locale.getDefault().toLanguageTag())}; " +
        "${clean(TimeZone.getDefault().id)}) native"

    private fun loadOrCreateInstallationId(): String {
        val file = File(appContext.noBackupFilesDir, "shopar-agency-installation-id")
        val existing = runCatching { file.readText().trim() }.getOrNull()
        if (existing != null && DEVICE_ID.matches(existing)) return existing
        val value = "dev_${ULID.randomULID()}"
        val temporary = File(appContext.noBackupFilesDir, "shopar-agency-installation-id.tmp")
        try {
            java.io.FileOutputStream(temporary).use { stream ->
                stream.write(value.toByteArray(Charsets.US_ASCII))
                stream.fd.sync()
            }
            check(temporary.renameTo(file)) { "Could not persist the Shopar installation ID" }
        } catch (error: Throwable) {
            temporary.delete()
            throw IllegalStateException("Could not persist the Shopar installation ID", error)
        }
        return value
    }

    private fun clean(value: String?): String = value?.trim()?.takeIf(String::isNotEmpty)
        ?.replace(';', '-')?.replace(')', '-')?.replace('(', '-')?.replace(' ', '_') ?: "unknown"

    private companion object {
        val DEVICE_ID = Regex("^dev_[0-9A-HJKMNP-TV-Z]{26}$")
    }
}
