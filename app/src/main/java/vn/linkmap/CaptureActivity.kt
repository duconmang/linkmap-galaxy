package vn.linkmap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.linkmap.core.Urls
import vn.linkmap.core.Bookmark
import vn.linkmap.core.IncomingContent
import vn.linkmap.ui.BookmarkEditor
import vn.linkmap.ui.LinkMapTheme

class CaptureActivity : ComponentActivity() {
    private val app get() = application as LinkMapApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED) // Never replace the selected text in the source app.
        enableEdgeToEdge()
        val text = when (intent.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }.orEmpty()
        val urls = runCatching { IncomingContent.detect(text).urls }.getOrDefault(emptyList())
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().take(200)
        setContent { LinkMapTheme {
            val ready by app.ready.collectAsState()
            var picked by remember { mutableStateOf(urls.singleOrNull() ?: text.takeIf { urls.isEmpty() && it.isNotBlank() }) }
            val store by app.stores.collectAsState()
            val bookmarks by store.bookmarks.collectAsState()
            var saved by remember { mutableStateOf<Bookmark?>(null) }
            var duplicate by remember { mutableStateOf(false) }
            var editing by remember { mutableStateOf(false) }
            var manual by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(ready,picked) {
                if(ready && picked!=null && !manual && saved==null) {
                    try {
                        val existing=bookmarks.find { !it.isNote && it.url==Urls.fromInput(picked.orEmpty()) }
                        duplicate=existing!=null
                        saved=existing ?: app.saveContent(picked.orEmpty(),subject,if(urls.isNotEmpty() && text.trim()!=picked && Urls.fromInput(text)!=picked) text else "","Cá nhân")
                    } catch(e:Exception) { error=e.message;manual=true }
                }
            }
            Surface(Modifier.fillMaxSize()) {
                if (!ready) Box(Modifier.padding(48.dp)) { CircularProgressIndicator() }
                else if(saved!=null && !editing) Column(Modifier.safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text(if(duplicate) "Bạn đã lưu link này" else "✓ Đã lưu vào LinkMap",style=MaterialTheme.typography.headlineSmall)
                    Text(saved!!.title);Text(if(saved!!.isNote) saved!!.note else saved!!.url,maxLines=6)
                    Button(onClick={finish()}) { Text("Xong · Quay lại") }
                    TextButton(onClick={editing=true}) { Text("Đổi tên / bộ sưu tập / ghi chú") }
                    TextButton(onClick={startActivity(Intent(this@CaptureActivity,MainActivity::class.java).putExtra("bookmarkId",saved!!.id));finish()}) { Text("Xem trên bản đồ") }
                    if(!duplicate) TextButton(onClick={scope.launch { store.delete(saved!!.id);finish() }}) { Text("Hoàn tác lưu") }
                }
                else if (picked != null && (manual || editing)) BookmarkEditor(
                    initialUrl=picked.orEmpty(),initialTitle=subject,categories=bookmarks.map {it.category}.distinct(),bookmark=if(editing) saved else null,onDismiss={finish()},
                    onSave={url,title,note,category ->
                        val existing=if(editing && saved?.isNote==true) null else bookmarks.find { !it.isNote && it.url==Urls.fromInput(url) && it.id!=(if(editing) saved?.id else null) }
                        duplicate=existing!=null
                        saved=existing ?: app.saveContent(url,title,note,category,if(editing) saved?.id else null)
                        manual=false;editing=false
                    }
                )
                else if(picked!=null) Box(Modifier.padding(48.dp)) { CircularProgressIndicator() }
                else Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Thêm vào LinkMap", style = MaterialTheme.typography.headlineSmall)
                    error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                    if (urls.isEmpty()) {
                        Text("Nhập hoặc dán một link hay ghi chú vào LinkMap.")
                        Button(onClick = { manual=true;picked = "" }) { Text("Nhập nội dung") }
                    } else {
                        Text("Văn bản có ${urls.size} link. Chọn link muốn thêm:")
                        androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                            items(urls.size) { index -> TextButton(onClick = { picked = urls[index] }) { Text(urls[index]) } }
                        }
                    }
                    TextButton(onClick = { finish() }) { Text("Đóng") }
                }
            }
        } }
    }
    override fun onStart() { super.onStart(); app.entered() }
    override fun onStop() { app.left(); super.onStop() }
}
