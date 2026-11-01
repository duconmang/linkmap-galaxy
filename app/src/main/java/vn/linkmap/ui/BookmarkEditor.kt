package vn.linkmap.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.linkmap.core.*

@Composable fun BookmarkEditor(
    initialUrl: String = "", initialTitle: String = "", bookmark: Bookmark? = null,
    onDismiss: () -> Unit, categories:List<String> = emptyList(), onSave: suspend (String, String, String, String) -> Unit
) {
    var content by rememberSaveable { mutableStateOf(if(bookmark?.isNote==true) bookmark.note else bookmark?.url ?: initialUrl) }
    var title by rememberSaveable { mutableStateOf(bookmark?.title ?: initialTitle) }
    var annotation by rememberSaveable { mutableStateOf(if(bookmark?.isNote==true) "" else bookmark?.note.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(bookmark?.category ?: "Thiên hà chung") }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var customCategory by rememberSaveable { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    val incoming=remember(content) { runCatching { IncomingContent.detect(content) }.getOrNull() }
    val isNote=bookmark?.isNote==true || incoming?.kind==EntryKind.NOTE
    val urls=if(bookmark?.isNote==true) emptyList() else incoming?.urls.orEmpty()
    val selected=urls.singleOrNull() ?: chosen?.takeIf { it in urls }
    fun submit(input:String=content) {
        if(saving) return
        val detected=try { IncomingContent.detect(input) } catch(e:IllegalArgumentException) {error=e.message;return}
        val keepNote=bookmark?.isNote==true || detected.kind==EntryKind.NOTE
        if(bookmark!=null && !bookmark.isNote && keepNote) {error="Nhập URL hợp lệ cho liên kết này. Dùng nút + để tạo note mới.";return}
        val link=if(keepNote) null else detected.urls.singleOrNull() ?: chosen?.takeIf { it in detected.urls }
        if(!keepNote && link==null) {error="Chọn một link trong nội dung trước khi lưu";return}
        val body=if(keepNote) detected.text else if(Urls.fromInput(detected.text)==link) annotation
            else listOf(if(bookmark!=null) annotation else "",detected.text).filter { it.isNotBlank() }.distinct().joinToString("\n\n")
        scope.launch {
            saving=true
            try { onSave(if(keepNote) detected.text else link!!,title,if(keepNote) "" else body,category) }
            catch(e:Exception) {error=e.message ?: "Chưa lưu được nội dung"}
            finally {saving=false}
        }
    }
    fun paste() {
        val text=(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if(text.isBlank()) error="Clipboard chưa có nội dung"
        else {content=text;chosen=null;error=null}
    }
    val primaryLabel=if(saving) "Đang lưu…" else if(bookmark!=null) "Lưu thay đổi" else if(isNote) "Lưu ghi chú" else "Thêm vào bản đồ"
    val enabled=!saving && content.isNotBlank()
    val options=(listOfNotNull(bookmark?.category)+listOf("Thiên hà chung","Design","Dev / GitHub","Cá nhân")+categories)
        .filter {it.isNotBlank()}.distinctBy {it.replace(" / "," & ")}.take(12)
    CosmicModal(title=if(bookmark==null) "Thêm vào LinkMap" else if(bookmark.isNote) "Chỉnh sửa ghi chú" else "Chỉnh sửa liên kết",
        subtitle="Chỉ cần một link hoặc điều cần nhớ. Icon và loại nội dung được chọn tự động.",enabled=!saving,onDismiss=onDismiss,
        footer={
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                OutlinedButton(onClick=onDismiss,enabled=!saving,shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.1f)),colors=ButtonDefaults.outlinedButtonColors(contentColor=Muted),modifier=Modifier.weight(.37f).heightIn(min=50.dp)) {Text("Hủy")}
                Row(Modifier.weight(.63f).clip(RoundedCornerShape(14.dp)).alpha(if(enabled) 1f else .38f)
                    .background(Brush.linearGradient(listOf(Color(0xFF24B9F3),Color(0xFF62E5EC))))
                    .clickable(enabled=enabled,role=Role.Button,onClick={submit()}).padding(horizontal=12.dp,vertical=12.dp).heightIn(min=26.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    Icon(if(bookmark==null) Icons.Default.Add else Icons.Default.Check,null,tint=Color(0xFF052B42),modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(primaryLabel,color=Color(0xFF052B42),fontWeight=FontWeight.SemiBold,fontSize=13.sp,textAlign=TextAlign.Center,maxLines=2)
                }
            }
        }) {
        Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFF040B19),border=BorderStroke(1.dp,StitchCyan.copy(alpha=.25f)),modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start=12.dp,end=5.dp,top=5.dp,bottom=5.dp),verticalAlignment=Alignment.Top) {
                Icon(if(isNote) Icons.Default.Notes else Icons.Default.Link,null,tint=StitchCyan,modifier=Modifier.padding(top=12.dp).size(18.dp))
                Spacer(Modifier.width(9.dp))
                BasicTextField(content,{content=it;chosen=null;error=null},enabled=!saving,textStyle=TextStyle(color=Color(0xFFE7F2FF),fontSize=14.sp,lineHeight=21.sp),
                    cursorBrush=Brush.linearGradient(listOf(StitchCyan,StitchCyan)),minLines=if(isNote && content.isNotBlank()) 3 else 1,maxLines=6,
                    modifier=Modifier.weight(1f).padding(vertical=11.dp).semantics {contentDescription="Nhập hoặc dán link / ghi chú"},decorationBox={field ->
                        Box {if(content.isEmpty()) Text("Nhập hoặc dán link / ghi chú…",fontSize=12.sp,color=Muted.copy(alpha=.75f),maxLines=2,overflow=TextOverflow.Ellipsis);field()}
                    })
                TextButton(onClick={paste()},enabled=!saving,shape=RoundedCornerShape(9.dp),colors=ButtonDefaults.textButtonColors(containerColor=StitchCyan.copy(alpha=.13f),contentColor=Color(0xFF7DD3FC)),contentPadding=PaddingValues(horizontal=9.dp,vertical=5.dp),modifier=Modifier.heightIn(min=40.dp)) {Text("DÁN",fontSize=10.sp,fontWeight=FontWeight.Bold)}
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={paste()},enabled=!saving,contentPadding=PaddingValues(0.dp),modifier=Modifier.weight(1f)) {
                Icon(Icons.Default.ContentPaste,null,tint=Color(0xFF7BBEDB),modifier=Modifier.size(14.dp));Spacer(Modifier.width(5.dp));Text("Dán từ bộ nhớ tạm",fontSize=10.sp,color=Color(0xFF7BBEDB))
            }
            Text(if(isNote) "Văn bản nhiều dòng" else "Tự nhận diện metadata",color=Muted.copy(alpha=.7f),fontSize=9.sp,textAlign=TextAlign.End,modifier=Modifier.weight(.9f))
        }
        if(incoming!=null) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                if(isNote) Icon(Icons.Default.Notes,"Ghi chú",tint=NoteColor,modifier=Modifier.size(16.dp))
                else Text(selected?.let {platformLetter(Provider.forUrl(it))} ?: "↗",color=StitchCyan,fontWeight=FontWeight.Bold)
                Text(if(isNote) "Ghi chú văn bản" else selected?.let {"Liên kết · ${Provider.forUrl(it).label}"} ?: "${urls.size} liên kết",color=if(isNote) NoteColor else StitchCyan,fontSize=11.sp)
                Spacer(Modifier.weight(1f))
                Text("${content.length}/64.000",color=Muted,fontSize=9.sp)
            }
            if(!isNote && urls.size>1) {
                Text("Chọn một link; toàn bộ văn bản sẽ được giữ lại.",color=Muted,fontSize=11.sp)
                LazyColumn(Modifier.heightIn(max=150.dp)) {items(urls,key={it}) {url -> TextButton(onClick={chosen=url;error=null}) {Text((if(selected==url) "✓ " else "")+url,fontSize=11.sp)}}}
            }
        }
        Text("Phân nhóm vì sao (Danh mục):",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Medium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(1.dp)) {
            options.forEach {name ->
                FilterChip(selected=category==name,onClick={category=name},enabled=!saving,shape=RoundedCornerShape(20.dp),
                    colors=FilterChipDefaults.filterChipColors(containerColor=Color.White.copy(alpha=.04f),selectedContainerColor=StitchCyan.copy(alpha=.15f),labelColor=Muted,selectedLabelColor=Color(0xFF9ADFFF)),
                    border=BorderStroke(1.dp,if(category==name) StitchCyan.copy(alpha=.4f) else Color.White.copy(alpha=.08f)),
                    label={Text(name.replace(" / "," & "),fontSize=10.sp,maxLines=1)})
            }
        }
        TextButton(onClick={customCategory=!customCategory},enabled=!saving,contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=32.dp)) {Text(if(customCategory) "Ẩn nhóm khác" else "+ Nhóm khác",fontSize=10.sp,color=Muted)}
        if(customCategory || bookmark!=null) OutlinedTextField(category,{category=it},label={Text("Bộ sưu tập")},singleLine=true,modifier=Modifier.fillMaxWidth())
        if(bookmark!=null) {
            OutlinedTextField(title,{title=it},label={Text("Tiêu đề")},singleLine=true,modifier=Modifier.fillMaxWidth())
            if(!bookmark.isNote) OutlinedTextField(annotation,{annotation=it},label={Text("Ghi chú kèm liên kết")},minLines=2,maxLines=6,modifier=Modifier.fillMaxWidth())
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp)}
    }
}
