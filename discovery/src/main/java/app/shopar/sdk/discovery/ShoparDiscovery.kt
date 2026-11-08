package app.shopar.sdk.discovery

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.shopar.sdk.discovery.internal.DiscoveryTransport
import app.shopar.sdk.discovery.internal.ShoparAgent
import app.shopar.sdk.discovery.ui.DiscoveryPlacementView
import app.shopar.sdk.discovery.ui.DiscoveryTheme

/** Entry point for unauthenticated Shopar product placements. Discovery never reads an Agency PWT. */
object ShoparDiscovery {
    @Volatile private var configuration: DiscoveryConfiguration? = null
    @Volatile private var applicationContext: Context? = null
    @Volatile private var transport: DiscoveryTransport? = null

    fun initialize(context: Context, config: DiscoveryConfiguration) {
        val appContext=context.applicationContext
        synchronized(this) {
            val existing=configuration
            require(existing==null||existing==config){"ShoparDiscovery is already initialized with a different app_id"}
            applicationContext=appContext
            configuration=config
            if(transport==null)transport=DiscoveryTransport(config.grpcHost,config.grpcPort,config.useTls,ShoparAgent(appContext,config))
        }
    }

    /** Update the host's current privacy preference for optional device metadata. Installation IDs stay stable. */
    fun setOptionalDeviceMetadataCollectionEnabled(enabled:Boolean) {
        checkNotNull(transport){"Call ShoparDiscovery.initialize before changing privacy preferences"}
            .setOptionalDeviceMetadataCollectionEnabled(enabled)
    }

    /** Call only after the user explicitly requests a new Discovery installation identity. */
    fun resetInstallationIdentity() {
        checkNotNull(transport){"Call ShoparDiscovery.initialize before resetting installation identity"}
            .resetInstallationIdentity()
    }

    @Composable
    fun Placement(placementId:String,modifier:Modifier=Modifier,onProductSelected:(String)->Unit={}) {
        val config=checkNotNull(configuration){"Call ShoparDiscovery.initialize before rendering a placement"}
        val service=checkNotNull(transport){"Call ShoparDiscovery.initialize before rendering a placement"}.stub
        DiscoveryTheme(config.style) {
            DiscoveryPlacementView(config.appId,placementId,service,modifier,onProductSelected)
        }
    }
}
