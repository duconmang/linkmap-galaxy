package vn.linkmap

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import vn.linkmap.core.*
import vn.linkmap.data.BookmarkStore
import vn.linkmap.data.SyncJson
import org.json.JSONObject
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StoreTest {
    @Test fun readingStateBackupAndTrashSurviveRestartAndRestoreKeepsSlot() = runBlocking {
        val b=store.save("https://example.com/reading","Đồ án","note","Học Android")
        store.setReading(b.id) { it.copy(pinned=true,openedAt=12345,ignored=true) }
        store.delete(b.id)
        assertEquals(b.id,store.trash.value.single().id)
        store.close();store=BookmarkStore(context,name);store.load()
        store.restore(b.id)
        assertEquals(b.slot,store.bookmarks.value.single().slot)
        assertEquals(12345L,store.reading.value.getValue(b.id).openedAt)
        assertTrue(store.reading.value.getValue(b.id).pinned)
        assertTrue(store.trash.value.isEmpty())
        val backup=JSONObject(store.exportJson()).getJSONArray("bookmarks").getJSONObject(0)
        assertTrue(backup.getBoolean("pinned"));assertEquals("Học Android",backup.getString("category"))
    }
    @Test fun metadataCannotOverwriteUserTitleOrApplyToAnEditedOrDeletedUrl() = runBlocking {
        val b=store.save("https://example.com/meta","","","Cá nhân")
        store.enrich(b,"Fetched title","/fixture.jpg")
        assertEquals("Fetched title",store.bookmarks.value.single().title)
        store.save(b.url,"User title","","Cá nhân",b.id)
        store.enrich(b,"Wrong override",null)
        assertEquals("User title",store.bookmarks.value.single().title)
        store.save("https://example.com/new-meta","","","Cá nhân",b.id)
        store.enrich(b,"Stale result","/not-applied.jpg")
        assertEquals(Urls.defaultTitle("https://example.com/new-meta"),store.bookmarks.value.single().title)
        assertTrue(store.reading.value[b.id]?.preview.isNullOrBlank())
        store.delete(b.id);store.enrich(b,"Late",null)
        assertTrue(store.bookmarks.value.isEmpty())
    }
    @Test fun versionThreeUpgradeAddsReadingWithoutChangingSyncedBookmarks() = runBlocking {
        val b=store.save("https://example.com/v3","Keep","Original note","Work")
        store.writableDatabase.execSQL("DROP TABLE reading")
        store.writableDatabase.version=3
        store.close();store=BookmarkStore(context,name);store.load()
        assertEquals(b,store.bookmarks.value.single())
        store.setReading(b.id) { it.copy(pinned=true) }
        assertTrue(store.reading.value.getValue(b.id).pinned)
        assertTrue(store.pending().isNotEmpty())
    }
    @Test fun htmlImportIgnoresDuplicatesAndRestoreDoesNotLoseNotes() = runBlocking {
        val b=store.save("https://example.com/existing","Keep","private note","Work")
        assertEquals(1,store.importBrowser("""<A HREF="https://example.com/existing">Replace</A><A HREF="https://example.com/new">New</A><A HREF="javascript:alert(1)">Bad</A>"""))
        assertEquals("private note",store.bookmarks.value.first { it.id==b.id }.note)
        store.delete(b.id);store.restore(b.id)
        assertEquals("private note",store.bookmarks.value.first { it.id==b.id }.note)
        val restore=SyncJson.decode(JSONObject(store.pending().last().second))
        assertFalse(restore.deleted)
    }
    @Test fun hiddenUrlCollisionFailsWithoutReplacingAnyRecord() = runBlocking {
        val b=store.save("https://example.com/old-id","Original","keep","Work")
        val changed=store.save("https://example.com/changed-id",b.title,b.note,b.category,b.id)
        store.delete(changed.id)
        try { store.save(changed.url,"Accidental replacement","","New");fail("Must preserve hidden record") } catch(_:IllegalArgumentException) {}
        assertEquals("keep",store.trash.value.single().note)
        store.restore(changed.id)
        assertEquals(b.id,store.bookmarks.value.single().id)
    }
    @Test fun thousandBookmarkImportSearchAndReopenStayConsistent() = runBlocking {
        val html=(0 until 1000).joinToString("") { "<A HREF='https://example.com/library-$it'>Tài liệu $it</A>" }
        val started=android.os.SystemClock.elapsedRealtime()
        assertEquals(1000,store.importBrowser(html))
        val elapsed=android.os.SystemClock.elapsedRealtime()-started
        println("LINKMAP_IMPORT_1000_MS=$elapsed")
        assertEquals(1,store.bookmarks.value.count { Library.matches(it,"tai lieu 999") })
        store.close();store=BookmarkStore(context,name);store.load()
        assertEquals(1000,store.bookmarks.value.size)
        assertEquals(1000,store.bookmarks.value.map { it.slot }.toSet().size)
    }
    @Test fun offlineDeleteIsNotResurrectedByAnOldDeviceJournal() = runBlocking {
        val b=store.save("https://example.com/sync-delete","Keep", "private note", "Cá nhân")
        val old=SyncJson.decode(JSONObject(store.pending().first().second))
        store.delete(b.id)
        val deletion=SyncJson.decode(JSONObject(store.pending().last().second))
        val otherName="sync-test-${UUID.randomUUID()}.db"
        val other=BookmarkStore(context,otherName)
        try {
            other.load();other.mergeRemote("deletion",listOf(deletion));other.mergeRemote("late-old-phone",listOf(old))
            assertTrue(other.bookmarks.value.isEmpty())
            other.mergeRemote("intentional-restore",listOf(deletion.copy(deleted=false,revision=Revision(deletion.revision.counter+1,"web"))))
            assertEquals("private note",other.bookmarks.value.single().note)
            assertTrue(other.seen("intentional-restore"))
        } finally { other.close();context.deleteDatabase(otherName) }
    }
    @Test fun twoOfflineStoresMergeEditsAndIndependentHealthWithoutGeneratingEchoes() = runBlocking {
        val b=store.save("https://example.com/sync-edit","Original","note","Cá nhân")
        store.record(b,CheckResult(Health.REACHABLE,"ok",200),1234567)
        val otherName="sync-test-${UUID.randomUUID()}.db";val other=BookmarkStore(context,otherName)
        try {
            other.load();other.mergeRemote("phone",store.pending().map { SyncJson.decode(JSONObject(it.second)) })
            assertTrue(other.pending().isEmpty())
            val remote=other.bookmarks.value.single()
            other.save(remote.url,"Edited on web","updated note",remote.category,remote.id)
            store.mergeRemote("web",other.pending().map { SyncJson.decode(JSONObject(it.second)) })
            assertEquals("Edited on web",store.bookmarks.value.single().title)
            assertEquals("updated note",store.bookmarks.value.single().note)
            assertEquals(1234567L,store.bookmarks.value.single().checkedAt)
            val queued=store.pending();store.acknowledge(queued.map { it.first });assertTrue(store.pending().isEmpty())
        } finally { other.close();context.deleteDatabase(otherName) }
    }
    private lateinit var context: Context
    private lateinit var store: BookmarkStore
    private val name = "test-${UUID.randomUUID()}.db"
    @Before fun setup() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = BookmarkStore(context, name); store.load()
    }
    @After fun cleanup() { store.close(); context.deleteDatabase(name) }

    @Test fun savesAndReopensWithHealthAndDailyCache() = runBlocking {
        val b = store.save("https://example.com/path?key=1#part", "Example", "note", "Công việc")
        store.record(b, CheckResult(Health.REACHABLE, "test result", 200), 123456789)
        store.setMeta("completedDay", "2026-10-27")
        store.close()
        store = BookmarkStore(context, name); store.load()
        assertEquals(1, store.bookmarks.value.size)
        assertEquals(123456789L, store.bookmarks.value.single().checkedAt)
        assertEquals(Health.REACHABLE, store.bookmarks.value.single().health)
        assertEquals("https://example.com/path?key=1#part", store.bookmarks.value.single().url)
        assertEquals("2026-10-27", store.meta("completedDay"))
    }
    @Test fun staleCheckCannotOverwriteChangedUrlOrRecreateDeletedBookmark() = runBlocking {
        val old = store.save("https://example.com/old", "Old", "", "Cá nhân")
        val updated = store.save("https://example.com/new", "New", "", "Cá nhân", old.id)
        store.record(old, CheckResult(Health.UNAVAILABLE, "old check", 410), 123)
        assertEquals(Health.UNCHECKED, store.bookmarks.value.single().health)
        store.delete(updated.id)
        store.record(updated, CheckResult(Health.REACHABLE, "late check", 200), 456)
        assertTrue(store.bookmarks.value.isEmpty())
    }
    @Test fun importIsAtomicAndDoesNotOverwriteExistingBookmarks() = runBlocking {
        store.save("https://example.com/keep", "Keep", "", "Cá nhân")
        val invalid = """{"schemaVersion":1,"bookmarks":[{"url":"https://example.com/new"},{"url":"javascript:bad"}]}"""
        try { store.importJson(invalid); fail("Expected invalid URL rejection") } catch (_: IllegalArgumentException) { }
        assertEquals(1, store.bookmarks.value.size)
        val valid = """{"schemaVersion":1,"bookmarks":[{"url":"https://example.com/keep","title":"Overwrite"},{"url":"https://github.com/android/compose-samples","title":"Samples"}]}"""
        assertEquals(1, store.importJson(valid))
        assertEquals("Keep", store.bookmarks.value.first { it.url.endsWith("keep") }.title)
        assertEquals(0, store.importJson(valid))
    }
    @Test fun deleteDoesNotRenumberOtherBookmarksAndDuplicateSaveFails() = runBlocking {
        val a = store.save("https://github.com/a", "a", "", "Cá nhân")
        val b = store.save("https://github.com/b", "b", "", "Cá nhân")
        store.delete(a.id)
        assertEquals(b.slot, store.bookmarks.value.single().slot)
        try { store.save(b.url, "duplicate", "", "Cá nhân"); fail("Expected duplicate rejection") } catch (_: IllegalArgumentException) { }
        assertEquals(1, store.bookmarks.value.size)
    }
    @Test fun quickEntryAssignsIconsAndGlobalSlotsWithoutExtraFields() = runBlocking {
        val a = store.save("github.com/android/compose-samples", "", "", "Cá nhân")
        val b = store.save("www.tiktok.com/@linkmap", "", "", "Cá nhân")
        assertEquals(Provider.GITHUB, a.provider)
        assertEquals(Provider.TIKTOK, b.provider)
        assertNotEquals(a.slot, b.slot)
        assertEquals("github.com · compose-samples", a.title)
        val edited = store.save("https://facebook.com/android", a.title, "", "Cá nhân", a.id)
        assertEquals(a.slot, edited.slot)
        assertEquals(Provider.FACEBOOK, edited.provider)
    }
    @Test fun versionOneUpgradePreservesDataAndConvertsOverlappingPlatformSlots() = runBlocking {
        store.close(); context.deleteDatabase(name)
        val db = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        db.execSQL("""CREATE TABLE bookmarks (id TEXT PRIMARY KEY, url TEXT NOT NULL UNIQUE, title TEXT NOT NULL,
            note TEXT NOT NULL, category TEXT NOT NULL, provider TEXT NOT NULL, slot INTEGER NOT NULL,
            health TEXT NOT NULL, reason TEXT NOT NULL, checkedAt INTEGER NOT NULL, httpCode INTEGER,
            notFoundAt INTEGER NOT NULL, createdAt INTEGER NOT NULL)""")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("INSERT INTO bookmarks VALUES ('a','https://facebook.com/a','FB','original note','Work','FACEBOOK',0,'REACHABLE','ok',123,200,0,1)")
        db.execSQL("INSERT INTO bookmarks VALUES ('b','https://github.com/b','GH','','Work','GITHUB',0,'UNCERTAIN','private',456,404,0,2)")
        db.execSQL("INSERT INTO meta VALUES ('completedDay','2026-10-27')")
        db.version = 1; db.close()
        store = BookmarkStore(context, name); store.load()
        val items = store.bookmarks.value
        assertEquals(listOf(0, 1), items.map { it.slot })
        assertEquals("original note", items.first().note)
        assertEquals(123L, items.first().checkedAt)
        assertEquals(Health.REACHABLE, items.first().health)
        assertEquals(456L, items.last().checkedAt)
        assertEquals("2026-10-27", store.meta("completedDay"))
        assertEquals(2, store.save("https://example.com", "", "", "Cá nhân").slot)
    }
}
