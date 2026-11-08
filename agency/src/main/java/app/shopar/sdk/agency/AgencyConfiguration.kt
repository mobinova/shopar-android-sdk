package app.shopar.sdk.agency

/** Public, non-secret configuration for one Agency SDK app property. */
data class AgencyConfiguration(
    val appId: String,
    val grpcHost: String = "grpc.shopar.app",
    val grpcPort: Int = 443,
    val useTls: Boolean = true,
    val style: AgencyStyle = AgencyStyle(),
) {
    init {
        require(appId.matches(Regex("^app_[0-9A-HJKMNP-TV-Z]{26}$"))) {
            "A valid Shopar Agency app_id is required"
        }
        require(grpcHost.isNotBlank() && grpcHost.none { it.isWhitespace() || it == '/' })
        require(grpcPort in 1..65535)
    }
}
