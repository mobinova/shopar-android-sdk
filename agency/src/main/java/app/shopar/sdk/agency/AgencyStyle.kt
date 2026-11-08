package app.shopar.sdk.agency

/** Host-configurable colors and corner geometry for the Agency SDK UI.
 * Colors use ARGB longs, for example 0xFFD8F36AL. The default matches Shopar's native apps.
 */
data class AgencyStyle(
    val backgroundColor: Long = 0xFFF6F7F2L,
    val surfaceColor: Long = 0xFFFFFFFFL,
    val surfaceSubtleColor: Long = 0xFFEFF2E9L,
    val textColor: Long = 0xFF18221DL,
    val secondaryTextColor: Long = 0xFF59655DL,
    val borderColor: Long = 0xFFDDE3D8L,
    val primaryColor: Long = 0xFF18221DL,
    val onPrimaryColor: Long = 0xFFFFFFFFL,
    val accentColor: Long = 0xFFD8F36AL,
    val onAccentColor: Long = 0xFF18221DL,
    val cardCornerRadiusDp: Float = 14f,
    val controlCornerRadiusDp: Float = 10f,
    val isDark: Boolean = false,
) {
    init {
        require(cardCornerRadiusDp in 0f..32f && controlCornerRadiusDp in 0f..32f) {
            "Shopar UI corner radii must be between 0 and 32 dp"
        }
    }

    companion object {
        /** High-contrast dark palette for hosts that need a dark embedded surface. */
        val Midnight = AgencyStyle(
            backgroundColor = 0xFF101714L,
            surfaceColor = 0xFF1A231EL,
            surfaceSubtleColor = 0xFF26312BL,
            textColor = 0xFFF6F7F2L,
            secondaryTextColor = 0xFFBBC7BEL,
            borderColor = 0xFF47554CL,
            primaryColor = 0xFFD8F36AL,
            onPrimaryColor = 0xFF18221DL,
            accentColor = 0xFFD8F36AL,
            onAccentColor = 0xFF18221DL,
            isDark = true,
        )
    }
}
