package vn.linkmap.ui

import androidx.compose.ui.graphics.Color

/** Platform identity is separate from the star's availability colour. */
fun platformNetworkColor(key: String): Color = when(key) {
    "platform:GITHUB" -> Color(0xFF51D9FF)
    "platform:FACEBOOK" -> Color(0xFF648BFF)
    "platform:TIKTOK" -> Color(0xFFFF78B7)
    "platform:DRIVE" -> Color(0xFFFFCC66)
    "platform:ONEDRIVE" -> Color(0xFFBB92FF)
    "platform:GOOGLE" -> Color(0xFF4DE0B3)
    else -> {
        // Stable host colour, independent of ordering, health, selection or collection.
        val hash=key.hashCode().toLong() and 0xffffffffL
        Color.hsv((hash%36000)/100f,.55f+((hash ushr 16)%25)/100f,.95f)
    }
}
