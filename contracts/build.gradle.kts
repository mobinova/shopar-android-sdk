import com.google.protobuf.gradle.id

plugins {
    id("com.android.library")
    id("com.google.protobuf")
    id("maven-publish")
}

group = "com.mobinova.shopar"
version = providers.gradleProperty("shoparSdkVersion").orElse("0.1.0-SNAPSHOT").get()

android {
    namespace = "app.shopar.sdk.contracts"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    publishing { singleVariant("release") }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.3" }
    plugins { id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.84.0" } }
    generateProtoTasks { all().configureEach { builtins { id("java") }; plugins { id("grpc") } } }
}

dependencies {
    api(platform("io.grpc:grpc-bom:1.84.0"))
    api("com.google.protobuf:protobuf-java:3.25.3")
    api("io.grpc:grpc-stub")
    api("io.grpc:grpc-okhttp")
    api("io.grpc:grpc-protobuf")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mobinova.shopar"
            artifactId = "shopar-contracts-android"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
        }
    }
}
