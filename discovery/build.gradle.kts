plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

group = "com.mobinova.shopar"
version = providers.gradleProperty("shoparSdkVersion").orElse("0.1.0-SNAPSHOT").get()

android {
    namespace = "app.shopar.sdk.discovery"
    compileSdk = 37
    defaultConfig { minSdk = 26; buildConfigField("String", "SHOPAR_SDK_VERSION", "\"${project.version}\""); buildConfigField("int", "SHOPAR_SDK_BUILD", "1") }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    api(project(":contracts"))
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.aallam.ulid:ulid-kotlin:1.3.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mobinova.shopar"
            artifactId = "discovery-android"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
        }
    }
}
