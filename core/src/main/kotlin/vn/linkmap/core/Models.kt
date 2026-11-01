package vn.linkmap.core

import java.net.URI
import java.time.Instant
import java.time.ZoneId

enum class Provider(val label: String, val domains: List<String>) {
    FACEBOOK("Facebook", listOf("facebook.com", "fb.com", "fb.watch")),
    TIKTOK("TikTok", listOf("tiktok.com")),
    GITHUB("GitHub", listOf("github.com", "github.io")),
    DRIVE("Google Drive", listOf("drive.google.com", "docs.google.com")),
    ONEDRIVE("OneDrive", listOf("onedrive.live.com", "1drv.ms", "sharepoint.com")),
    GOOGLE("Google", listOf("google.com")), OTHER("Khác", emptyList());
    companion object {
        fun forUrl(url: String): Provider {
            val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
            return entries.firstOrNull { p -> p.domains.any { host == it || host.endsWith(".$it") } } ?: OTHER
        }
    }
}
enum class Health(val label: String) {
    UNCHECKED("Chưa kiểm tra"), REACHABLE("Truy cập được"),
    UNAVAILABLE("Không truy cập được"), UNCERTAIN("Cần xác minh")
}
enum class EntryKind { LINK, NOTE }
data class Bookmark(
    val id: String, val url: String, val title: String,
    val note: String = "", val category: String = "Cá nhân",
    val provider: Provider = Provider.forUrl(url), val slot: Int = 0,
    val health: Health = Health.UNCHECKED, val reason: String = "Chưa có kết quả",
    val checkedAt: Long = 0, val httpCode: Int? = null, val notFoundAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(), val kind: EntryKind = EntryKind.LINK
) { val isNote: Boolean get() = kind == EntryKind.NOTE }
data class CheckResult(val health: Health, val reason: String, val code: Int? = null, val notFoundAt: Long = 0, val title: String? = null, val previewUrl: String? = null)
object Urls {
    fun fromInput(input: String): String? {
        val value = input.trim()
        if (value.length > 8192 || value.isBlank() || value.any { it.isWhitespace() }) return null
        val candidate = if (value.contains("://")) value else "https://$value"
        val normalized = normalize(candidate) ?: return null
        val host = URI(normalized).host
        return normalized.takeIf { host.contains('.') || host.startsWith('[') || host == "localhost" }
    }
    fun defaultTitle(url: String): String {
        val uri = URI(url)
        val host = uri.host.removePrefix("www.")
        val path = uri.path.orEmpty().split('/').lastOrNull { it.isNotBlank() }
        return if (path == null) host else "$host · ${path.take(64)}"
    }
    fun normalize(input: String): String? = runCatching {
        val value = input.trim()
        val uri = URI(value)
        require(uri.scheme?.lowercase() in setOf("https", "http"))
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null)
        require(uri.port == -1 || uri.port in 1..65535)
        // Keep original escaping, query and fragment, including shared-file resource keys.
        uri.scheme.lowercase() + value.substring(value.indexOf(':'))
    }.getOrNull()
    fun extract(text: String): List<String> = Regex("https?://[^\\s<>\\\"\\u0000-\\u001f]+", RegexOption.IGNORE_CASE)
        .findAll(text).mapNotNull { match ->
            var candidate = match.value.trimEnd('.', ',', ';', '!', '?', '。', '，')
            while (candidate.endsWith(')') && candidate.count { it == ')' } > candidate.count { it == '(' }) candidate = candidate.dropLast(1)
            while (candidate.endsWith(']') && candidate.count { it == ']' } > candidate.count { it == '[' }) candidate = candidate.dropLast(1)
            normalize(candidate)
        }.distinct().toList()
}
object DailyPolicy {
    fun day(now: Long, zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()
    fun due(lastCompletedDay: String?, today: String): Boolean = lastCompletedDay != today
    fun checkedToday(bookmark: Bookmark, today: String, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        bookmark.checkedAt > 0 && day(bookmark.checkedAt, zone) == today
}
object HealthRules {
    fun classify(code: Int, provider: Provider, loginPage: Boolean, previous404: Long, now: Long): CheckResult = when {
        loginPage || code == 401 || code == 403 -> CheckResult(Health.UNCERTAIN, "Cần đăng nhập hoặc máy chủ chặn kiểm tra", code)
        code in 200..299 -> CheckResult(Health.REACHABLE, "Máy chủ phản hồi thành công; chưa xác nhận nội dung riêng tư", code)
        code == 410 -> CheckResult(Health.UNAVAILABLE, "HTTP 410: tài nguyên không còn khả dụng", code)
        code == 404 && provider in setOf(Provider.FACEBOOK, Provider.TIKTOK, Provider.DRIVE, Provider.ONEDRIVE, Provider.GITHUB) ->
            CheckResult(Health.UNCERTAIN, "HTTP 404: có thể thiếu quyền hoặc nội dung không tồn tại", code)
        code == 404 && previous404 > 0 && now - previous404 >= 6 * 60 * 60 * 1000 ->
            CheckResult(Health.UNAVAILABLE, "HTTP 404 lặp lại: không tìm thấy tài nguyên", code, previous404)
        code == 404 -> CheckResult(Health.UNCERTAIN, "HTTP 404: chờ xác minh ở lần kiểm tra sau", code, previous404.takeIf { it > 0 } ?: now)
        code == 429 -> CheckResult(Health.UNCERTAIN, "Máy chủ giới hạn tần suất; sẽ thử lại vào ngày sau", code)
        code in 500..599 -> CheckResult(Health.UNCERTAIN, "Máy chủ tạm thời gặp lỗi HTTP $code", code)
        else -> CheckResult(Health.UNCERTAIN, "Chưa xác định (HTTP $code)", code)
    }
}

object HtmlSignals {
    fun needsVerification(html: String): Boolean {
        val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)?.lowercase().orEmpty()
        return Regex("<input\\b[^>]*\\btype\\s*=\\s*[\"']?password\\b", RegexOption.IGNORE_CASE).containsMatchIn(html) ||
            listOf("just a moment", "access denied", "sign in", "log in", "login", "captcha", "security check", "not available", "unavailable").any { it in title } ||
            "cf-chl-" in html.lowercase()
    }
}
