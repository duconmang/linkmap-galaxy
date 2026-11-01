package vn.linkmap.core

import org.junit.Assert.*
import org.junit.Test

class PlatformNetworkTest {
    @Test fun allLinksInEachPlatformAreConnectedWithoutCrossingPlatformBoundaries() {
        val records=(0 until 100).map { i ->
            val host=listOf("github.com","facebook.com","drive.google.com","www.figma.com")[i%4]
            Bookmark("$i","https://$host/$i","$i",category="Group ${i%3}",slot=i)
        }
        val edges=PlatformNetwork.edges(GalaxyPresentation.layout(records))
        assertEquals(96,edges.size)
        val byId=records.associateBy { it.id }
        edges.forEach { assertEquals(PlatformNetwork.key(byId.getValue(it.fromId)),PlatformNetwork.key(byId.getValue(it.toId))) }
        records.groupBy { PlatformNetwork.key(it) }.forEach { (_,group) ->
            val reached=mutableSetOf(group.first().id)
            repeat(group.size) { edges.forEach { e -> if(e.fromId in reached) reached.add(e.toId);if(e.toId in reached) reached.add(e.fromId) } }
            assertTrue(group.all { it.id in reached })
        }
        assertEquals(edges,PlatformNetwork.edges(GalaxyPresentation.layout(records.reversed())))
    }
    @Test fun unrelatedUnknownDomainsNeverBecomeOnePlatform() {
        val records=listOf("https://www.figma.com/a","https://figma.com/b","https://notion.so/a","https://figma.com.evil.test/a")
            .mapIndexed { i,url -> Bookmark("$i",url,"$i",slot=i) }
        val edges=PlatformNetwork.edges(GalaxyPresentation.layout(records))
        assertEquals(1,edges.size)
        assertEquals(setOf("0","1"),setOf(edges.single().fromId,edges.single().toId))
    }
}
