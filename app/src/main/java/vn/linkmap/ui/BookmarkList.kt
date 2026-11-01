package vn.linkmap.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.linkmap.core.*

@Composable fun BookmarkList(items: List<Bookmark>, reading: Map<String,ReadingState>, onSelect:(Bookmark)->Unit,onOpen:(Bookmark)->Unit,onLocate:(Bookmark)->Unit) {
    LazyColumn(contentPadding=PaddingValues(start=12.dp,end=12.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(items.isEmpty()) item { Text("Chưa có link phù hợp. Thử từ khóa khác hoặc bỏ bộ lọc.",modifier=Modifier.padding(24.dp),color=Muted) }
        items(items,key={it.id}) { b ->
            Surface(color=Space.copy(alpha=.9f),shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.clickable { onSelect(b) }.padding(14.dp)) {
                    Row {
                        PlatformBadge(b.provider,b.health,b.isNote)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text((if(reading[b.id]?.pinned==true) "★ " else "")+b.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text("${if(b.isNote) "Ghi chú" else b.provider.label} · ${b.category}",color=Muted,style=MaterialTheme.typography.bodySmall)
                            if(b.isNote) Text(b.note,maxLines=3,overflow=TextOverflow.Ellipsis,color=Muted,style=MaterialTheme.typography.bodySmall)
                            else Text("${healthSymbol(b.health)} ${b.health.label}",color=b.health.color(),style=MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                        TextButton(onClick={onLocate(b)}) { Icon(Icons.Default.Map,null);Spacer(Modifier.width(6.dp));Text("Bản đồ") }
                        TextButton(onClick={onOpen(b)}) { Icon(Icons.AutoMirrored.Filled.OpenInNew,null);Spacer(Modifier.width(6.dp));Text(if(b.isNote) "Đọc note" else "Mở link") }
                    }
                }
            }
        }
    }
}

@Composable fun BookmarkPreview(path: String) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,path) {
        value=if(path.isBlank()) null else withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    bitmap?.let { Image(it,"Ảnh xem trước nội dung",modifier=Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Crop) }
}
