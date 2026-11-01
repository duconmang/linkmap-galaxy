package vn.linkmap

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import vn.linkmap.data.LinkChecker

class PreviewTest {
    @Test fun previewDecodingBoundsMemoryAndRejectsInvalidContent() {
        val source=Bitmap.createBitmap(1000,600,Bitmap.Config.ARGB_8888)
        val output=java.io.ByteArrayOutputStream()
        source.compress(Bitmap.CompressFormat.PNG,100,output);source.recycle()
        val checker=LinkChecker()
        val decoded=checker.decodePreview(output.toByteArray())
        assertTrue(decoded.width<=512 && decoded.height<=512);decoded.recycle()
        try { checker.decodePreview("not an image".toByteArray());fail("Reject non-image") } catch(_:IllegalArgumentException) {}
        try { checker.decodePreview(ByteArray(1024*1024+1));fail("Reject oversize") } catch(_:IllegalArgumentException) {}
    }
    @Test fun previewsNeverProbeCleartextOrLoopbackTargets() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val checker=LinkChecker()
        assertNull(checker.cachePreview("http://example.com/picture.jpg",context.cacheDir))
        assertNull(checker.cachePreview("https://127.0.0.1/picture.jpg",context.cacheDir))
        assertNull(checker.cachePreview("https://[::1]/picture.jpg",context.cacheDir))
    }
}
