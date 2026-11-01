package vn.linkmap.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import vn.linkmap.core.Health
import vn.linkmap.core.Bookmark

val Space = Color(0xFF050D1C)
val Panel = Color(0xFF0E1B30)
val Blue = Color(0xFF76AEFF)
val Muted = Color(0xFF9DAFCB)
val NoteColor = Color(0xFFF0BA6A)
fun Bookmark.entryColor() = if(isNote) NoteColor else health.color()
fun Health.color() = when (this) {
    Health.REACHABLE -> Color(0xFF1AA76D)
    Health.UNAVAILABLE -> Color(0xFFD54E65)
    Health.UNCERTAIN -> Color(0xFFC69226)
    Health.UNCHECKED -> Color(0xFF52617A)
}
@Composable fun LinkMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Blue, onPrimary = Color(0xFF052351), background = Space,
        surface = Panel, onSurface = Color(0xFFF1F5FF), onSurfaceVariant = Muted,
        secondaryContainer = Color(0xFF193A65), surfaceContainer = Panel
    ), content = content)
}
