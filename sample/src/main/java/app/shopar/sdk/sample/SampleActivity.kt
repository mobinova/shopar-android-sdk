package app.shopar.sdk.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.shopar.sdk.agency.AgencyConfiguration
import app.shopar.sdk.agency.ShoparAgency
import app.shopar.sdk.discovery.DiscoveryConfiguration
import app.shopar.sdk.discovery.ShoparDiscovery

class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShoparAgency.initialize(applicationContext, AgencyConfiguration(BuildConfig.AGENCY_APP_ID))
        ShoparDiscovery.initialize(applicationContext, DiscoveryConfiguration(BuildConfig.DISCOVERY_APP_ID))
        setContent { SampleHost() }
    }
}

@Composable
private fun SampleHost() {
    val screen = remember { mutableStateOf("home") }
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (screen.value) {
                "agency-login" -> ShoparAgency.SignInScreen { screen.value = "agency-links" }
                "agency-links" -> ShoparAgency.LinksScreen()
                "discovery" -> ShoparDiscovery.Placement(BuildConfig.DISCOVERY_PLACEMENT_ID)
                else -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Shopar partner SDK sample", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { screen.value = if (ShoparAgency.creatorSession.value == null) "agency-login" else "agency-links" }) { Text("Agency SDK") }
                    Button(onClick = { screen.value = "discovery" }) { Text("Discovery SDK") }
                    Button(onClick = { screen.value = "home" }) { Text("Home") }
                }
            }
        }
    }
}
