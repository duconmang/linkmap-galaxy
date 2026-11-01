package vn.linkmap

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

/** UI fixture uses its own account database, never the user's guest library. */
class LibraryUiTest {
    @Test fun mapSearchPinAndReturnToMapWith150SyntheticLinks() = exerciseLibrary(150)
    @Test fun mapSearchPinAndReturnToMapWith1000SyntheticLinks() = exerciseLibrary(1000)
    private fun exerciseLibrary(count:Int) = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as LinkMapApplication
        val subject="ui-fixture-${UUID.randomUUID()}"
        val database="linkmap-${SyncRules.id(subject).takeLast(32)}.db"
        var activity: android.app.Activity?=null
        try {
            withContext(Dispatchers.Main) { app.openAccount(subject) }
            val now=System.currentTimeMillis()
            val providers=listOf("https://github.com/android/","https://drive.google.com/file/d/","https://www.facebook.com/","https://www.tiktok.com/@","https://onedrive.live.com/?id=","https://www.google.com/search?q=")
            val records=(0 until count).map { i ->
                val url=providers[i%6]+"linkmap-fixture-$i"
                SyncRecord(Bookmark(SyncRules.id(url),url,if(i==0) "Đồ án Android Compose" else "Tài liệu tham khảo $i",note="Dữ liệu kiểm thử tổng hợp; trạng thái minh họa",category=listOf("Học Android","Công việc","Du lịch")[i%3],health=if(i%9==0) Health.UNCERTAIN else Health.REACHABLE,reason="Trạng thái minh họa của bộ kiểm thử",checkedAt=now,createdAt=now+i),Revision(i+1L,"ui-fixture"),healthDevice="ui-fixture")
            }
            app.store.mergeRemote("fixture",records)
            activity=instrumentation.startActivitySync(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            fun nodes(node:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(node==null) emptyList() else listOf(node)+(0 until node.childCount).flatMap { nodes(node.getChild(it)) }
            fun find(label:String):AccessibilityNodeInfo? = nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull { it.text?.toString()==label || it.contentDescription?.toString()==label }
            fun waitFor(label:String):AccessibilityNodeInfo {
                repeat(30) { find(label)?.let { return it };SystemClock.sleep(150) }
                throw AssertionError("Missing UI label: $label; visible="+nodes(instrumentation.uiAutomation.rootInActiveWindow).mapNotNull { it.text?.toString() })
            }
            fun click(label:String) {
                var node:AccessibilityNodeInfo?=waitFor(label)
                while(node!=null && !node.isClickable) node=node.parent
                assertNotNull("Clickable $label",node);assertTrue(node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK));SystemClock.sleep(250)
            }
            fun screenshot(name:String) {
                // An intentionally animated Canvas need not make the main queue idle.
                SystemClock.sleep(350)
                val bitmap=instrumentation.uiAutomation.takeScreenshot()
                val suffix=InstrumentationRegistry.getArguments().getString("screenshotSuffix", "")
                val filename=(if(count==150) name else name.replace("v030","1000-v030")).replace(".png","$suffix.png")
                requireNotNull(bitmap).let { b -> java.io.File(app.getExternalFilesDir(null),filename).outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };b.recycle() }
            }
            waitFor("Tìm vì sao & liên kết tri thức…")
            screenshot("13-android-map-v030.png")
            click("Xem toàn bản đồ")
            screenshot("14-android-overview-150-v030.png")
            click("Thư viện")
            val input=nodes(instrumentation.uiAutomation.rootInActiveWindow).first { it.className=="android.widget.EditText" }
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"do an android") }))
            waitFor("Đồ án Android Compose")
            screenshot("15-android-search-v030.png")
            click("Đồ án Android Compose")
            click("Ghim link")
            waitFor("Đã ghim")
            assertTrue(app.store.reading.value.getValue(records.first().bookmark.id).pinned)
            screenshot("16-android-detail-v030.png")
            click("Xem trên bản đồ")
            waitFor("Về vị trí trước")
            screenshot("17-android-focused-v030.png")
            assertEquals(count,app.store.bookmarks.value.size)
        } finally {
            withContext(Dispatchers.Main) { activity?.finish() }
            SystemClock.sleep(250)
            val fixture=app.store
            withContext(Dispatchers.Main) { app.openAccount(null) }
            fixture.close();app.deleteDatabase(database)
        }
    }
}
