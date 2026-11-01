package vn.linkmap

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import vn.linkmap.core.SyncRules
import java.util.UUID

class CaptureFlowTest {
    @Test fun shareSavesWithoutFormAndProcessTextDuplicateOpensExistingItem() = runBlocking {
        val ins=InstrumentationRegistry.getInstrumentation()
        val app=ins.targetContext.applicationContext as LinkMapApplication
        val subject="capture-fixture-${UUID.randomUUID()}"
        val database="linkmap-${SyncRules.id(subject).takeLast(32)}.db"
        var activity:android.app.Activity?=null
        fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap { nodes(n.getChild(it)) }
        fun waitFor(label:String):AccessibilityNodeInfo {
            repeat(30) { nodes(ins.uiAutomation.rootInActiveWindow).firstOrNull { it.text?.toString()==label }?.let { return it };SystemClock.sleep(150) }
            throw AssertionError("Missing $label")
        }
        try {
            withContext(Dispatchers.Main) { app.openAccount(subject) }
            activity=ins.startActivitySync(Intent(app,CaptureActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"http://example.com/linkmap-capture-fixture").putExtra(Intent.EXTRA_SUBJECT,"Tài liệu được chia sẻ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("✓ Đã lưu vào LinkMap")
            assertEquals("Tài liệu được chia sẻ",app.store.bookmarks.value.single().title)
            val bitmap=ins.uiAutomation.takeScreenshot()
            bitmap?.let { b -> java.io.File(app.getExternalFilesDir(null),"18-android-share-v030.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };b.recycle() }
            withContext(Dispatchers.Main) { activity?.finish() };SystemClock.sleep(200)
            activity=ins.startActivitySync(Intent(app,CaptureActivity::class.java).setAction(Intent.ACTION_PROCESS_TEXT).setType("text/plain").putExtra(Intent.EXTRA_PROCESS_TEXT,"http://example.com/linkmap-capture-fixture").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("Bạn đã lưu link này")
            assertEquals(1,app.store.bookmarks.value.size)
            assertEquals("Tài liệu được chia sẻ",app.store.bookmarks.value.single().title)
            val widget=android.appwidget.AppWidgetManager.getInstance(app).installedProviders.firstOrNull { it.provider.className==LinkMapWidget::class.java.name }
            assertNotNull("Quick-add widget registered",widget)
        } finally {
            withContext(Dispatchers.Main) { activity?.finish() };SystemClock.sleep(200)
            val fixture=app.store
            withContext(Dispatchers.Main) { app.openAccount(null) }
            fixture.close();app.deleteDatabase(database)
        }
    }
}
