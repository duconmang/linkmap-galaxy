package vn.linkmap.core

import java.text.Normalizer

data class ReadingState(val pinned: Boolean = false, val openedAt: Long = 0, val ignored: Boolean = false, val preview: String = "")
enum class LibraryFilter(val label: String) {
    ALL("Tất cả"), NOTES("Ghi chú"), LINKS("Liên kết"), RECENT("Mới lưu"), OPENED("Vừa mở"), PINNED("Đã ghim"), UNREAD("Chưa mở"), ATTENTION("Cần chú ý")
}
object Library {
    fun fold(text: String): String = Normalizer.normalize(text.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace('đ', 'd')
    fun matches(b: Bookmark, query: String): Boolean {
        val haystack = fold(listOf(b.title, b.url, b.note, b.category, b.provider.label).joinToString(" "))
        return fold(query).trim().split(Regex("\\s+")).all { it in haystack }
    }
    fun accepts(b: Bookmark, state: ReadingState, filter: LibraryFilter): Boolean = when (filter) {
        LibraryFilter.ALL, LibraryFilter.RECENT -> true
        LibraryFilter.NOTES -> b.isNote
        LibraryFilter.LINKS -> !b.isNote
        LibraryFilter.PINNED -> state.pinned
        LibraryFilter.OPENED -> state.openedAt>0
        LibraryFilter.UNREAD -> state.openedAt == 0L
        LibraryFilter.ATTENTION -> !b.isNote && b.health in setOf(Health.UNCERTAIN, Health.UNAVAILABLE) && !state.ignored
    }
    fun text(html: String): String = html.replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
        .replace(Regex("&#(x[0-9a-fA-F]+|[0-9]+);")) { m ->
            val value = m.groupValues[1]
            val code = if (value.startsWith("x")) value.drop(1).toIntOrNull(16) else value.toIntOrNull()
            if (code != null && Character.isValidCodePoint(code)) String(Character.toChars(code)) else ""
        }.replace(Regex("\\s+"), " ").trim()
    fun pageTitle(html: String): String? = Regex("<title\\b[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)?.groupValues?.get(1)?.let(::text)?.take(200)?.takeIf { it.isNotBlank() }
    fun previewUrl(html: String): String? {
        return Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(html).firstNotNullOfOrNull { tag ->
            val attrs = Regex("([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2", RegexOption.DOT_MATCHES_ALL).findAll(tag.value)
                .associate { it.groupValues[1].lowercase() to it.groupValues[3] }
            if (attrs["property"]?.lowercase() == "og:image" || attrs["name"]?.lowercase() == "twitter:image") attrs["content"]?.let(::text) else null
        }
    }
    // Netscape bookmark exports. Only valid web URLs are imported, never script/file links.
    fun browserBookmarks(html: String): List<Pair<String, String>> = Regex("<a\\b[^>]*\\bhref\\s*=\\s*([\"'])(.*?)\\1[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .findAll(html).mapNotNull { m -> Urls.normalize(text(m.groupValues[2]))?.let { it to text(m.groupValues[3]).take(200) } }
        .distinctBy { it.first }.take(10001).toList().also { require(it.size <= 10000) { "Mỗi lần nhập tối đa 10.000 link" } }
}
