package vn.linkmap.core

import org.junit.Assert.*
import org.junit.Test

class GalaxyPresentationTest {
    @Test fun hundredStarsFormACompactCentralGalaxy() {
        val records=(0 until 100).map { Bookmark("$it","https://example.com/$it","$it",slot=it) }
        val layout=GalaxyPresentation.layout(records)
        assertTrue(layout.width<850f)
        assertTrue(layout.height<1050f)
        assertTrue(layout.tiles.all { kotlin.math.abs(it.x)<360f && kotlin.math.abs(it.y)<450f })
    }
    @Test fun olderStarsShrinkSmoothlyButNeverDisappearAndFutureDatesStayFullSize() {
        val now=2_000_000_000_000L
        val ages=listOf(0,1,30,180,365,3000)
        val sizes=ages.map { GalaxyPresentation.ageScale(now-it*86_400_000L,now) }
        assertEquals(1f,sizes.first(),.0001f)
        assertTrue(sizes.zipWithNext().all { (newer,older) -> newer>older })
        assertTrue(sizes.all { it in .34f..1f })
        assertEquals(1f,GalaxyPresentation.ageScale(now+86400000,now),.0001f)
    }
    @Test fun shrinkingAndFilteringNeverMoveTheRemainingConstellation() {
        val all=(0 until 1000).map { Bookmark("$it","https://example.com/$it","$it",slot=it,createdAt=100) }
        val coordinates=GalaxyPresentation.layout(all).tiles.associate { it.bookmark.id to (it.x to it.y) }
        assertEquals(1000,coordinates.values.toSet().size)
        GalaxyPresentation.layout(all.filter { it.slot%3==0 }.map { it.copy(createdAt=0) }).tiles.forEach {
            assertEquals(coordinates[it.bookmark.id],it.x to it.y)
        }
    }
    @Test fun borderPulsesHaveIndependentSmoothPhasesAndStayWithinOpacityRange() {
        val a=(0..300).map { GalaxyPresentation.pulse("demo-0",it/20f) }
        val b=(0..300).map { GalaxyPresentation.pulse("demo-1",it/20f) }
        assertTrue(a.all { it in 0f..1f });assertTrue(a.zipWithNext().all { (x,y) -> kotlin.math.abs(x-y)<.025f })
        assertNotEquals(a,b)
    }
}
