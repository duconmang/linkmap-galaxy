package vn.linkmap.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.linkmap.LinkMapApplication
import vn.linkmap.core.*
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LinkMapRoot(app: LinkMapApplication, command: Pair<Long,Intent>? = null) {
    val activeStore by app.stores.collectAsStateWithLifecycle()
    val storedBookmarks by activeStore.bookmarks.collectAsStateWithLifecycle()
    val demoBookmarks=remember { vn.linkmap.data.DemoLibrary.load(app) }
    var demoMode by rememberSaveable { mutableStateOf(false) }
    val collectionScroll=remember(demoMode,activeStore) { LazyListState() }
    var demoReading by remember { mutableStateOf<Map<String,ReadingState>>(emptyMap()) }
    val bookmarks=if(demoMode) demoBookmarks else storedBookmarks
    val storedReading by activeStore.reading.collectAsStateWithLifecycle()
    val reading=if(demoMode) demoReading else storedReading
    val trash by activeStore.trash.collectAsStateWithLifecycle()
    val cameraState = rememberSaveableStateHolder()
    var listMode by rememberSaveable { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf(LibraryFilter.ALL.name) }
    var collection by rememberSaveable { mutableStateOf("") }
    var showTrash by rememberSaveable { mutableStateOf(false) }
    var background by rememberSaveable { mutableStateOf("Dịu") }
    var starTiles by rememberSaveable { mutableStateOf(true) }
    var twinkle by rememberSaveable { mutableStateOf(true) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var mapSelectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var overviewRequest by rememberSaveable { mutableIntStateOf(0) }
    val filter=LibraryFilter.valueOf(filterName)
    val progress by app.progress.collectAsStateWithLifecycle()
    val ready by app.ready.collectAsStateWithLifecycle()
    val account by app.sync.state.collectAsStateWithLifecycle()
    val profileInitials=account.profile?.name?.split(" ")?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.take(1) }?.uppercase()?.ifBlank { "LM" } ?: "LM"
    val lastDay by app.lastDay.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var focusId by remember { mutableStateOf<String?>(null) }
    var add by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Bookmark?>(null) }
    var delete by remember { mutableStateOf<Bookmark?>(null) }
    LaunchedEffect(activeStore) {
        selectedId=null;focusId=null;editing=null;delete=null;query="";collection="";showTrash=false
        listMode=activeStore.meta("listMode")=="true"
        background=activeStore.meta("background") ?: "Dịu"
        starTiles=activeStore.meta("tileShape")!="square"
        twinkle=activeStore.meta("twinkle")!="false"
        demoMode=activeStore.meta("showDemo")?.toBooleanStrictOrNull() ?: activeStore.bookmarks.value.isEmpty()
        mapSelectionId=null
        filterName=LibraryFilter.ALL.name
    }
    LaunchedEffect(command,ready) {
        if(ready) {
            when(command?.second?.action) {
                "vn.linkmap.ADD" -> { demoMode=false;activeStore.setMeta("showDemo","false");add=true }
                "vn.linkmap.SEARCH" -> { demoMode=false;activeStore.setMeta("showDemo","false");tab=1 }
            }
            command?.second?.getStringExtra("bookmarkId")?.let { demoMode=false;activeStore.setMeta("showDemo","false");selectedId=it;focusId=it;mapSelectionId=it }
        }
    }
    val selected = bookmarks.find { it.id == selectedId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun setReading(id:String, transform:(ReadingState)->ReadingState) {
        if(demoMode) demoReading=demoReading+(id to transform(demoReading[id] ?: ReadingState()))
        else scope.launch { activeStore.setReading(id,transform) }
    }
    fun switchDemo(value:Boolean) {
        demoMode=value;tab=0;listMode=false;selectedId=null;mapSelectionId=null;focusId=null;query="";collection="";filterName=LibraryFilter.ALL.name;overviewRequest++
        scope.launch { activeStore.setMeta("showDemo",value.toString()) }
    }
    fun openLink(item: Bookmark) {
        if(item.isNote) { selectedId=item.id;setReading(item.id) { it.copy(openedAt=System.currentTimeMillis()) };return }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            setReading(item.id) { it.copy(openedAt=System.currentTimeMillis()) }
        } catch (_: Exception) { message("Không có ứng dụng mở được link này") }
    }
    fun removeItem(item: Bookmark) {
        scope.launch {
            try {
                activeStore.delete(item.id); selectedId=null
                if(snackbar.showSnackbar("Đã chuyển vào Đã xóa", "Hoàn tác", duration=SnackbarDuration.Long)==SnackbarResult.ActionPerformed) activeStore.restore(item.id)
            } catch(e:Exception) { message(e.message ?: "Chưa xóa được") }
        }
    }
    val results=remember(bookmarks,reading,query,collection,filter) {
        bookmarks.filter { Library.matches(it,query) && (collection.isBlank() || it.category==collection) && Library.accepts(it,reading[it.id] ?: ReadingState(),filter) }
            .let { if(filter==LibraryFilter.RECENT) it.sortedByDescending { b -> b.createdAt } else if(filter==LibraryFilter.OPENED) it.sortedByDescending { b -> reading[b.id]?.openedAt ?: 0L } else it.sortedWith(compareByDescending<Bookmark> { b -> reading[b.id]?.pinned==true }.thenByDescending { b -> b.createdAt }) }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = activeStore.exportJson()
                withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(json) } }
                message("Đã xuất bản sao lưu")
            } catch (_: Exception) { message("Không ghi được bản sao lưu") }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= vn.linkmap.data.BookmarkStore.MAX_BACKUP_BYTES) {
                            val count = stream.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                        }
                        val bytes = output.toByteArray()
                        require(bytes.size <= vn.linkmap.data.BookmarkStore.MAX_BACKUP_BYTES) { "Bản sao lưu vượt quá 25 MB" }
                        bytes.toString(Charsets.UTF_8)
                    }
                }
                val count = if(text.trimStart().startsWith("{")) activeStore.importJson(text) else activeStore.importBrowser(text)
                switchDemo(false);app.checkDaily(); message("Đã thêm $count bookmark; các link trùng được bỏ qua")
            } catch (e: Exception) { message(e.message ?: "Không đọc được bản sao lưu") }
        }
    }
    Box(Modifier.fillMaxSize()) {
    StarField(Modifier.fillMaxSize(), background!="Tĩnh", if(background=="Rõ" && tab==0) 1f else .38f)
    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal=16.dp,vertical=8.dp)) {
                StitchBrandHeader(profileInitials,bookmarks.any { !it.isNote && it.health in setOf(Health.UNCERTAIN,Health.UNAVAILABLE) && reading[it.id]?.ignored!=true },
                    onAttention={filterName=LibraryFilter.ATTENTION.name;query="";collection="";tab=1},onProfile={tab=2},
                    onView={listMode=!listMode;tab=0;scope.launch { activeStore.setMeta("listMode",listMode.toString()) }})
                Spacer(Modifier.height(8.dp))
                StitchSearch(query,onQuery={query=it},onFilter={showFilters=!showFilters},onCenter={mapSelectionId=null;focusId=null;tab=0;listMode=false;overviewRequest++})
                if(tab<2) LazyRow(state=collectionScroll,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected=collection.isBlank(),onClick={collection=""},label={Text("Thiên hà (${bookmarks.size} sao)",style=MaterialTheme.typography.labelMedium)},leadingIcon={Box(Modifier.size(5.dp).background(StitchCyan,MaterialTheme.shapes.extraLarge))},shape=MaterialTheme.shapes.extraLarge,colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Blue.copy(alpha=.12f),selectedLabelColor=Blue),border=androidx.compose.foundation.BorderStroke(1.dp,if(collection.isBlank()) Blue.copy(alpha=.55f) else Muted.copy(alpha=.18f))) }
                    items(bookmarks.map { it.category }.distinct().sorted()) { name -> FilterChip(selected=collection==name,onClick={collection=if(collection==name) "" else name},label={Text(name,style=MaterialTheme.typography.labelMedium)},shape=MaterialTheme.shapes.extraLarge,colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Blue.copy(alpha=.12f),selectedLabelColor=Blue),border=androidx.compose.foundation.BorderStroke(1.dp,if(collection==name) Blue.copy(alpha=.55f) else Muted.copy(alpha=.18f))) }
                }
                if(showFilters && tab<2) LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(LibraryFilter.entries) { f -> FilterChip(selected=filter==f,onClick={filterName=f.name},label={Text(f.label)}) }
                }
                if(tab<2) Row(verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={switchDemo(!demoMode)}) { Text(if(demoMode) "Demo · 100 sao • Về thư viện" else "Xem 100 link demo",style=MaterialTheme.typography.labelSmall) }
                    if(demoMode) Text("Trạng thái minh họa",color=Muted,style=MaterialTheme.typography.labelSmall)
                }
            }
        },
        bottomBar = {
            StitchBottomBar(tab,onTab={tab=it},onAdd={if(demoMode) switchDemo(false);add=true})
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!demoMode && progress.running) {
                LinearProgressIndicator(progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total }, modifier = Modifier.fillMaxWidth())
                Text("Đang kiểm tra ${progress.done}/${progress.total}", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            } else if (!demoMode && progress.text.isNotEmpty() && tab==2) Text(progress.text, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            if (!ready) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else when (tab) {
                0,1 -> Column(Modifier.fillMaxSize()) {
                    if(bookmarks.isEmpty()) EmptyMap { add=true }
                    else if(tab==1 || listMode || query.isNotBlank()) BookmarkList(results,reading,onSelect={if(it.isNote) openLink(it) else selectedId=it.id},onOpen={openLink(it)},onLocate={
                        query="";tab=0;listMode=false;selectedId=null;focusId=it.id;mapSelectionId=it.id
                    })
                    else Box(Modifier.weight(1f)) {
                        cameraState.SaveableStateProvider(activeStore.databaseName+if(demoMode) "-demo-v4" else "-galaxy-v4") {
                            GalaxyMap(bookmarks,mapSelectionId,focusId,background!="Tĩnh",onSelect={mapSelectionId=it},onFocusConsumed={focusId=null},highlighted=results.map { it.id }.toSet(),reading=reading,starTiles=starTiles,twinkle=twinkle,onOpen={id -> bookmarks.find { it.id==id }?.let { openLink(it) }},onDetails={selectedId=it},overviewRequest=overviewRequest)
                        }
                        if(results.isEmpty()) Text("Không có link phù hợp bộ lọc",modifier=Modifier.align(Alignment.TopCenter).background(Space).padding(12.dp),color=Muted)
                    }
                }
                2 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Không gian của bạn", style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick={switchDemo(!demoMode);tab=0}) { Text(if(demoMode) "Về thư viện của tôi" else "Khám phá 100 link demo") }
                    Text("Demo có ngày thêm và màu trạng thái minh họa. Thư viện thật, bản sao lưu và Google không nhận dữ liệu demo tự động.",color=Muted)
                    GoogleSyncPanel(app)
                    SettingsCard("Kiểm tra mỗi ngày", "Lần mở app đầu ngày tự kiểm tra bằng kết nối của thiết bị. Những lần sau dùng kết quả đã lưu. Không chạy lịch nền.")
                    Text("Hoàn tất gần nhất: ${lastDay ?: "Chưa có"}\n${storedBookmarks.size} bookmark được lưu trên máy", color = Muted)
                    OutlinedButton(onClick = { app.checkDaily(force = true) }, enabled = !progress.running) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Kiểm tra lại toàn bộ ngay") }
                    Text("Hình bookmark trên bản đồ",style=MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(true to "Sao 5 cánh",false to "Ô vuông").forEach { (star,label) ->
                            FilterChip(selected=starTiles==star,onClick={starTiles=star;scope.launch { activeStore.setMeta("tileShape",if(star) "star5" else "square") }},label={Text(label)})
                        }
                    }
                    Text("Đổi hình vẫn giữ vị trí, icon và màu trạng thái. Vùng chạm bao quanh cả ngôi sao.",color=Muted,style=MaterialTheme.typography.bodySmall)
                    Text("Nền thiên hà",style=MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("Rõ","Dịu","Tĩnh").forEach { mode -> FilterChip(selected=background==mode,onClick={background=mode;scope.launch { activeStore.setMeta("background",mode) }},label={Text(mode)}) }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Viền sao lấp lánh chậm");Text("Viền sáng trắng và quầng màu, nhịp riêng 8–14 giây. Điều khiển riêng với nền; dừng khi app ở nền hoặc hệ thống tắt chuyển động.",color=Muted,style=MaterialTheme.typography.bodySmall) }
                        Switch(twinkle,{value -> twinkle=value;scope.launch { activeStore.setMeta("twinkle",value.toString()) }})
                    }
                    Text("Sao mới lớn, sao cũ nhỏ dần theo ngày thêm; sao lâu năm vẫn có kích thước tối thiểu. Ghim/lịch sử/bỏ qua cảnh báo lưu trên thiết bị.",color=Muted)
                    OutlinedButton(onClick={showTrash=true}) { Text("Đã xóa (${trash.size}) · Khôi phục") }
                    HorizontalDivider()
                    Text("Sao lưu của bạn", style = MaterialTheme.typography.titleLarge)
                    Text("Xuất JSON qua trình chọn tệp Android. Có thể chọn Google Drive nếu Drive xuất hiện trong trình chọn tệp. Đây là sao lưu thủ công, chưa phải đồng bộ tự động.", color = Muted)
                    Button(onClick = { export.launch("LinkMap-${DailyPolicy.day(System.currentTimeMillis())}.json") }) { Text("Xuất bản sao lưu") }
                    OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "text/html", "application/octet-stream")) }) { Text("Nhập JSON / bookmark HTML trình duyệt") }
                    HorizontalDivider()
                    SettingsCard("Thêm từ ứng dụng khác", "Chọn link → Chia sẻ → Thêm vào LinkMap. Với app hỗ trợ, bôi đen URL → menu ⋮ → Thêm vào LinkMap. Android không cho LinkMap theo dõi mọi lần Copy khi chạy nền.")
                    SettingsCard("Màu thể hiện lần kiểm tra gần nhất", "Xanh: máy chủ phản hồi được. Đỏ: lỗi truy cập có bằng chứng. Vàng: cần xác minh / đăng nhập / lỗi tạm thời. Xám: chưa kiểm tra. Link riêng tư không thể luôn xác nhận bằng HTTP công khai.")
                    Text("LinkMap Galaxy 0.7.0 • Android native\nDữ liệu trên thiết bị và Drive của bạn", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    }
    if (selected != null) ModalBottomSheet(onDismissRequest = { selectedId = null }, containerColor = Panel) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(selected.provider, selected.health,selected.isNote)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) { Text(selected.title, style = MaterialTheme.typography.titleLarge); Text("${if(selected.isNote) "Ghi chú" else selected.provider.label} · ${selected.category}", color = Muted) }
            }
            BookmarkPreview(reading[selected.id]?.preview.orEmpty())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=reading[selected.id]?.pinned==true,onClick={setReading(selected.id) { it.copy(pinned=!it.pinned) }},label={Text(if(reading[selected.id]?.pinned==true) "Đã ghim" else if(selected.isNote) "Ghim note" else "Ghim link")})
                TextButton(onClick={focusId=selected.id;mapSelectionId=selected.id;query="";tab=0;listMode=false;selectedId=null}) { Text("Xem trên bản đồ") }
            }
            if(!selected.isNote) Text(selected.url, color = Blue, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if(!selected.isNote) Text("${healthSymbol(selected.health)} ${selected.health.label}", color = selected.health.color(), fontWeight = FontWeight.Bold)
            if(!selected.isNote) Text(selected.reason, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text("Đã thêm: ${DateFormat.getDateInstance().format(Date(selected.createdAt))} · Sao thu nhỏ dần theo thời gian",color=Muted,style=MaterialTheme.typography.labelSmall)
            if(!selected.isNote) Text(if (selected.checkedAt == 0L) "Chưa kiểm tra" else "Kiểm tra: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(selected.checkedAt))}", color = Muted, style = MaterialTheme.typography.labelSmall)
            if (selected.note.isNotBlank()) androidx.compose.foundation.text.selection.SelectionContainer { Text(selected.note) }
            Button(onClick = { if(selected.isNote) {editing=selected;selectedId=null} else openLink(selected) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text(if(selected.isNote) "Chỉnh sửa ghi chú" else "Mở liên kết") }
            if(!demoMode && !selected.isNote) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(enabled=!progress.running,onClick={app.checkDaily(force=true,onlyId=selected.id)}) { Text("Kiểm tra lại link") }
                if(selected.health in setOf(Health.UNCERTAIN,Health.UNAVAILABLE)) TextButton(onClick={ setReading(selected.id) { it.copy(ignored=!it.ignored) } }) { Text(if(reading[selected.id]?.ignored==true) "Theo dõi lại" else "Bỏ qua cảnh báo") }
            }
            Text(if((reading[selected.id]?.openedAt ?: 0L)==0L) "Chưa mở qua LinkMap" else "Lần mở gần nhất: ${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(reading.getValue(selected.id).openedAt))}",color=Muted,style=MaterialTheme.typography.labelSmall)
            if(demoMode) {
                Text("Thêm minh họa: ${DateFormat.getDateInstance().format(Date(selected.createdAt))} · Không đồng bộ",color=Muted)
                OutlinedButton(onClick={scope.launch {
                    try { val copy=storedBookmarks.find { it.url==selected.url } ?: app.save(selected.url,selected.title,"",selected.category);switchDemo(false);selectedId=copy.id;mapSelectionId=copy.id;focusId=copy.id }
                    catch(e:Exception) {message(e.message ?: "Chưa lưu được")}
                }}) {Text("Lưu link này vào thư viện của tôi")}
            }
            if(!demoMode) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { editing = selected; selectedId = null }) { Text("Chỉnh sửa") }
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("LinkMap", if(selected.isNote) selected.note else selected.url)); message(if(selected.isNote) "Đã sao chép ghi chú" else "Đã sao chép URL")
                }) { Text(if(selected.isNote) "Sao chép nội dung" else "Sao chép") }
                TextButton(onClick = { delete = selected; selectedId = null }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (add || editing != null) BookmarkEditor(bookmark = editing, categories=storedBookmarks.map {it.category}.distinct(), onDismiss = { add = false; editing = null }, onSave = { url, title, note, group ->
        val duplicate=if(editing?.isNote==true) null else bookmarks.find { !it.isNote && it.url==Urls.fromInput(url) && it.id!=editing?.id }
        val item = if(duplicate!=null) { message("Bạn đã lưu link này — đang mở mục cũ"); duplicate } else app.saveContent(url, title, note, group, editing?.id)
        add = false; editing = null; tab = 0; query="";collection="";filterName=LibraryFilter.ALL.name;focusId = item.id;selectedId=item.id;mapSelectionId=item.id
        if(duplicate==null) scope.launch {
            if(snackbar.showSnackbar("Đã lưu vào LinkMap", "Đổi tên / bộ sưu tập", duration=SnackbarDuration.Long)==SnackbarResult.ActionPerformed) { selectedId=null;editing=item }
        }
    })
    if(showTrash) ModalBottomSheet(onDismissRequest={showTrash=false},containerColor=Panel) {
        Text("Đã xóa · ${trash.size} link",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(20.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=500.dp).navigationBarsPadding()) {
            if(trash.isEmpty()) item { Text("Chưa có mục đã xóa",modifier=Modifier.padding(24.dp)) }
            items(trash,key={it.id}) { b -> ListItem(headlineContent={Text(b.title)},supportingContent={Text(if(b.isNote) b.note else b.url,maxLines=1,overflow=TextOverflow.Ellipsis)},trailingContent={TextButton(onClick={scope.launch { try { activeStore.restore(b.id);message("Đã khôi phục") } catch(e:Exception) {message(e.message ?: "Chưa khôi phục được")} }}) { Text("Khôi phục") }},colors=ListItemDefaults.colors(containerColor=Color.Transparent)) }
        }
    }
    delete?.let { item -> AlertDialog(onDismissRequest = { delete = null }, title = { Text("Chuyển vào Đã xóa?") }, text = { Text(item.title) },
        confirmButton = { TextButton(onClick = { scope.launch { delete = null; removeItem(item) } }) { Text("Xóa") } },
        dismissButton = { TextButton(onClick = { delete = null }) { Text("Hủy") } }) }
}

@Composable private fun SettingsCard(title: String, text: String) {
    Surface(color = Panel, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium) }
    }
}
@Composable private fun EmptyMap(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.AutoAwesome, null, tint = Blue, modifier = Modifier.size(54.dp))
            Text("Thiên hà của bạn bắt đầu từ một link hoặc note", style = MaterialTheme.typography.headlineSmall)
            Text("Dán link, ý tưởng hoặc ghi chú. LinkMap tự nhận diện nội dung và thêm vì sao mới.", color = Muted)
            Button(onClick = onAdd) { Text("Thêm bookmark đầu tiên") }
            Text("Hoặc chia sẻ một URL từ ứng dụng khác vào LinkMap", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}
fun platformLetter(p: Provider) = when (p) {
    Provider.FACEBOOK -> "f"; Provider.TIKTOK -> "♪"; Provider.GITHUB -> "GH"; Provider.DRIVE -> "△"
    Provider.ONEDRIVE -> "☁"; Provider.GOOGLE -> "G"; Provider.OTHER -> "↗"
}
fun healthSymbol(h: Health) = when (h) { Health.REACHABLE -> "✓"; Health.UNAVAILABLE -> "×"; Health.UNCERTAIN -> "?"; Health.UNCHECKED -> "·" }
