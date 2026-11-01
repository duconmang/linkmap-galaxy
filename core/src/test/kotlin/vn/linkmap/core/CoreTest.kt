package vn.linkmap.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CoreTest {
    @Test fun `sync concurrent edits converge and stale health cannot resurrect deletion`() {
        val b=Bookmark(SyncRules.id("https://example.com"),"https://example.com","Example")
        val a=SyncRecord(b.copy(title="A"),Revision(4,"android"))
        val w=SyncRecord(b.copy(title="B"),Revision(4,"web"))
        assertEquals(SyncRules.merge(a,w),SyncRules.merge(w,a))
        assertEquals("B",SyncRules.merge(a,w).bookmark.title)
        val deleted=a.copy(revision=Revision(5,"android"),deleted=true)
        val late=a.copy(bookmark=b.copy(health=Health.REACHABLE,checkedAt=100),healthDevice="old-phone")
        assertTrue(SyncRules.merge(deleted,late).deleted)
        assertEquals(SyncRules.merge(deleted,late),SyncRules.merge(late,deleted))
    }
    @Test fun `sync independent health never overwrites newer user content or a changed URL`() {
        val b=Bookmark(SyncRules.id("https://example.com"),"https://example.com","Example")
        val a=SyncRecord(b.copy(title="Edited"),Revision(7,"web"))
        val health=SyncRecord(b.copy(health=Health.REACHABLE,checkedAt=200),Revision(1,"android"),healthDevice="phone")
        assertEquals("Edited",SyncRules.merge(a,health).bookmark.title)
        assertEquals(200L,SyncRules.merge(a,health).bookmark.checkedAt)
        assertEquals(0L,SyncRules.merge(a.copy(bookmark=a.bookmark.copy(url="https://example.org")),health).bookmark.checkedAt)
    }
    @Test fun `calendar day uses device timezone rather than elapsed 24 hours`() {
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        val before = Instant.parse("2026-10-27T16:59:00Z").toEpochMilli()
        val after = Instant.parse("2026-10-27T17:01:00Z").toEpochMilli()
        assertEquals("2026-10-27", DailyPolicy.day(before, zone))
        assertEquals("2026-10-28", DailyPolicy.day(after, zone))
        assertTrue(DailyPolicy.due(DailyPolicy.day(before, zone), DailyPolicy.day(after, zone)))
    }
    @Test fun `reopening same day does not recheck completed bookmarks`() {
        val now = Instant.parse("2026-10-27T03:00:00Z").toEpochMilli()
        val b = Bookmark("1", "https://example.com", "Example", checkedAt = now)
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        assertTrue(DailyPolicy.checkedToday(b, "2026-10-27", zone))
        assertFalse(DailyPolicy.due("2026-10-27", "2026-10-27"))
        assertFalse(DailyPolicy.checkedToday(b.copy(checkedAt = 0), "2026-10-27", zone))
    }
    @Test fun `shared text extracts distinct links without dropping resource keys`() {
        assertEquals(listOf("https://drive.google.com/file/d/abc?resourcekey=secret#view", "https://example.com/a_(b)"),
            Urls.extract("Tài liệu https://drive.google.com/file/d/abc?resourcekey=secret#view và (https://example.com/a_(b)). https://example.com/a_(b)"))
    }
    @Test fun `rejects executable schemes and credential URLs`() {
        listOf("javascript:alert(1)", "file:///etc/passwd", "https://user:secret@example.com", "not a link", "https://", "https://example.com:99999").forEach { assertNull(it, Urls.normalize(it)) }
    }
    @Test fun `platform uses hostname boundaries`() {
        assertEquals(Provider.FACEBOOK, Provider.forUrl("https://m.facebook.com/groups/123"))
        assertEquals(Provider.OTHER, Provider.forUrl("https://facebook.com.evil.test"))
        assertEquals(Provider.OTHER, Provider.forUrl("https://notfacebook.com"))
        assertEquals(Provider.DRIVE, Provider.forUrl("https://docs.google.com/document/d/123"))
    }
    @Test fun `private platform 404 never proves content deleted`() {
        listOf(Provider.DRIVE, Provider.ONEDRIVE, Provider.GITHUB, Provider.FACEBOOK, Provider.TIKTOK).forEach { p ->
            assertEquals(Health.UNCERTAIN, HealthRules.classify(404, p, false, 1, 100000000).health)
        }
    }
    @Test fun `login and temporary server errors never turn a link red`() {
        assertEquals(Health.UNCERTAIN, HealthRules.classify(200, Provider.OTHER, true, 0, 100).health)
        listOf(401, 403, 429, 500, 503).forEach { code ->
            assertEquals(Health.UNCERTAIN, HealthRules.classify(code, Provider.OTHER, false, 0, 100).health)
        }
    }
    @Test fun `public 404 needs observations spaced apart`() {
        val first = 1000000L
        val result = HealthRules.classify(404, Provider.OTHER, false, 0, first)
        assertEquals(first, result.notFoundAt)
        assertEquals(Health.UNCERTAIN, result.health)
        assertEquals(Health.UNCERTAIN, HealthRules.classify(404, Provider.OTHER, false, first, first + 1000).health)
        assertEquals(Health.UNAVAILABLE, HealthRules.classify(404, Provider.OTHER, false, first, first + 21600000).health)
        assertEquals(Health.REACHABLE, HealthRules.classify(200, Provider.OTHER, false, first, first + 21600000).health)
        assertEquals(0L, HealthRules.classify(200, Provider.OTHER, false, first, first + 21600000).notFoundAt)
    }
    @Test fun `410 explicitly reports unavailable`() {
        assertEquals(Health.UNAVAILABLE, HealthRules.classify(410, Provider.OTHER, false, 0, 100).health)
    }
    @Test fun `generic captcha script on an otherwise public page is not a login challenge`() {
        assertFalse(HtmlSignals.needsVerification("<title>Public GitHub repository</title><script src='/assets/captcha.js'></script>"))
        assertTrue(HtmlSignals.needsVerification("<title>Sign in to your account</title>"))
        assertTrue(HtmlSignals.needsVerification("<form><input name='secret' type = 'password'></form>"))
        assertTrue(HtmlSignals.needsVerification("<title>Just a moment...</title><script src='/cf-chl-platform'></script>"))
    }
    @Test fun `spiral never overlaps slots and grows along adjacent cells`() {
        val points = (0..10000).map(MapGeometry::spiral)
        assertEquals(points.size, points.distinct().size)
        points.zipWithNext().forEach { (a, b) -> assertEquals(1, kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)) }
    }
    @Test fun `status changes do not move map tiles`() {
        val items = (0..30).map { Bookmark("$it", "https://github.com/test/$it", "test", slot = it) }
        val a = MapGeometry.layout(items).tiles.map { it.x to it.y }
        val b = MapGeometry.layout(items.map { it.copy(health = Health.UNAVAILABLE) }).tiles.map { it.x to it.y }
        assertEquals(a, b)
    }
    @Test fun `changing platform never changes tile position on the shared map`() {
        val items = (0..150).map { Bookmark("$it", "https://example.com/$it", "test", provider = Provider.entries[it % Provider.entries.size], slot = it) }
        val before = MapGeometry.layout(items).tiles.map { it.x to it.y }
        val after = MapGeometry.layout(items.map { it.copy(provider = Provider.FACEBOOK) }).tiles.map { it.x to it.y }
        assertEquals(before, after)
        assertEquals(items.size, before.distinct().size)
    }
    @Test fun `deleting or adding an outer tile preserves coordinates of remaining bookmarks`() {
        val items = (0..20).map { Bookmark("$it", "https://example.com/$it", "test", slot = it) }
        val before = MapGeometry.layout(items).tiles.associate { it.bookmark.id to (it.x to it.y) }
        MapGeometry.layout(items.drop(3) + items.last().copy(id = "new", slot = 35)).tiles.filter { it.bookmark.id != "new" }.forEach {
            assertEquals(before[it.bookmark.id], it.x to it.y)
        }
    }
    @Test fun `quick entry accepts a bare domain and chooses its platform automatically`() {
        val url = Urls.fromInput("  github.com/android/compose-samples?tab=readme#top  ")!!
        assertEquals("https://github.com/android/compose-samples?tab=readme#top", url)
        assertEquals(Provider.GITHUB, Provider.forUrl(url))
        assertEquals("github.com · compose-samples", Urls.defaultTitle(url))
        assertNull(Urls.fromInput("not a link"))
        assertNull(Urls.fromInput("javascript:alert(1)"))
        assertNull(Urls.fromInput("https://user:password@example.com"))
    }
}
