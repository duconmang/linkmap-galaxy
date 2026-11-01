package vn.linkmap.core

import org.junit.Assert.*
import org.junit.Test

class LibraryTest {
    @Test fun vietnameseQueriesMatchAcrossFieldsAndAllWordsMustMatch() {
        val b=Bookmark("a","https://example.com/tai-lieu","Đồ án Android",note="Tài liệu cần đọc",category="Công việc")
        assertTrue(Library.matches(b,"do an"));assertTrue(Library.matches(b,"ANDROID cong viec"))
        assertFalse(Library.matches(b,"android du lich"))
    }
    @Test fun ignoredWarningsAndOpeningDoNotChangeTheActualHealth() {
        val b=Bookmark("a","https://example.com","A",health=Health.UNCERTAIN)
        assertTrue(Library.accepts(b,ReadingState(),LibraryFilter.ATTENTION))
        assertFalse(Library.accepts(b,ReadingState(ignored=true),LibraryFilter.ATTENTION))
        assertFalse(Library.accepts(b,ReadingState(openedAt=100),LibraryFilter.UNREAD))
        assertEquals(Health.UNCERTAIN,b.health)
    }
    @Test fun browserImportDecodesEntitiesDeduplicatesAndRejectsExecutableLinks() {
        val html="""<DL><A HREF="https://example.com/a?k=1&amp;x=2">Đồ án &amp; ghi chú</A><a href='javascript:alert(1)'>bad</a><A HREF="file:///secret">bad</A><a href='https://example.com/a?k=1&amp;x=2'>duplicate</a></DL>"""
        assertEquals(listOf("https://example.com/a?k=1&x=2" to "Đồ án & ghi chú"),Library.browserBookmarks(html))
    }
    @Test fun metadataPreservesCapitalizationAndSupportsReorderedAttributes() {
        val html="""<title>Jetpack Compose &amp; Android</title><meta content="https://example.com/Photo.JPG?a=1&amp;b=2" property="og:image">"""
        assertEquals("Jetpack Compose & Android",Library.pageTitle(html))
        assertEquals("https://example.com/Photo.JPG?a=1&b=2",Library.previewUrl(html))
        assertEquals("Đ",Library.text("&#272;"))
    }
    @Test fun thousandBookmarksKeepCoordinatesWhenFilteringAndDeleting() {
        val all=(0 until 1000).map { Bookmark("$it","https://example.com/$it","$it",slot=it) }
        val before=MapGeometry.layout(all).tiles.associate { it.bookmark.id to (it.x to it.y) }
        val subset=MapGeometry.layout(all.filter { it.slot%3==0 }).tiles
        assertEquals(1000,before.values.toSet().size)
        subset.forEach { assertEquals(before[it.bookmark.id],it.x to it.y) }
    }
}
