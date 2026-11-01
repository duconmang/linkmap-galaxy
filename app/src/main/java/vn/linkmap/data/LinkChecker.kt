package vn.linkmap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import vn.linkmap.core.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class LinkChecker {
    // Never probe a user's LAN, loopback or metadata endpoints through a shared URL.
    private val publicDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.any { !isPublic(it) }) throw UnknownHostException("Địa chỉ nội bộ không được kiểm tra tự động")
            return addresses
        }
    }
    private val client = OkHttpClient.Builder().dns(publicDns)
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()

    suspend fun check(bookmark: Bookmark): CheckResult = withContext(Dispatchers.IO) {
        if(bookmark.isNote) return@withContext CheckResult(Health.UNCHECKED,"Ghi chú không cần kiểm tra HTTP")
        try {
            var url = bookmark.url.toHttpUrl()
            if (!url.isHttps) return@withContext CheckResult(Health.UNCERTAIN, "Link HTTP: hãy dùng HTTPS để kiểm tra an toàn")
            var loginRedirect = false
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(18)
            for (hop in 0..5) {
                if (System.nanoTime() >= deadline) return@withContext CheckResult(Health.UNCERTAIN, "Hết thời gian kiểm tra")
                // GET streaming also detects login/error HTML returned with status 200.
                // Only peek a bounded prefix, never download entire files or execute web scripts.
                val request = Request.Builder().url(url).header("User-Agent", "LinkMap/0.1 (bookmark availability check)")
                    .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8").get().build()
                client.newCall(request).execute().use { response ->
                    if (response.code in setOf(301, 302, 303, 307, 308)) {
                        val target = response.header("Location")?.let { url.resolve(it) }
                            ?: return@withContext CheckResult(Health.UNCERTAIN, "Chuyển hướng không hợp lệ", response.code)
                        if (!target.isHttps || target.username.isNotEmpty() || target.password.isNotEmpty())
                            return@withContext CheckResult(Health.UNCERTAIN, "Chuyển hướng không an toàn", response.code)
                        val path = target.encodedPath.lowercase()
                        loginRedirect = loginRedirect || listOf("login", "signin", "sign-in", "checkpoint", "authwall").any { it in path }
                        url = target
                    } else {
                        val html = if (response.header("Content-Type").orEmpty().contains("text/html"))
                            response.peekBody(16 * 1024).string() else ""
                        val login = loginRedirect || HtmlSignals.needsVerification(html)
                        val result=HealthRules.classify(response.code, bookmark.provider, login, bookmark.notFoundAt, System.currentTimeMillis())
                        return@withContext if(result.health==Health.REACHABLE) result.copy(title=Library.pageTitle(html), previewUrl=Library.previewUrl(html)?.let { url.resolve(it)?.toString() }) else result
                    }
                }
            }
            CheckResult(Health.UNCERTAIN, "Quá nhiều lần chuyển hướng")
        } catch (_: UnknownHostException) {
            CheckResult(Health.UNCERTAIN, "Không phân giải được tên miền hoặc địa chỉ không công khai")
        } catch (_: java.io.IOException) {
            CheckResult(Health.UNCERTAIN, "Lỗi mạng, TLS hoặc hết thời gian; chưa kết luận link chết")
        } catch (_: IllegalArgumentException) {
            CheckResult(Health.UNCERTAIN, "URL không hợp lệ")
        }
    }
    suspend fun cachePreview(url: String, directory: java.io.File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target=url.toHttpUrl(); require(target.isHttps && target.username.isEmpty() && target.password.isEmpty())
            val cached=java.io.File(directory,SyncRules.id(url)+".jpg")
            if(cached.isFile && System.currentTimeMillis()-cached.lastModified()<7*86400000L) return@runCatching cached.absolutePath
            client.newCall(Request.Builder().url(target).build()).execute().use { response ->
                require(response.isSuccessful && response.header("Content-Type").orEmpty().startsWith("image/"))
                val body=requireNotNull(response.body)
                require(body.contentLength() <= 1024*1024)
                val output=java.io.ByteArrayOutputStream()
                body.byteStream().use { input -> val buffer=ByteArray(8192); while(output.size()<=1024*1024) { val count=input.read(buffer); if(count<0) break; output.write(buffer,0,count) } }
                val bytes=output.toByteArray(); require(bytes.size<=1024*1024)
                val bitmap=decodePreview(bytes)
                directory.mkdirs()
                var cacheSize=directory.listFiles()?.sumOf { it.length() } ?: 0L
                directory.listFiles()?.sortedBy { it.lastModified() }?.forEach { old ->
                    if(cacheSize>40*1024*1024) { val size=old.length(); if(old.delete()) cacheSize-=size }
                }
                val file=java.io.File(directory,SyncRules.id(url)+".jpg")
                val temporary=java.io.File.createTempFile("preview-", ".tmp",directory)
                try {
                    temporary.outputStream().use { require(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,80,it)) }
                    java.nio.file.Files.move(temporary.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    file.absolutePath
                } finally { bitmap.recycle();temporary.delete() }
            }
        }.getOrNull()
    }
    internal fun decodePreview(bytes: ByteArray): android.graphics.Bitmap {
        require(bytes.size<=1024*1024)
        val bounds=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
        android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        require(bounds.outWidth in 1..8192 && bounds.outHeight in 1..8192)
        val options=android.graphics.BitmapFactory.Options().apply { inSampleSize=1;while(maxOf(bounds.outWidth,bounds.outHeight)/inSampleSize>512) inSampleSize*=2 }
        return requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,options))
    }
    private fun isPublic(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address
        if (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc) return false
        if (bytes.size == 4) {
            val a = bytes[0].toInt() and 255; val b = bytes[1].toInt() and 255
            if (a == 0 || a >= 224 || (a == 100 && b in 64..127) || (a == 198 && b in 18..19)) return false
        }
        return true
    }
}
