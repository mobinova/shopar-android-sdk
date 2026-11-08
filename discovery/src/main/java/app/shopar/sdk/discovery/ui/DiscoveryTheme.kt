package app.shopar.sdk.discovery.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.shopar.sdk.discovery.DiscoveryStyle

@Composable
internal fun DiscoveryTheme(style: DiscoveryStyle, content: @Composable () -> Unit) {
    val colors = if (style.isDark) {
        darkColorScheme(
            primary = Color(style.primaryColor), onPrimary = Color(style.onPrimaryColor),
            primaryContainer = Color(style.accentColor), onPrimaryContainer = Color(style.onAccentColor),
            secondary = Color(style.primaryColor), onSecondary = Color(style.onPrimaryColor),
            secondaryContainer = Color(style.surfaceSubtleColor), onSecondaryContainer = Color(style.textColor),
            background = Color(style.backgroundColor), onBackground = Color(style.textColor),
            surface = Color(style.surfaceColor), onSurface = Color(style.textColor),
            surfaceVariant = Color(style.surfaceSubtleColor), onSurfaceVariant = Color(style.secondaryTextColor),
            outline = Color(style.secondaryTextColor), outlineVariant = Color(style.borderColor),
            error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        )
    } else {
        lightColorScheme(
            primary = Color(style.primaryColor), onPrimary = Color(style.onPrimaryColor),
            primaryContainer = Color(style.accentColor), onPrimaryContainer = Color(style.onAccentColor),
            secondary = Color(style.primaryColor), onSecondary = Color(style.onPrimaryColor),
            secondaryContainer = Color(style.surfaceSubtleColor), onSecondaryContainer = Color(style.textColor),
            background = Color(style.backgroundColor), onBackground = Color(style.textColor),
            surface = Color(style.surfaceColor), onSurface = Color(style.textColor),
            surfaceVariant = Color(style.surfaceSubtleColor), onSurfaceVariant = Color(style.secondaryTextColor),
            outline = Color(style.secondaryTextColor), outlineVariant = Color(style.borderColor),
            error = Color(0xFFAD2F3E), onError = Color.White,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            small = RoundedCornerShape(style.controlCornerRadiusDp.dp),
            medium = RoundedCornerShape(style.controlCornerRadiusDp.dp),
            large = RoundedCornerShape(style.cardCornerRadiusDp.dp),
            extraLarge = RoundedCornerShape(style.cardCornerRadiusDp.dp),
        ),
    ) {
        Surface(color = colors.background, contentColor = colors.onBackground, content = content)
    }
}

@Composable
internal fun discoveryPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)
