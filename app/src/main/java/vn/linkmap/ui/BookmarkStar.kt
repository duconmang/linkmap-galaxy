package vn.linkmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

// Broad centre leaves room for the provider logo; gently rounded tips avoid sharp visual noise.
private val starVertices = List(10) { index ->
    val angle = -Math.PI / 2 + index * Math.PI / 5
    val radius = if (index % 2 == 0) .49f else .27f
    Offset(.5f + cos(angle).toFloat() * radius, .5f + sin(angle).toFloat() * radius)
}

internal fun bookmarkStarPath(origin: Offset, edge: Float): Path = Path().apply {
    starVertices.indices.forEach { i ->
        val vertex = starVertices[i]
        val before = vertex + (starVertices[(i + 9) % 10] - vertex) * .085f
        val after = vertex + (starVertices[(i + 1) % 10] - vertex) * .085f
        val start = origin + before * edge
        val control = origin + vertex * edge
        val end = origin + after * edge
        if (i == 0) moveTo(start.x, start.y) else lineTo(start.x, start.y)
        quadraticBezierTo(control.x, control.y, end.x, end.y)
    }
    close()
}
