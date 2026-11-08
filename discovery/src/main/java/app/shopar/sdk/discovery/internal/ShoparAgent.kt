package app.shopar.sdk.discovery.internal

import android.content.Context
import android.os.Build
import app.shopar.sdk.discovery.BuildConfig
import app.shopar.sdk.discovery.DiscoveryConfiguration
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import ulid.ULID

internal class ShoparAgent(context: Context, private val config: DiscoveryConfiguration) {
    private val appContext=context.applicationContext
    private val collectDeviceMetadata=AtomicBoolean(config.collectDeviceMetadata)
    private val installationIdLock=Any()
    @Volatile private var storedInstallationId:String?=null
    private val installationId:String get()=synchronized(installationIdLock){storedInstallationId?:loadOrCreateId().also{storedInstallationId=it}}

    fun setOptionalDeviceMetadataCollectionEnabled(enabled:Boolean){collectDeviceMetadata.set(enabled)}
    fun resetInstallationIdentity(){synchronized(installationIdLock){val file=File(appContext.noBackupFilesDir,"shopar-discovery-installation-id");if(file.exists())check(file.delete()){"Could not reset the Shopar installation ID"};storedInstallationId=loadOrCreateId()}}

    fun header():String {
        val collect=collectDeviceMetadata.get()
        val manufacturer=if(collect)Build.MANUFACTURER else "unknown"
        val model=if(collect)Build.MODEL else "unknown"
        val os=if(collect)"Android ${Build.VERSION.RELEASE}" else "unknown"
        val device=if(collect)Build.DEVICE else "unknown"
        return "Shopar/${BuildConfig.SHOPAR_SDK_BUILD}/${clean(BuildConfig.SHOPAR_SDK_VERSION)} " +
            "(${clean(manufacturer)}; ${clean(model)}; ${clean(os)}; ${clean(device)}; $installationId; " +
            "unknown; unknown) native"
    }

    private fun loadOrCreateId():String {
        val file=File(appContext.noBackupFilesDir,"shopar-discovery-installation-id")
        val old=runCatching{file.readText().trim()}.getOrNull();if(old!=null&&DEVICE_ID.matches(old))return old
        val value="dev_${ULID.randomULID()}";val temporary=File(appContext.noBackupFilesDir,"shopar-discovery-installation-id.tmp")
        try{java.io.FileOutputStream(temporary).use{stream->stream.write(value.toByteArray(Charsets.US_ASCII));stream.fd.sync()};check(temporary.renameTo(file))}
        catch(error:Throwable){temporary.delete();throw IllegalStateException("Could not persist the Shopar installation ID",error)}
        return value
    }
    private fun clean(value:String?)=value?.trim()?.takeIf(String::isNotEmpty)?.replace(';','-')?.replace(')','-')?.replace('(','-')?.replace(' ','_')?:"unknown"
    private companion object{val DEVICE_ID=Regex("^dev_[0-9A-HJKMNP-TV-Z]{26}$")}
}
