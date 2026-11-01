package vn.linkmap

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import vn.linkmap.core.*
import vn.linkmap.data.*
import java.util.UUID

class NoteStoreTest {
    private val context:Context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="notes-test-${UUID.randomUUID()}.db"
    private lateinit var store:BookmarkStore
    @Before fun setup()=runBlocking {store=BookmarkStore(context,name);store.load()}
    @After fun cleanup(){store.close();context.deleteDatabase(name)}
    @Test fun multipleNotesPersistEditRestoreAndRoundtripBackupWithoutUrlCollisions()=runBlocking {
        val a=store.saveNote("Ý tưởng\nNội dung tiếng Việt","","Công việc")
        val b=store.saveNote("Ý tưởng\nNội dung tiếng Việt")
        assertNotEquals(a.id,b.id);assertTrue(a.isNote);assertEquals("",a.url)
        assertEquals(2,store.bookmarks.value.size)
        val updated=store.saveNote("Có URL https://github.com/android nhưng vẫn là note","Tên mới",a.category,a.id)
        assertEquals(a.id,updated.id);assertTrue(updated.isNote);assertEquals(a.slot,updated.slot)
        store.setReading(a.id){it.copy(pinned=true)}
        store.close();store=BookmarkStore(context,name);store.load()
        store.delete(a.id);store.restore(a.id)
        assertEquals(updated.note,store.bookmarks.value.first {it.id==a.id}.note)
        val backup=store.exportJson();assertEquals(2,JSONObject(backup).getInt("schemaVersion"))
        val otherName="note-import-${UUID.randomUUID()}.db";val other=BookmarkStore(context,otherName)
        try {other.load();assertEquals(2,other.importJson(backup));assertEquals(0,other.importJson(backup));assertTrue(other.bookmarks.value.all {it.isNote});assertTrue(other.reading.value.getValue(a.id).pinned);assertEquals(a.createdAt,other.bookmarks.value.first {it.id==a.id}.createdAt)} finally {other.close();context.deleteDatabase(otherName)}
        Unit
    }
    @Test fun longNotesSyncAcrossStoresAndDeletionDoesNotResurrect()=runBlocking {
        val body="Đ".repeat(64000)
        val note=store.saveNote(body)
        val old=SyncJson.decode(JSONObject(store.pending().single().second))
        assertEquals(body,old.bookmark.note)
        val otherName="note-sync-${UUID.randomUUID()}.db";val other=BookmarkStore(context,otherName)
        try {
            other.load();other.mergeRemote("first",listOf(old))
            assertEquals(body,other.bookmarks.value.single().note);assertTrue(other.pending().isEmpty())
            other.saveNote("Sửa ở tablet","",id=note.id)
            store.mergeRemote("tablet",other.pending().map {SyncJson.decode(JSONObject(it.second))})
            assertEquals("Sửa ở tablet",store.bookmarks.value.single().note)
            store.delete(note.id);val deletion=SyncJson.decode(JSONObject(store.pending().last().second))
            other.mergeRemote("delete",listOf(deletion));other.mergeRemote("late",listOf(old))
            assertTrue(other.bookmarks.value.isEmpty())
        } finally {other.close();context.deleteDatabase(otherName)}
    }
    @Test fun notesAreNeverMutatedByAHttpCheckAndLegacyBackupsStillImport()=runBlocking {
        val note=store.saveNote("Không phải URL")
        store.record(note,CheckResult(Health.UNAVAILABLE,"must not apply",410),1234)
        assertEquals(0L,store.bookmarks.value.single().checkedAt)
        assertEquals(Health.UNCHECKED,LinkChecker().check(note).health)
        assertEquals(1,store.importJson("""{"schemaVersion":1,"bookmarks":[{"url":"https://example.com/legacy","note":"original"}]}"""))
        assertEquals(1,store.bookmarks.value.count { !it.isNote })
    }
    @Test fun versionFourMigrationRetainsEveryBookmarkAndAllowsNullUrlsForNotes()=runBlocking {
        val b=store.save("https://example.com/v4","Keep","Original note","Work")
        val db=store.writableDatabase
        db.execSQL("ALTER TABLE bookmarks RENAME TO fixture_version5")
        db.execSQL("""CREATE TABLE bookmarks (id TEXT PRIMARY KEY,url TEXT NOT NULL UNIQUE,title TEXT NOT NULL,note TEXT NOT NULL,category TEXT NOT NULL,provider TEXT NOT NULL,slot INTEGER NOT NULL,health TEXT NOT NULL,reason TEXT NOT NULL,checkedAt INTEGER NOT NULL,httpCode INTEGER,notFoundAt INTEGER NOT NULL,createdAt INTEGER NOT NULL)""")
        db.execSQL("INSERT INTO bookmarks SELECT id,url,title,note,category,provider,slot,health,reason,checkedAt,httpCode,notFoundAt,createdAt FROM fixture_version5")
        db.version=4;store.close();store=BookmarkStore(context,name);store.load()
        assertEquals(b,store.bookmarks.value.single())
        store.saveNote("Note thứ nhất");store.saveNote("Note thứ hai")
        assertEquals(3,store.bookmarks.value.size)
        assertEquals(1,store.readableDatabase.rawQuery("SELECT COUNT(*) FROM bookmarks_before_notes_v4",null).use {it.moveToFirst();it.getInt(0)})
    }
}
