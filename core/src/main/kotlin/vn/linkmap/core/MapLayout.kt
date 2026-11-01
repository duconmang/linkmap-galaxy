package vn.linkmap.core

data class MapTile(val bookmark: Bookmark, val x: Float, val y: Float)
data class MapLayout(val tiles: List<MapTile>, val width: Float, val height: Float, val left: Float, val top: Float)

object MapGeometry {
    const val TILE = 52f
    const val STEP = 76f
    // Global slots are independent of platform. Deleting a bookmark never moves its neighbors.
    fun spiral(slot: Int): Pair<Int, Int> {
        require(slot >= 0)
        if (slot == 0) return 0 to 0
        val ring = kotlin.math.ceil((kotlin.math.sqrt((slot + 1).toDouble()) - 1) / 2).toInt()
        val side = 2 * ring
        val distance = (2 * ring + 1) * (2 * ring + 1) - 1 - slot
        return when {
            distance < side -> ring - distance to ring
            distance < side * 2 -> -ring to ring - (distance - side)
            distance < side * 3 -> -ring + (distance - side * 2) to -ring
            else -> ring to -ring + (distance - side * 3)
        }
    }
    fun layout(bookmarks: List<Bookmark>): MapLayout {
        val tiles = bookmarks.map { b ->
            val (x, y) = spiral(b.slot)
            MapTile(b, x * STEP, y * STEP)
        }
        val left = (tiles.minOfOrNull { it.x } ?: 0f) - 48f
        val top = (tiles.minOfOrNull { it.y } ?: 0f) - 60f
        val right = (tiles.maxOfOrNull { it.x } ?: 0f) + TILE + 48f
        val bottom = (tiles.maxOfOrNull { it.y } ?: 0f) + TILE + 72f
        return MapLayout(tiles, right - left, bottom - top, left, top)
    }
}
