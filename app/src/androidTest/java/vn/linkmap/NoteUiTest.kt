package vn.linkmap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import vn.linkmap.core.*
import java.util.UUID

class NoteUiTest {
    @Test fun typedNoteEditsAndClipboardMixedTextKeepTheirCorrectKinds()=runBlocking {
        val ins=InstrumentationRegistry.getInstrumentation()
        val app=ins.targetContext.applicationContext as LinkMapApplication
        val subject="note-ui-${UUID.randomUUID()}"
        val database="linkmap-${SyncRules.id(subject).takeLast(32)}.db"
        var activity:android.app.Activity?=null
        fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap { nodes(n.getChild(it)) }
        fun find(text:String):AccessibilityNodeInfo? {ins.uiAutomation.clearCache();return nodes(ins.uiAutomation.rootInActiveWindow).firstOrNull {it.text?.toString()==text || it.contentDescription?.toString()==text}}
        fun waitFor(text:String):AccessibilityNodeInfo {
            repeat(40) {find(text)?.let {return it};SystemClock.sleep(100)}
            throw AssertionError("Missing $text; visible="+nodes(ins.uiAutomation.rootInActiveWindow).mapNotNull {it.text?.toString()})
        }
        fun click(text:String) {var node:AccessibilityNodeInfo?=waitFor(text);while(node!=null && !node.isClickable) node=node.parent;assertTrue(node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK));SystemClock.sleep(250)}
        fun type(text:String) {
            ins.uiAutomation.clearCache()
            val input=nodes(ins.uiAutomation.rootInActiveWindow).first {it.className=="android.widget.EditText"}
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply {putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text)}));SystemClock.sleep(200)
        }
        try {
            withContext(Dispatchers.Main) {app.openAccount(subject)}
            activity=ins.startActivitySync(Intent(app,MainActivity::class.java).setAction("vn.linkmap.ADD").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("Nhập hoặc dán link / ghi chú")
            SystemClock.sleep(700)
            ins.uiAutomation.takeScreenshot()?.let {b ->java.io.File(app.getExternalFilesDir(null),"35-cosmic-modal-v061.png").outputStream().use {b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
            val body="Ý tưởng LinkMap\nLưu những điều cần nhớ vào thiên hà."
            type(body);click("Design")
            ins.uiAutomation.takeScreenshot()?.let {b ->java.io.File(app.getExternalFilesDir(null),"36-cosmic-note-v061.png").outputStream().use {b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
            click("Lưu ghi chú")
            waitFor("Chỉnh sửa ghi chú")
            assertEquals(body,app.store.bookmarks.value.single().note)
            assertTrue(app.store.bookmarks.value.single().isNote)
            assertEquals("Design",app.store.bookmarks.value.single().category)
            ins.uiAutomation.takeScreenshot()?.let {b ->java.io.File(app.getExternalFilesDir(null),"33-note-detail-v060.png").outputStream().use {b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
            click("Chỉnh sửa ghi chú")
            val edited="$body\nTham khảo http://github.com/android khi cần."
            type(edited);click("Lưu thay đổi");waitFor("Chỉnh sửa ghi chú")
            assertTrue(app.store.bookmarks.value.single().isNote);assertEquals(edited,app.store.bookmarks.value.single().note)
            ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);SystemClock.sleep(200)
            click("Thêm vào LinkMap")
            val mixed="Tài liệu cần đọc http://github.com/android/compose-samples\nĐọc trước thứ Sáu."
            withContext(Dispatchers.Main) {(app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("test",mixed))}
            click("Dán từ bộ nhớ tạm")
            assertEquals(1,app.store.bookmarks.value.size)
            click("Thêm vào bản đồ")
            waitFor("Mở liên kết")
            val link=app.store.bookmarks.value.single { !it.isNote }
            assertEquals(Provider.GITHUB,link.provider);assertEquals(mixed,link.note)
            assertEquals(0L,app.store.bookmarks.value.first {it.isNote}.checkedAt)
            ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);SystemClock.sleep(200)
            click("Thư viện")
            // Let Android's clipboard overlay and the save snackbar disappear before visual QA.
            SystemClock.sleep(9000)
            ins.uiAutomation.takeScreenshot()?.let {b ->java.io.File(app.getExternalFilesDir(null),"34-links-and-notes-v060.png").outputStream().use {b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
            withContext(Dispatchers.Main) {activity?.finish()};SystemClock.sleep(200)
            activity=ins.startActivitySync(Intent(app,CaptureActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Việc cần làm\nMua đồ dùng hôm nay").putExtra(Intent.EXTRA_SUBJECT,"Nhắc việc").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("✓ Đã lưu vào LinkMap")
            assertEquals(2,app.store.bookmarks.value.count {it.isNote})
            assertEquals("Việc cần làm\nMua đồ dùng hôm nay",app.store.bookmarks.value.first {it.title=="Nhắc việc"}.note)
            withContext(Dispatchers.Main) {activity?.finish()};SystemClock.sleep(200)
            val multiple="Xem http://github.com/android/compose-samples và http://drive.google.com/file/d/example?resourcekey=keep#view\nGiữ nguyên cả đoạn này."
            val chosen="http://drive.google.com/file/d/example?resourcekey=keep#view"
            activity=ins.startActivitySync(Intent(app,CaptureActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,multiple).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            click(chosen);waitFor("✓ Đã lưu vào LinkMap")
            val drive=app.store.bookmarks.value.first {it.url==chosen}
            assertEquals(Provider.DRIVE,drive.provider);assertEquals(multiple,drive.note)
        } finally {
            withContext(Dispatchers.Main) {activity?.finish()};SystemClock.sleep(200)
            val fixture=app.store
            withContext(Dispatchers.Main) {app.openAccount(null)}
            fixture.close();app.deleteDatabase(database)
        }
    }
}
