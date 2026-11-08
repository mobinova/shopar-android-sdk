plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }

android {
    namespace = "app.shopar.sdk.sample"
    compileSdk = 37
    defaultConfig { applicationId = "app.shopar.sdk.sample"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0" }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    defaultConfig {
        buildConfigField("String", "AGENCY_APP_ID", "\"${providers.gradleProperty("agencyAppId").orElse("app_00000000000000000000000000").get()}\"")
        buildConfigField("String", "DISCOVERY_APP_ID", "\"${providers.gradleProperty("discoveryAppId").orElse("app_00000000000000000000000000").get()}\"")
        buildConfigField("String", "DISCOVERY_PLACEMENT_ID", "\"${providers.gradleProperty("discoveryPlacementId").orElse("plc_00000000000000000000000000").get()}\"")
    }
}

dependencies {
    implementation(project(":agency"))
    implementation(project(":discovery"))
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
