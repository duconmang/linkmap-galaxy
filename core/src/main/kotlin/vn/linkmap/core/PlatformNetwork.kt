package vn.linkmap.core

import java.net.URI

data class PlatformEdge(val fromId: String, val toId: String, val platform: String)

/** A connected, thin network per platform, independent of collection, age and health. */
object PlatformNetwork {
    fun key(bookmark: Bookmark): String = if(bookmark.isNote) "note:${bookmark.id}" else if(bookmark.provider!=Provider.OTHER) "platform:${bookmark.provider.name}"
        else "host:"+URI(bookmark.url).host.lowercase().removePrefix("www.")

    fun edges(layout: MapLayout): List<PlatformEdge> = buildList {
        layout.tiles.filterNot { it.bookmark.isNote }.groupBy { key(it.bookmark) }.toSortedMap().forEach { (platform, group) ->
            val nodes=group.sortedBy { it.bookmark.id }
            if(nodes.size<2) return@forEach
            // Prim's tree connects every node, without the n*(n-1)/2 lines of a full mesh.
            val visited=BooleanArray(nodes.size)
            val distance=DoubleArray(nodes.size) { Double.POSITIVE_INFINITY }
            val parent=IntArray(nodes.size) { -1 }
            distance[0]=0.0
            repeat(nodes.size) {
                var nearest=-1
                for(i in nodes.indices) if(!visited[i] && (nearest<0 || distance[i]<distance[nearest])) nearest=i
                visited[nearest]=true
                if(parent[nearest]>=0) add(PlatformEdge(nodes[parent[nearest]].bookmark.id,nodes[nearest].bookmark.id,platform))
                val source=nodes[nearest]
                for(i in nodes.indices) if(!visited[i]) {
                    val dx=(source.x-nodes[i].x).toDouble();val dy=(source.y-nodes[i].y).toDouble()
                    val squared=dx*dx+dy*dy
                    if(squared<distance[i]) { distance[i]=squared;parent[i]=nearest }
                }
            }
        }
    }
}
