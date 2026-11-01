package vn.linkmap

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import vn.linkmap.data.BookmarkStore
import vn.linkmap.data.DriveJournal
import java.util.UUID

/** Explicitly opted-in live test. No credentials or user content are written to test output. */
class GoogleDriveLiveTest {
    @Test fun realDriveRoundtripAcrossIndependentStores()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveGoogle")=="true")
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LinkMapApplication
        val tokenField=GoogleSyncController::class.java.getDeclaredField("token").apply {isAccessible=true}
        repeat(100) {if(!app.ready.value || !app.sync.state.value.driveEnabled || tokenField.get(app.sync)==null)delay(300)}
        val subject=requireNotNull(app.sync.state.value.profile).subject
        // Access is confined to the instrumentation process; the token never leaves memory or enters logs/files.
        val access=requireNotNull(tokenField.get(app.sync) as? String) {"Connect Drive in the app before running the live test"}
        val tag=UUID.randomUUID().toString()
        val firstName="live-probe-a-$tag.db";val secondName="live-probe-b-$tag.db"
        val first=BookmarkStore(app,firstName);val second=BookmarkStore(app,secondName)
        val drive=DriveJournal()
        val owned=mutableListOf<String>()
        try {
            first.load();second.load()
            val note=first.saveNote("LinkMap Galaxy live sync probe\n$tag","Kiểm thử đồng bộ","Kiểm thử")
            val link=first.save("https://example.com/linkmap-test/$tag","Link kiểm thử","Nội dung đi kèm","Kiểm thử")
            owned+=note.id;owned+=link.id
            drive.sync(access,subject,first)
            assertTrue(first.pending().isEmpty())
            drive.sync(access,subject,second)
            assertEquals(note.note,second.bookmarks.value.single {it.id==note.id}.note)
            assertEquals(link.url,second.bookmarks.value.single {it.id==link.id}.url)
            second.saveNote("${note.note}\nEdited by second store",note.title,note.category,note.id)
            drive.sync(access,subject,second);drive.sync(access,subject,first)
            assertTrue(first.bookmarks.value.single {it.id==note.id}.note.endsWith("Edited by second store"))
            first.delete(note.id);first.delete(link.id)
            drive.sync(access,subject,first);drive.sync(access,subject,second)
            assertFalse(second.bookmarks.value.any {it.id in owned})
            second.close()
            val reloaded=BookmarkStore(app,secondName)
            try {reloaded.load();assertFalse(reloaded.bookmarks.value.any {it.id in owned})}
            finally {reloaded.close()}
        } finally {
            // Only tombstone this test's own UUID records; no cloud file or pre-existing item is deleted.
            runCatching {owned.forEach {id -> first.delete(id)};drive.sync(access,subject,first)}
            first.close();second.close();app.deleteDatabase(firstName);app.deleteDatabase(secondName)
        }
    }
}
