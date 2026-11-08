package app.shopar.sdk.discovery

/** Public, non-secret configuration for one Discovery app property. */
data class DiscoveryConfiguration(
    val appId: String,
    val grpcHost: String = "grpc.shopar.app",
    val grpcPort: Int = 443,
    val useTls: Boolean = true,
    /** Android manufacturer/model/version are public device attributes; disabled by default. */
    val collectDeviceMetadata: Boolean = false,
) {
    init {
        require(appId.matches(Regex("^app_[0-9A-HJKMNP-TV-Z]{26}$"))) {
            "A valid Shopar Discovery app_id is required"
        }
        require(grpcHost.isNotBlank() && grpcHost.none { it.isWhitespace() || it == '/' })
        require(grpcPort in 1..65535)
    }
}
