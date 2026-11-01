package vn.linkmap

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import vn.linkmap.core.*
import vn.linkmap.data.BookmarkStore
import vn.linkmap.data.LinkChecker

data class CheckProgress(val running: Boolean = false, val done: Int = 0, val total: Int = 0, val text: String = "")

class LinkMapApplication : Application() {
    private lateinit var storeMutable: MutableStateFlow<BookmarkStore>
    val stores get() = storeMutable.asStateFlow()
    val store get() = storeMutable.value
    lateinit var sync: GoogleSyncController
        private set
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gate = Mutex()
    private var rerunRequested = false
    private val checker = LinkChecker()
    private val _progress = MutableStateFlow(CheckProgress())
    val progress = _progress.asStateFlow()
    private val _lastDay = MutableStateFlow<String?>(null)
    val lastDay = _lastDay.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private var visibleCount = 0
    private var runningCheck: Job? = null
    private var foregroundSync: Job? = null
    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    override fun onCreate() {
        super.onCreate()
        storeMutable = MutableStateFlow(BookmarkStore(this))
        scope.launch { stores.collectLatest { library -> library.bookmarks.collect { LinkMapWidget.update(this@LinkMapApplication,it.size) } } }
        sync = GoogleSyncController(this)
        scope.launch {
            store.load(); _lastDay.value = store.meta("completedDay")
            sync.restore()
            _ready.value = true
            if (visibleCount > 0) checkDaily()
        }
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) scope.launch {
                    if (visibleCount > 0 && _ready.value) { checkDaily(); sync.trySync() }
                }
            }
        })
    }
    fun entered() {
        visibleCount++; if (_ready.value) checkDaily(); sync.trySync()
        if(foregroundSync?.isActive!=true) foregroundSync=scope.launch {
            while(isActive) {delay(60_000); if(_ready.value)sync.trySync()}
        }
    }
    fun left() {
        visibleCount = (visibleCount - 1).coerceAtLeast(0)
        if(visibleCount==0){foregroundSync?.cancel();foregroundSync=null}
    }
    fun online(): Boolean = connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

    fun checkDaily(force: Boolean = false, onlyId: String? = null) {
        scope.launch {
            if (!_ready.value) return@launch
            if (!gate.tryLock()) { rerunRequested = true; return@launch }
            runningCheck = coroutineContext.job
            try {
                val today = DailyPolicy.day(System.currentTimeMillis())
                val all = store.bookmarks.value.filterNot { it.isNote }
                val pending = if (onlyId!=null) all.filter { it.id==onlyId } else if (force) all else all.filter { !DailyPolicy.checkedToday(it, today) }
                if (!force && !DailyPolicy.due(_lastDay.value, today) && pending.isEmpty()) return@launch
                if (all.isEmpty()) { _progress.value = CheckProgress(text = "Thêm bookmark đầu tiên để bắt đầu"); return@launch }
                if (!online()) { _progress.value = CheckProgress(text = "Đang offline • giữ kết quả cũ, thử lại khi có mạng"); return@launch }
                _progress.value = CheckProgress(true, 0, pending.size, "Đang kiểm tra cho hôm nay")
                val hostLocks = mutableMapOf<String, Semaphore>()
                val slots = Semaphore(4)
                // Build host gates before spawning tasks, avoiding concurrent map writes.
                pending.forEach { hostLocks.getOrPut(java.net.URI(it.url).host) { Semaphore(1) } }
                coroutineScope {
                    pending.map { item -> async {
                        slots.withPermit {
                            hostLocks.getValue(java.net.URI(item.url).host).withPermit host@ {
                                if (!online()) return@host
                                val result = checker.check(item)
                                // Don't replace cached statuses with device-wide connectivity errors.
                                if (online()) {
                                    store.record(item, result, System.currentTimeMillis())
                                    val preview=result.previewUrl?.let { checker.cachePreview(it,java.io.File(cacheDir,"previews")) }
                                    store.enrich(item,result.title,preview)
                                }
                                _progress.value = _progress.value.copy(done = _progress.value.done + 1)
                            }
                        }
                    } }.awaitAll()
                }
                val remaining = store.bookmarks.value.any { !it.isNote && !DailyPolicy.checkedToday(it, today) }
                if (!remaining && online()) {
                    store.setMeta("completedDay", today); _lastDay.value = today
                    _progress.value = CheckProgress(text = "Đã kiểm tra hôm nay • $today")
                } else _progress.value = CheckProgress(text = "Chưa hoàn tất • sẽ tiếp tục khi mở app và có mạng")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _progress.value = CheckProgress(text = "Chưa hoàn tất kiểm tra • dữ liệu đã lưu vẫn được giữ") }
            finally {
                runningCheck = null; _progress.value = _progress.value.copy(running = false); gate.unlock()
                if (rerunRequested) { rerunRequested = false; checkDaily() }
            }
        }
    }
    suspend fun save(url: String, title: String, note: String, category: String, id: String? = null): Bookmark {
        val bookmark = store.save(url, title, note, category, id)
        checkDaily()
        return bookmark
    }
    suspend fun saveContent(content:String,title:String="",annotation:String="",category:String="Cá nhân",id:String?=null,chosenUrl:String?=null):Bookmark {
        val previous=store.bookmarks.value.find { it.id==id }
        if(previous?.isNote==true) return store.saveNote(content,title,category,id)
        val incoming=IncomingContent.detect(content)
        if(incoming.kind==EntryKind.NOTE) {
            require(previous==null) { "Nhập URL hợp lệ để sửa liên kết" }
            return store.saveNote(incoming.text,title,category)
        }
        val url=chosenUrl?.also { require(it in incoming.urls) } ?: incoming.urls.singleOrNull()
            ?: throw IllegalArgumentException("Nội dung có nhiều link; hãy chọn link muốn lưu")
        val body=if(incoming.text==url || Urls.fromInput(incoming.text)==url) annotation else incoming.text
        return save(url,title,body,category,id)
    }
    suspend fun openAccount(subject: String?) {
        runningCheck?.cancelAndJoin()
        gate.withLock {
            // Load before publishing the new store. Hiding the root here cancels the
            // Google panel's coroutine while it is still waiting for this switch.
            val name=if(subject==null) "linkmap.db" else "linkmap-${SyncRules.id(subject).takeLast(32)}.db"
            val next=BookmarkStore(this,name);next.load();storeMutable.value=next
            _lastDay.value=next.meta("completedDay");_progress.value=CheckProgress();_ready.value=true
        }
        if(visibleCount>0)checkDaily()
    }
}
