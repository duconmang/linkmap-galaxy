package vn.linkmap.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.linkmap.R
import vn.linkmap.core.Health
import vn.linkmap.core.Provider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes

@Composable fun PlatformBadge(provider: Provider, health: Health, isNote:Boolean=false) {
    val icon = when (provider) {
        Provider.FACEBOOK -> R.drawable.brand_facebook
        Provider.TIKTOK -> R.drawable.brand_tiktok
        Provider.GITHUB -> R.drawable.brand_github
        Provider.DRIVE -> R.drawable.brand_googledrive
        else -> null
    }
    Surface(color = if(isNote) NoteColor.copy(alpha=.25f) else health.color(), shape = MaterialTheme.shapes.small) {
        if(isNote) Icon(Icons.Default.Notes,"Ghi chú",tint=NoteColor,modifier=Modifier.padding(12.dp).size(28.dp))
        else if (icon != null) Icon(painterResource(icon), provider.label, tint = Color.White, modifier = Modifier.padding(12.dp).size(28.dp))
        else Text(platformLetter(provider), Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
    }
}
