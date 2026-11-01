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
import vn.linkmap.core.*
import vn.linkmap.data.DemoLibrary
import java.util.UUID

class GalaxyDemoTest {
    @Test fun bundled100DemoLinksTwinkleAndNeverEnterTheRealLibraryOrOutbox() = runBlocking {
        val ins=InstrumentationRegistry.getInstrumentation()
        val app=ins.targetContext.applicationContext as LinkMapApplication
        val subject="galaxy-demo-test-${UUID.randomUUID()}"
        val database="linkmap-${SyncRules.id(subject).takeLast(32)}.db"
        var activity:android.app.Activity?=null
        fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap { nodes(n.getChild(it)) }
        fun waitFor(text:String):AccessibilityNodeInfo {
            repeat(40) {
                ins.uiAutomation.clearCache()
                nodes(ins.uiAutomation.rootInActiveWindow).firstOrNull { it.text?.toString()==text || it.contentDescription?.toString()==text }?.let { return it }
                SystemClock.sleep(100)
            }
            ins.uiAutomation.takeScreenshot()?.let { b -> java.io.File(app.getExternalFilesDir(null),"galaxy-ui-failure.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };b.recycle() }
            throw AssertionError("Missing $text; visible="+nodes(ins.uiAutomation.rootInActiveWindow).mapNotNull { it.text?.toString() })
        }
        fun click(text:String) {
            var n:AccessibilityNodeInfo?=waitFor(text)
            while(n!=null && !n.isClickable) n=n.parent
            assertTrue(n!!.performAction(AccessibilityNodeInfo.ACTION_CLICK));SystemClock.sleep(250);ins.uiAutomation.clearCache()
        }
        fun save(name:String,b:android.graphics.Bitmap) {
            val suffix=InstrumentationRegistry.getArguments().getString("screenshotSuffix","")
            java.io.File(app.getExternalFilesDir(null),name.replace(".png","$suffix.png")).outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        }
        try {
            withContext(Dispatchers.Main) { app.openAccount(subject) }
            val examples=DemoLibrary.load(app)
            assertEquals(100,examples.size);assertEquals(100,examples.map { it.url }.toSet().size)
            assertTrue(examples.all { Urls.normalize(it.url)!=null })
            assertTrue(examples.map { it.createdAt }.toSet().size>80)
            activity=ins.startActivitySync(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("Thiên hà (100 sao)")
            SystemClock.sleep(800)
            val recording=if(InstrumentationRegistry.getArguments().getString("captureVideo")=="true")
                ins.uiAutomation.executeShellCommand("screenrecord --size 720x1600 --bit-rate 2000000 --time-limit 12 /sdcard/Android/data/vn.linkmap/files/galaxy-v041.mp4") else null
            val first=requireNotNull(ins.uiAutomation.takeScreenshot());save("23-galaxy-v040.png",first)
            assertNotEquals("Emulator must render the scene before checking animation",android.graphics.Color.BLACK,first.getPixel(first.width/2,first.height/2))
            val zoomBounds=android.graphics.Rect()
            waitFor("Phóng to").getBoundsInScreen(zoomBounds)
            assertTrue("Zoom controls belong in the lower right",zoomBounds.centerY()>first.height/2 && zoomBounds.centerX()>first.width*.75f)
            SystemClock.sleep(2000)
            val second=requireNotNull(ins.uiAutomation.takeScreenshot());save("24-galaxy-twinkle-v040.png",second)
            var changed=0
            var brightChanges=0
            for(y in first.height/4 until first.height*9/10 step 8) for(x in 0 until first.width step 8) {
                val a=first.getPixel(x,y);val b=second.getPixel(x,y)
                if(a!=b) changed++
                val delta=kotlin.math.abs(android.graphics.Color.red(a)-android.graphics.Color.red(b))+kotlin.math.abs(android.graphics.Color.green(a)-android.graphics.Color.green(b))+kotlin.math.abs(android.graphics.Color.blue(a)-android.graphics.Color.blue(b))
                if(delta>90) brightChanges++
            }
            first.recycle();second.recycle()
            assertTrue("Animated star borders must change pixels ($changed)",changed>5)
            assertTrue("Twinkle must create a visible brightness change ($brightChanges)",brightChanges>35)
            if(recording!=null) { SystemClock.sleep(10500);recording.close() }
            assertTrue(app.store.bookmarks.value.isEmpty());assertTrue(app.store.pending().isEmpty())
            click("Xem toàn bản đồ")
            val overview=requireNotNull(ins.uiAutomation.takeScreenshot());save("25-galaxy-100-overview-v040.png",overview);overview.recycle()
            click("Thư viện")
            click("Compose Samples")
            click("Ghim link")
            assertTrue(app.store.reading.value.isEmpty())
            assertTrue(app.store.pending().isEmpty())
            click("Xem trên bản đồ")
            waitFor("Về vị trí trước")
            val network=requireNotNull(ins.uiAutomation.takeScreenshot());save("platform-network.png",network);network.recycle()
            click("Về tâm thiên hà")
            click("Demo · 100 sao • Về thư viện")
            waitFor("Thiên hà của bạn bắt đầu từ một link hoặc note")
            assertTrue(app.store.bookmarks.value.isEmpty());assertTrue(app.store.pending().isEmpty())
            click("Xem 100 link demo")
            waitFor("Thiên hà (100 sao)")
            // Reopening with reduced motion disables all animated map frames.
            withContext(Dispatchers.Main) { activity?.finish() };SystemClock.sleep(200)
            app.store.setMeta("twinkle","false")
            activity=ins.startActivitySync(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("Thiên hà (100 sao)");SystemClock.sleep(600)
            val stillA=requireNotNull(ins.uiAutomation.takeScreenshot());SystemClock.sleep(1000)
            val stillB=requireNotNull(ins.uiAutomation.takeScreenshot())
            var differences=0
            for(y in stillA.height/3 until stillA.height*4/5 step 12) for(x in 0 until stillA.width step 12) if(stillA.getPixel(x,y)!=stillB.getPixel(x,y)) differences++
            stillA.recycle();stillB.recycle()
            assertEquals("Reduced-motion map must remain still",0,differences)
            click("Link cần chú ý")
            assertTrue(nodes(ins.uiAutomation.rootInActiveWindow).any { it.text?.toString()?.let { text -> "Cần xác minh" in text || "Không truy cập được" in text }==true })
            click("Hồ sơ người dùng")
            waitFor("Không gian của bạn")
            Unit
        } finally {
            withContext(Dispatchers.Main) { activity?.finish() };SystemClock.sleep(200)
            val fixture=app.store
            withContext(Dispatchers.Main) { app.openAccount(null) }
            fixture.close();app.deleteDatabase(database)
        }
    }
}
