package vn.linkmap.core

import org.junit.Assert.*
import org.junit.Test

class IncomingContentTest {
    @Test fun textWithoutUrlsBecomesANoteWithoutLosingLineBreaks() {
        val input="Ý tưởng LinkMap\nGiữ những điều cần nhớ."
        val parsed=IncomingContent.detect(input)
        assertEquals(EntryKind.NOTE,parsed.kind);assertEquals(input,parsed.text)
        assertEquals("Ý tưởng LinkMap",parsed.suggestedTitle)
        assertEquals(EntryKind.NOTE,IncomingContent.detect("Email: me@example.com, phiên bản 1.2.3").kind)
    }
    @Test fun linksInsideTextAndSchemeLessDomainsAreDetectedWithoutDiscardingText() {
        val text="Đọc tài liệu https://github.com/android/compose-samples trước thứ Sáu."
        val parsed=IncomingContent.detect(text)
        assertEquals(EntryKind.LINK,parsed.kind);assertEquals(text,parsed.text)
        assertEquals(listOf("https://github.com/android/compose-samples"),parsed.urls)
        assertEquals(listOf("https://github.com/android"),IncomingContent.detect("Xem github.com/android nhé").urls)
        assertEquals(listOf("https://www.google.com"),IncomingContent.detect("www.google.com").urls)
        assertEquals(listOf("https://github.com"),IncomingContent.detect("Xem github.com.").urls)
        assertEquals(1,IncomingContent.detect("Tìm https://www.google.com/search?q=github.com").urls.size)
    }
    @Test fun multipleLinksRemainAvailableForExplicitSelectionAndResourceKeysSurvive() {
        val parsed=IncomingContent.detect("https://drive.google.com/a?resourcekey=secret#view và https://github.com/android https://github.com/android")
        assertEquals(2,parsed.urls.size);assertTrue(parsed.urls.first().endsWith("?resourcekey=secret#view"))
    }
    @Test fun oversizedOrBlankTextIsRejectedBeforeSaving() {
        for(text in listOf("   ","a".repeat(IncomingContent.MAX_LENGTH+1))) {
            try {IncomingContent.detect(text);fail("Must reject invalid content")} catch(_:IllegalArgumentException) {}
        }
    }
    @Test fun notesNeverBecomePlatformEdgesOrHttpWarningsAndMergeAsFullContent() {
        val b=Bookmark("note-00000000-0000-0000-0000-000000000001","","Note",note="Nội dung",kind=EntryKind.NOTE)
        assertTrue(PlatformNetwork.edges(GalaxyPresentation.layout(listOf(b,b.copy(id="note-2",slot=1)))).isEmpty())
        assertFalse(Library.accepts(b.copy(health=Health.UNAVAILABLE),ReadingState(),LibraryFilter.ATTENTION))
        val a=SyncRecord(b,Revision(1,"phone"));val newer=a.copy(bookmark=b.copy(note="Đã sửa"),revision=Revision(2,"tablet"))
        assertEquals("Đã sửa",SyncRules.merge(a,newer).bookmark.note)
        val deleted=newer.copy(deleted=true,revision=Revision(3,"phone"))
        assertTrue(SyncRules.merge(deleted,newer).deleted)
    }
}
