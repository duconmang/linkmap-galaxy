package vn.linkmap.core

import kotlin.math.*

/** Appearance never changes a bookmark's stable position or its health. */
object GalaxyPresentation {
    fun ageScale(createdAt: Long, now: Long): Float {
        val days=((now.toDouble()-createdAt.toDouble()).coerceAtLeast(0.0)/86_400_000.0)
        return (.34 + .66 * exp(-days/120.0)).toFloat()
    }
    fun pulse(id: String, seconds: Float): Float {
        var mixed=id.hashCode()
        mixed=(mixed xor (mixed ushr 16))*0x45d9f3b
        mixed=(mixed xor (mixed ushr 16))*0x45d9f3b
        val seed=(mixed xor (mixed ushr 16)).toLong() and 0x7fffffff
        val period=8f+(seed%61)/10f
        val phase=(seed%1000)/1000f*(2*PI).toFloat()
        return (.5+.5*sin(seconds/period*2*PI+phase)).toFloat()
    }
    fun position(slot:Int):Pair<Float,Float> {
        require(slot>=0)
        if(slot==0) return 0f to 0f
        val angle=slot*2.399963229728653
        val radius=sqrt(slot.toDouble()+1)*30.0+15.0
        return (cos(angle)*radius).toFloat() to (sin(angle)*radius*1.28).toFloat()
    }
    fun layout(bookmarks: List<Bookmark>): MapLayout {
        val tiles=bookmarks.map { b -> val (x,y)=position(b.slot);MapTile(b,x-MapGeometry.TILE/2,y-MapGeometry.TILE/2) }
        val left=(tiles.minOfOrNull { it.x } ?: 0f)-80f
        val top=(tiles.minOfOrNull { it.y } ?: 0f)-80f
        val right=(tiles.maxOfOrNull { it.x } ?: 0f)+MapGeometry.TILE+80f
        val bottom=(tiles.maxOfOrNull { it.y } ?: 0f)+MapGeometry.TILE+100f
        return MapLayout(tiles,right-left,bottom-top,left,top)
    }
}
