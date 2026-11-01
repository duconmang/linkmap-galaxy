package vn.linkmap.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import vn.linkmap.core.*
import java.net.URI
import java.util.UUID

class BookmarkStore(context: Context, name: String = "linkmap.db") : SQLiteOpenHelper(context, name, null, 5) {
    companion object { const val MAX_BACKUP_BYTES=25*1024*1024 }
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val mutable = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks = mutable.asStateFlow()
    private val dirtyMutable = MutableStateFlow(0L)
    val changes = dirtyMutable.asStateFlow()
    private val readingMutable = MutableStateFlow<Map<String, ReadingState>>(emptyMap())
    val reading = readingMutable.asStateFlow()
    private val trashMutable = MutableStateFlow<List<Bookmark>>(emptyList())
    val trash = trashMutable.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE bookmarks (
            id TEXT PRIMARY KEY, url TEXT UNIQUE, title TEXT NOT NULL,
            note TEXT NOT NULL, category TEXT NOT NULL, provider TEXT NOT NULL, slot INTEGER NOT NULL,
            health TEXT NOT NULL, reason TEXT NOT NULL, checkedAt INTEGER NOT NULL,
            httpCode INTEGER, notFoundAt INTEGER NOT NULL, createdAt INTEGER NOT NULL, kind TEXT NOT NULL DEFAULT 'LINK')""")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        createSyncTables(db)
        createReadingTable(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) createReadingTable(db)
        if (oldVersion < 2) {
            // SQLiteOpenHelper wraps migration in a transaction. Preserve all fields except layout slots.
            val ids = db.rawQuery("SELECT id FROM bookmarks ORDER BY createdAt, id", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            ids.forEachIndexed { slot, id ->
                db.update("bookmarks", ContentValues().apply { put("slot", slot) }, "id = ?", arrayOf(id))
            }
        }
        if (oldVersion < 3) {
            createSyncTables(db)
            val old = db.rawQuery("SELECT * FROM bookmarks ORDER BY createdAt,id",null).use { c -> buildList { while(c.moveToNext()) add(c.bookmark()) } }
            old.forEach { b ->
                val item = b.copy(id = SyncRules.id(b.url))
                db.update("bookmarks",ContentValues().apply { put("id",item.id) },"id=?",arrayOf(b.id))
                enqueue(SyncRecord(item,Revision(nextClock(db),deviceId(db)),healthDevice = deviceId(db)),db)
            }
        }
        if(oldVersion<5) migrateEntries(db)
    }
    private fun migrateEntries(db: SQLiteDatabase) {
        val columns="id,url,title,note,category,provider,slot,health,reason,checkedAt,httpCode,notFoundAt,createdAt"
        val hasKind=db.rawQuery("PRAGMA table_info(bookmarks)",null).use { c -> var found=false;while(c.moveToNext()) if(c.getString(1)=="kind") found=true;found }
        // Retain the previous table as an on-device migration safety copy; do not drop user rows.
        db.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_before_notes_v4")
        db.execSQL("""CREATE TABLE bookmarks (id TEXT PRIMARY KEY,url TEXT UNIQUE,title TEXT NOT NULL,
            note TEXT NOT NULL,category TEXT NOT NULL,provider TEXT NOT NULL,slot INTEGER NOT NULL,
            health TEXT NOT NULL,reason TEXT NOT NULL,checkedAt INTEGER NOT NULL,httpCode INTEGER,
            notFoundAt INTEGER NOT NULL,createdAt INTEGER NOT NULL,kind TEXT NOT NULL DEFAULT 'LINK')""")
        db.execSQL("INSERT INTO bookmarks ($columns,kind) SELECT $columns,${if(hasKind) "kind" else "'LINK'"} FROM bookmarks_before_notes_v4")
    }
    private fun createReadingTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS reading (id TEXT PRIMARY KEY, pinned INTEGER NOT NULL DEFAULT 0, openedAt INTEGER NOT NULL DEFAULT 0, ignored INTEGER NOT NULL DEFAULT 0, preview TEXT NOT NULL DEFAULT '')")
    }
    private fun createSyncTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sync_state (id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE sync_outbox (seq INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE sync_seen (id TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE sync_hidden (id TEXT PRIMARY KEY)")
    }
    private fun metaValue(db: SQLiteDatabase,key:String):String? = db.rawQuery("SELECT value FROM meta WHERE key=?",arrayOf(key)).use { if(it.moveToFirst()) it.getString(0) else null }
    private fun putMeta(db:SQLiteDatabase,key:String,value:String) { db.execSQL("INSERT OR REPLACE INTO meta VALUES (?,?)",arrayOf(key,value)) }
    private fun deviceId(db:SQLiteDatabase):String = metaValue(db,"deviceId") ?: UUID.randomUUID().toString().also { putMeta(db,"deviceId",it) }
    private fun nextClock(db:SQLiteDatabase):Long = ((metaValue(db,"clock")?.toLongOrNull() ?: 0L)+1).also { putMeta(db,"clock",it.toString()) }
    private fun state(id:String,db:SQLiteDatabase=readableDatabase):SyncRecord? = db.rawQuery("SELECT payload FROM sync_state WHERE id=?",arrayOf(id)).use { if(it.moveToFirst()) SyncJson.decode(JSONObject(it.getString(0))) else null }
    private fun enqueue(record:SyncRecord,db:SQLiteDatabase=writableDatabase) {
        val json=SyncJson.encode(record).toString()
        db.execSQL("INSERT OR REPLACE INTO sync_state VALUES (?,?)",arrayOf(record.bookmark.id,json))
        db.execSQL("INSERT INTO sync_outbox(payload) VALUES (?)",arrayOf(json)); dirtyMutable.value++
    }
    private fun <T> atomic(block:()->T):T {
        val db=writableDatabase; db.beginTransaction()
        try { return block().also { db.setTransactionSuccessful() } } finally { db.endTransaction() }
    }

    suspend fun load() = withContext(io) { refresh() }
    private fun refresh() {
        mutable.value = readableDatabase.query("bookmarks", null, "id NOT IN (SELECT id FROM sync_hidden)", null, null, null, "createdAt, id")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.bookmark()) } }
        readingMutable.value = readableDatabase.rawQuery("SELECT id,pinned,openedAt,ignored,preview FROM reading", null).use { c ->
            buildMap { while(c.moveToNext()) put(c.getString(0), ReadingState(c.getInt(1)!=0,c.getLong(2),c.getInt(3)!=0,c.getString(4))) }
        }
        trashMutable.value = readableDatabase.rawQuery("SELECT payload FROM sync_state WHERE id IN (SELECT id FROM sync_hidden)", null).use { c ->
            buildList { while(c.moveToNext()) { val record=SyncJson.decode(JSONObject(c.getString(0))); if(record.deleted) add(record.bookmark) } }
        }.sortedByDescending { it.createdAt }
    }
    private fun Cursor.bookmark(): Bookmark {
        fun s(key: String) = getString(getColumnIndexOrThrow(key)).orEmpty()
        fun l(key: String) = getLong(getColumnIndexOrThrow(key))
        return Bookmark(s("id"), s("url"), s("title"), s("note"), s("category"),
            Provider.valueOf(s("provider")), l("slot").toInt(), Health.valueOf(s("health")), s("reason"),
            l("checkedAt"), getColumnIndexOrThrow("httpCode").let { if (isNull(it)) null else getInt(it) }, l("notFoundAt"), l("createdAt"), kind=getColumnIndex("kind").let { if(it<0) EntryKind.LINK else EntryKind.valueOf(getString(it)) })
    }
    private fun Bookmark.values() = ContentValues().apply {
        put("id", id); if(isNote) putNull("url") else put("url", url); put("kind",kind.name); put("title", title); put("note", note); put("category", category)
        put("provider", provider.name); put("slot", slot); put("health", health.name); put("reason", reason)
        put("checkedAt", checkedAt); if (httpCode == null) putNull("httpCode") else put("httpCode", httpCode)
        put("notFoundAt", notFoundAt); put("createdAt", createdAt)
    }
    private fun writeBookmark(db: SQLiteDatabase, b: Bookmark) {
        require(if(b.isNote) b.url.isEmpty() else Urls.normalize(b.url)!=null) { "Loại nội dung hoặc URL không hợp lệ" }
        require(b.isNote || !db.rawQuery("SELECT id FROM bookmarks WHERE url=? AND id<>?",arrayOf(b.url,b.id)).use { it.moveToFirst() }) { "URL này đã được lưu; hãy khôi phục mục cũ trong Đã xóa" }
        val exists=db.rawQuery("SELECT id FROM bookmarks WHERE id=?",arrayOf(b.id)).use { it.moveToFirst() }
        if(exists) db.update("bookmarks",b.values(),"id=?",arrayOf(b.id)) else db.insertOrThrow("bookmarks",null,b.values())
    }
    private fun nextSlot(): Int = readableDatabase.rawQuery(
        "SELECT COALESCE(MAX(slot), -1) + 1 FROM bookmarks", null
    ).use { it.moveToFirst(); it.getInt(0) }

    suspend fun save(url: String, title: String, note: String, category: String, id: String? = null): Bookmark = withContext(io) {
        atomic {
        val normalized = requireNotNull(Urls.fromInput(url)) { "Nhập một địa chỉ web hợp lệ, ví dụ github.com/android" }
        val previous = mutable.value.find { it.id == id }
        require(previous?.isNote!=true) { "Hãy chỉnh sửa mục này dưới dạng ghi chú" }
        require(mutable.value.none { !it.isNote && it.url == normalized && it.id != id }) { "Link này đã có trên bản đồ" }
        val provider = Provider.forUrl(normalized)
        val changed = previous?.url != normalized
        val item = Bookmark(
            id = previous?.id ?: SyncRules.id(normalized), url = normalized,
            title = title.trim().take(200).ifBlank { Urls.defaultTitle(normalized) }, note = note.also { require(it.length<=IncomingContent.MAX_LENGTH) { "Nội dung tối đa 64.000 ký tự" } },
            category = category.trim().take(50).ifBlank { "Cá nhân" }, provider = provider,
            slot = previous?.slot ?: nextSlot(),
            health = if (changed) Health.UNCHECKED else previous!!.health,
            reason = if (changed) "Chưa có kết quả" else previous!!.reason,
            checkedAt = if (changed) 0 else previous!!.checkedAt,
            httpCode = if (changed) null else previous!!.httpCode,
            notFoundAt = if (changed) 0 else previous!!.notFoundAt,
            createdAt = previous?.createdAt ?: System.currentTimeMillis()
        )
        writeBookmark(writableDatabase,item)
        if(changed) {
            val oldReading=readingMutable.value[item.id] ?: ReadingState()
            writableDatabase.execSQL("INSERT OR REPLACE INTO reading VALUES (?,?,?,?,?)",arrayOf<Any>(item.id,if(oldReading.pinned) 1 else 0,0L,0,""))
        }
        writableDatabase.delete("sync_hidden","id=?",arrayOf(item.id))
        enqueue(SyncRecord(item,Revision(nextClock(writableDatabase),deviceId(writableDatabase)),healthDevice=if(changed) "" else state(item.id)?.healthDevice.orEmpty()))
        refresh(); item
        }
    }
    suspend fun saveNote(body:String,title:String="",category:String="Cá nhân",id:String?=null): Bookmark = withContext(io) { atomic {
        require(body.isNotBlank()) { "Ghi chú chưa có nội dung" }
        require(body.length<=IncomingContent.MAX_LENGTH) { "Ghi chú tối đa 64.000 ký tự" }
        val previous=mutable.value.find { it.id==id }
        require(id==null || previous?.isNote==true) { "Ghi chú không còn tồn tại hoặc đã đổi loại" }
        val b=Bookmark(id=previous?.id ?: "note-${UUID.randomUUID()}",url="",title=title.trim().take(200).ifBlank { body.lineSequence().first { it.isNotBlank() }.trim().take(120) },
            note=body,category=category.trim().take(50).ifBlank { "Cá nhân" },provider=Provider.OTHER,slot=previous?.slot ?: nextSlot(),
            reason="Ghi chú được lưu trên thiết bị, không cần kiểm tra link",createdAt=previous?.createdAt ?: System.currentTimeMillis(),kind=EntryKind.NOTE)
        writeBookmark(writableDatabase,b)
        writableDatabase.delete("sync_hidden","id=?",arrayOf(b.id))
        enqueue(SyncRecord(b,Revision(nextClock(writableDatabase),deviceId(writableDatabase))))
        refresh();b
    } }
    suspend fun delete(id: String) = withContext(io) {
        atomic {
            state(id)?.let { enqueue(it.copy(revision=Revision(nextClock(writableDatabase),deviceId(writableDatabase)),deleted=true)) }
            writableDatabase.execSQL("INSERT OR IGNORE INTO sync_hidden VALUES (?)",arrayOf(id)); refresh()
        }
    }
    suspend fun restore(id: String) = withContext(io) { atomic {
        val old = requireNotNull(state(id)) { "Không tìm thấy mục đã xóa" }
        require(old.bookmark.isNote || mutable.value.none { !it.isNote && it.url == old.bookmark.url && it.id != id }) { "URL này đã có trong thư viện" }
        val db=writableDatabase
        val slot=db.rawQuery("SELECT slot FROM bookmarks WHERE id=?",arrayOf(id)).use { if(it.moveToFirst()) it.getInt(0) else nextSlot() }
        val b=old.bookmark.copy(slot=slot)
        writeBookmark(db,b)
        db.delete("sync_hidden","id=?",arrayOf(id))
        enqueue(old.copy(bookmark=b,deleted=false,revision=Revision(nextClock(db),deviceId(db))))
        refresh()
    } }
    suspend fun setReading(id: String, transform: (ReadingState) -> ReadingState) = withContext(io) {
        val v=transform(readingMutable.value[id] ?: ReadingState())
        writableDatabase.execSQL("INSERT OR REPLACE INTO reading VALUES (?,?,?,?,?)",arrayOf(id,if(v.pinned) 1 else 0,v.openedAt,if(v.ignored) 1 else 0,v.preview))
        refresh()
    }
    suspend fun enrich(item: Bookmark, title: String?, preview: String?) = withContext(io) { atomic {
        val current=mutable.value.find { !it.isNote && it.id==item.id && it.url==item.url } ?: return@atomic
        // Metadata never overwrites a title edited while the network request was running.
        if(!title.isNullOrBlank() && current.title==Urls.defaultTitle(current.url)) {
            val updated=current.copy(title=title.take(200))
            writableDatabase.update("bookmarks",updated.values(),"id=?",arrayOf(item.id))
            state(item.id)?.let { enqueue(it.copy(bookmark=updated,revision=Revision(nextClock(writableDatabase),deviceId(writableDatabase)))) }
        }
        if(!preview.isNullOrBlank()) {
            val v=readingMutable.value[item.id] ?: ReadingState()
            writableDatabase.execSQL("INSERT OR REPLACE INTO reading VALUES (?,?,?,?,?)",arrayOf(item.id,if(v.pinned) 1 else 0,v.openedAt,if(v.ignored) 1 else 0,preview))
        }
        refresh()
    } }
    suspend fun importBrowser(html: String): Int = withContext(io) {
        val items=Library.browserBookmarks(html)
        require(items.isNotEmpty()) { "Không tìm thấy bookmark web hợp lệ trong tệp HTML" }
        importJson(JSONObject().put("schemaVersion",1).put("bookmarks",JSONArray().apply {
            items.forEach { (url,title) -> put(JSONObject().put("url",url).put("title",title).put("category","Đã nhập")) }
        }).toString())
    }
    suspend fun record(item: Bookmark, result: CheckResult, now: Long) = withContext(io) {
        atomic {
        if(item.isNote || mutable.value.none { !it.isNote && it.id==item.id && it.url==item.url }) return@atomic
        // A check must not recreate a deleted item or overwrite results for a newly edited URL.
        writableDatabase.update("bookmarks", ContentValues().apply {
            put("health", result.health.name); put("reason", result.reason); put("checkedAt", now)
            if (result.code == null) putNull("httpCode") else put("httpCode", result.code)
            put("notFoundAt", result.notFoundAt)
        }, "id = ? AND url = ?", arrayOf(item.id, item.url))
        refresh()
        state(item.id)?.let { old -> mutable.value.find { it.id==item.id }?.let { updated -> enqueue(old.copy(bookmark=updated,healthDevice=deviceId(writableDatabase))) } }
        }
    }
    suspend fun meta(key: String): String? = withContext(io) {
        readableDatabase.query("meta", arrayOf("value"), "key = ?", arrayOf(key), null, null, null).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }
    suspend fun setMeta(key: String, value: String) = withContext(io) {
        writableDatabase.insertWithOnConflict("meta", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }
    suspend fun exportJson(): String = withContext(io) {
        JSONObject().put("schemaVersion", 2).put("exportedAt", System.currentTimeMillis())
            .put("bookmarks", JSONArray().apply {
                mutable.value.forEach { b -> val r=readingMutable.value[b.id] ?: ReadingState(); put(JSONObject().put("id",b.id).put("kind",b.kind.name).put("url",if(b.isNote) JSONObject.NULL else b.url).put("title", b.title).put("note", b.note).put("category", b.category).put("createdAt",b.createdAt).put("pinned",r.pinned).put("openedAt",r.openedAt).put("ignored",r.ignored)) }
            }).toString(2).also { require(it.toByteArray(Charsets.UTF_8).size<=MAX_BACKUP_BYTES) { "Bản sao lưu vượt 25 MB; dữ liệu trên máy vẫn được giữ" } }
    }
    suspend fun importJson(text: String): Int = withContext(io) {
        require(text.toByteArray(Charsets.UTF_8).size<=MAX_BACKUP_BYTES) { "Bản sao lưu vượt quá 25 MB" }
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") in 1..2) { "Phiên bản bản sao lưu chưa được hỗ trợ" }
        val entries = root.getJSONArray("bookmarks")
        require(entries.length() <= 10000) { "Mỗi lần nhập tối đa 10.000 link" }
        // Validate everything before writing. Import adds only; never deletes or replaces existing data.
        val parsed=(0 until entries.length()).map { i ->
            val o=entries.getJSONObject(i)
            val kind=EntryKind.valueOf(o.optString("kind","LINK"))
            val url=if(kind==EntryKind.NOTE) "" else requireNotNull(Urls.normalize(o.getString("url"))) { "URL không hợp lệ ở mục ${i+1}" }
            require(url.length<=8192)
            val body=o.optString("note");require(body.length<=IncomingContent.MAX_LENGTH)
            if(kind==EntryKind.NOTE) require(body.isNotBlank())
            val suppliedId=o.optString("id")
            val id=if(suppliedId.isBlank()) { if(kind==EntryKind.LINK) SyncRules.id(url) else "note-${UUID.randomUUID()}" }
                else suppliedId.also { require(it.matches(if(kind==EntryKind.LINK) Regex("url-[a-f0-9]{64}") else Regex("note-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))) { "ID trong bản sao lưu không hợp lệ" } }
            Bookmark(id,url,o.optString("title").take(200).ifBlank { if(kind==EntryKind.NOTE) body.lineSequence().first { it.isNotBlank() }.take(120) else Urls.defaultTitle(url) },
                note=body,category=o.optString("category","Cá nhân").take(50),kind=kind,createdAt=o.optLong("createdAt",System.currentTimeMillis()).coerceAtLeast(0)) to o
        }.distinctBy { it.first.id }
        var count = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existingUrls=db.rawQuery("SELECT url FROM bookmarks WHERE url IS NOT NULL",null).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }.toMutableSet()
            val existingIds=db.rawQuery("SELECT id FROM bookmarks",null).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }.toMutableSet()
            parsed.forEach { (entry,o) ->
                if(entry.id !in existingIds && (entry.isNote || entry.url !in existingUrls)) {
                    val b=entry.copy(slot=nextSlot())
                    writeBookmark(db,b);existingIds.add(b.id);if(!b.isNote) existingUrls.add(b.url)
                    db.execSQL("INSERT OR REPLACE INTO reading VALUES (?,?,?,?,?)",arrayOf<Any>(b.id,if(o.optBoolean("pinned")) 1 else 0,o.optLong("openedAt").coerceAtLeast(0),if(o.optBoolean("ignored")) 1 else 0,""));count++
                    enqueue(SyncRecord(b,Revision(nextClock(db),deviceId(db))),db)
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        refresh(); count
    }
    suspend fun pending():List<Pair<Long,String>> = withContext(io) { readableDatabase.rawQuery("SELECT seq,payload FROM sync_outbox ORDER BY seq LIMIT 100",null).use { c -> buildList { while(c.moveToNext()) add(c.getLong(0) to c.getString(1)) } } }
    suspend fun acknowledge(ids:List<Long>) = withContext(io) { atomic { ids.forEach { writableDatabase.delete("sync_outbox","seq=?",arrayOf(it.toString())) } } }
    suspend fun seen(id:String):Boolean = withContext(io) { readableDatabase.rawQuery("SELECT id FROM sync_seen WHERE id=?",arrayOf(id)).use { it.moveToFirst() } }
    suspend fun mergeRemote(fileId:String,records:List<SyncRecord>) = withContext(io) { atomic {
        val db=writableDatabase
        records.forEach { remote ->
            val merged=SyncRules.merge(state(remote.bookmark.id),remote)
            db.execSQL("INSERT OR REPLACE INTO sync_state VALUES (?,?)",arrayOf(merged.bookmark.id,SyncJson.encode(merged).toString()))
            val clock=metaValue(db,"clock")?.toLongOrNull() ?: 0L
            if(remote.revision.counter>clock) putMeta(db,"clock",remote.revision.counter.toString())
            if(merged.deleted) db.execSQL("INSERT OR IGNORE INTO sync_hidden VALUES (?)",arrayOf(merged.bookmark.id))
            else {
                val existing=db.rawQuery("SELECT slot FROM bookmarks WHERE id=?",arrayOf(merged.bookmark.id)).use { if(it.moveToFirst()) it.getInt(0) else null }
                val collision=!merged.bookmark.isNote && db.rawQuery("SELECT id FROM bookmarks WHERE url=? AND id<>?",arrayOf(merged.bookmark.url,merged.bookmark.id)).use { it.moveToFirst() }
                // Keep conflicting content in sync_state; never replace another bookmark's URL/notes.
                if(!collision) {
                    val b=merged.bookmark.copy(slot=existing ?: nextSlot())
                    if(existing==null) db.insertOrThrow("bookmarks",null,b.values()) else db.update("bookmarks",b.values(),"id=?",arrayOf(b.id))
                    db.delete("sync_hidden","id=?",arrayOf(b.id))
                } else putMeta(db,"syncConflict","Một URL có nhiều bản chỉnh sửa; dữ liệu xung đột đã được giữ lại")
            }
        }
        db.execSQL("INSERT OR IGNORE INTO sync_seen VALUES (?)",arrayOf(fileId)); refresh()
    } }
}
