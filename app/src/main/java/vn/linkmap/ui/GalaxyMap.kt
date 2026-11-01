package vn.linkmap.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.linkmap.R
import vn.linkmap.core.*
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.isActive
import androidx.lifecycle.repeatOnLifecycle
import kotlin.random.Random

// Multiply the original light and shadow detail instead of replacing every pixel with a flat tint.
private fun nebulaTint(color: Color) = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    color.red, 0f, 0f, 0f, 0f,
    0f, color.green, 0f, 0f, 0f,
    0f, 0f, color.blue, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)))

private data class Star(val x: Float, val y: Float, val light: Float)

@Composable fun StarField(modifier: Modifier, effects: Boolean, intensity: Float = 1f) {
    val cloud = ImageBitmap.imageResource(R.drawable.nebula_cloud)
    val stars = remember { val r = Random(72); List(850) { Star(r.nextFloat(), r.nextFloat(), r.nextFloat()) } }
    val blueCloud = remember { nebulaTint(Color(0xFF3566BF)) }
    val violetCloud = remember { nebulaTint(Color(0xFF8451B2)) }
    Canvas(modifier.clipToBounds()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF020817), Color(0xFF060B21), Color(0xFF020A1D))))
        if (effects) {
            // One continuous galaxy, including the area behind transparent app bars.
            val width = size.width * 1.7f
            val height = size.height * .72f
            withTransform({ rotate(-24f, center) }) {
                drawImage(cloud, dstOffset = IntOffset((-size.width * .5f).toInt(), (size.height * .32f).toInt()),
                    dstSize = IntSize(width.toInt(), height.toInt()), alpha = .62f * intensity, colorFilter = blueCloud, filterQuality = FilterQuality.Medium)
                drawImage(cloud, dstOffset = IntOffset((size.width * .1f).toInt(), (-size.height * .1f).toInt()),
                    dstSize = IntSize(width.toInt(), height.toInt()), alpha = .48f * intensity, colorFilter = violetCloud, filterQuality = FilterQuality.Medium)
            }
            stars.forEach { star ->
                val pos = Offset(star.x * size.width, star.y * size.height)
                val bright = star.light > .975f
                val radius = (if (bright) 1.05f else .22f + star.light * .42f).dp.toPx()
                val color = if (star.light > .65f) Color(0xFFCFDCFF) else Color(0xFF7199D9)
                drawCircle(color.copy(alpha = .22f + star.light * .65f), radius, pos)
                if (bright) {
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .24f), Color.Transparent), pos, radius * 6), radius * 6, pos)
                    drawLine(color.copy(alpha = .65f), pos - Offset(radius * 3.5f, 0f), pos + Offset(radius * 3.5f, 0f), .4.dp.toPx())
                    drawLine(color.copy(alpha = .65f), pos - Offset(0f, radius * 3.5f), pos + Offset(0f, radius * 3.5f), .4.dp.toPx())
                }
            }
        }
    }
}


@Composable fun GalaxyMap(
    bookmarks: List<Bookmark>, selectedId: String?, focusId: String?, effects: Boolean,
    onSelect: (String) -> Unit, onFocusConsumed: () -> Unit,
    highlighted: Set<String> = bookmarks.map { it.id }.toSet(), reading: Map<String, ReadingState> = emptyMap(),
    starTiles: Boolean = true, twinkle: Boolean = true,
    onOpen: (String) -> Unit = onSelect, onDetails: (String) -> Unit = onSelect,
    overviewRequest: Int = 0
) {
    val layout=remember(bookmarks.map { it.id to it.slot }) { GalaxyPresentation.layout(bookmarks) }
    val records=remember(bookmarks) { bookmarks.associateBy { it.id } }
    val nodesById=remember(layout) { layout.tiles.associateBy { it.bookmark.id } }
    val platformEdges=remember(bookmarks.map { Triple(it.id,it.url,it.slot) }) { PlatformNetwork.edges(GalaxyPresentation.layout(bookmarks)) }
    val platformCounts=remember(bookmarks.map { it.id to it.url }) { bookmarks.groupingBy { PlatformNetwork.key(it) }.eachCount() }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var fitted by rememberSaveable { mutableStateOf(false) }
    var previousX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var previousY by rememberSaveable { mutableFloatStateOf(0f) }
    var previousZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context=LocalContext.current
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val clock=remember { mutableFloatStateOf(0f) }
    val animationStart=remember { android.os.SystemClock.elapsedRealtime() }
    val animate=twinkle && android.provider.Settings.Global.getFloat(context.contentResolver,android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f
    LaunchedEffect(lifecycle,animate) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            if(animate) while(kotlinx.coroutines.currentCoroutineContext().isActive) {
                clock.floatValue=(android.os.SystemClock.elapsedRealtime()-animationStart)/1000f
                kotlinx.coroutines.delay(100)
            }
        }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while(kotlinx.coroutines.currentCoroutineContext().isActive) { now=System.currentTimeMillis();kotlinx.coroutines.delay(60_000) }
        }
    }
    val tapEdge=with(LocalDensity.current) { 48.dp.toPx() }
    val labelSize=with(LocalDensity.current) { 12.sp.toPx() }
    val rimUnit=with(LocalDensity.current) { 1.25.dp.toPx() }
    val maxZoom=tapEdge*4/MapGeometry.TILE
    val ageSizes=remember(bookmarks,now) { bookmarks.associate { it.id to MapGeometry.TILE*GalaxyPresentation.ageScale(it.createdAt,now) } }
    val glyphs=remember {
        mapOf(Provider.FACEBOOK to R.drawable.brand_facebook,Provider.TIKTOK to R.drawable.brand_tiktok,
            Provider.GITHUB to R.drawable.brand_github,Provider.DRIVE to R.drawable.brand_googledrive).mapValues { (_,id) ->
            val drawable=requireNotNull(context.getDrawable(id));val bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
            drawable.setBounds(0,0,96,96);drawable.draw(android.graphics.Canvas(bitmap));bitmap.asImageBitmap()
        }
    }
    fun fit() {
        if(viewport==IntSize.Zero) return
        val relevant=layout.tiles.filter { it.bookmark.id in highlighted }
        val left=(relevant.minOfOrNull { it.x } ?: layout.left)-18
        val top=(relevant.minOfOrNull { it.y } ?: layout.top)-22
        val right=(relevant.maxOfOrNull { it.x } ?: 0f)+MapGeometry.TILE+18
        val bottom=(relevant.maxOfOrNull { it.y } ?: 0f)+MapGeometry.TILE+36
        val paddingX=tapEdge*.55f
        val paddingY=tapEdge*.7f
        zoom=min((viewport.width-paddingX*2)/(right-left),(viewport.height-paddingY*2)/(bottom-top)).coerceIn(.04f,maxZoom)
        panX=viewport.width/2f-(left+right)/2*zoom;panY=viewport.height/2f-(top+bottom)/2*zoom;fitted=true
    }
    fun scale(factor:Float) {
        val next=(zoom*factor).coerceIn(.04f,maxZoom)
        val center=Offset(viewport.width/2f,viewport.height/2f)
        panX=center.x-(center.x-panX)*next/zoom;panY=center.y-(center.y-panY)*next/zoom;zoom=next
    }
    LaunchedEffect(viewport,overviewRequest) {
        if(viewport!=IntSize.Zero) fit()
    }
    LaunchedEffect(focusId,viewport) {
        val tile=layout.tiles.find { it.bookmark.id==focusId }
        if(tile!=null && viewport!=IntSize.Zero) {
            previousX=panX;previousY=panY;previousZoom=zoom
            zoom=(tapEdge*2.1f/(ageSizes[tile.bookmark.id] ?: MapGeometry.TILE)).coerceAtMost(maxZoom)
            panX=viewport.width/2f-(tile.x+26)*zoom;panY=viewport.height*.56f-(tile.y+26)*zoom
            onFocusConsumed()
        }
    }
    val paint=remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("sans-serif",Typeface.NORMAL) } }
    val chosen=records[selectedId]?.takeIf { it.id in highlighted }
    val chosenTile=layout.tiles.find { it.bookmark.id==selectedId }
    val chosenPlatform=chosen?.let { PlatformNetwork.key(it) }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        Canvas(Modifier.fillMaxSize().onSizeChanged { viewport=it }
            .semantics { contentDescription="Thiên hà ${bookmarks.size} bookmark. Sao mới lớn, sao cũ nhỏ. Kéo để di chuyển, chụm để zoom. Dùng Thư viện để đọc từng link." }
            .pointerInput(layout) {
                detectTransformGestures { center,pan,gestureZoom,_ ->
                    val next=(zoom*gestureZoom).coerceIn(.04f,maxZoom)
                    panX=center.x-(center.x-panX)*next/zoom+pan.x;panY=center.y-(center.y-panY)*next/zoom+pan.y;zoom=next
                }
            }.pointerInput(layout,highlighted,selectedId,ageSizes) {
                fun hit(tap:Offset)=layout.tiles.filter { it.bookmark.id in highlighted }.map { tile ->
                    val center=Offset((tile.x+26)*zoom+panX,(tile.y+26)*zoom+panY)
                    tile to (tap-center).getDistance()
                }.filter { (tile,distance) -> distance<=maxOf(tapEdge/2,(ageSizes[tile.bookmark.id] ?: MapGeometry.TILE)*zoom/2) }.minByOrNull { it.second }?.first
                detectTapGestures(onTap={ tap -> hit(tap)?.let { if(it.bookmark.id==selectedId) onOpen(it.bookmark.id) else onSelect(it.bookmark.id) } },
                    onLongPress={tap -> hit(tap)?.let { onDetails(it.bookmark.id) } },onDoubleTap={scale(1.4f)})
            }) {
            val seconds=clock.floatValue
            fun center(tile:MapTile)=Offset((tile.x+26)*zoom+panX,(tile.y+26)*zoom+panY)
            platformEdges.forEach { edge ->
                val from=nodesById[edge.fromId] ?: return@forEach
                val to=nodesById[edge.toId] ?: return@forEach
                val a=center(from);val b=center(to)
                if((a.x<0 && b.x<0) || (a.x>size.width && b.x>size.width) || (a.y<0 && b.y<0) || (a.y>size.height && b.y>size.height)) return@forEach
                val visible=edge.fromId in highlighted && edge.toId in highlighted
                val selected=edge.platform==chosenPlatform
                val opacity=if(!visible) .035f else if(selected) .7f else if(chosenPlatform==null) .36f else .14f
                drawLine(platformNetworkColor(edge.platform).copy(alpha=opacity),a,b,
                    strokeWidth=if(selected) 1.35.dp.toPx() else .65.dp.toPx())
            }
            val occupied=mutableListOf<androidx.compose.ui.geometry.Rect>()
            val sorted=layout.tiles.sortedBy { if(it.bookmark.id==selectedId) 0 else 1 }
            sorted.forEach { tile ->
                val b=records[tile.bookmark.id] ?: return@forEach
                val c=center(tile);val edge=(ageSizes[b.id] ?: MapGeometry.TILE)*zoom
                if(c.x+edge<0 || c.y+edge<0 || c.x-edge>size.width || c.y-edge>size.height) return@forEach
                val pos=c-Offset(edge/2,edge/2)
                val alpha=if(b.id in highlighted) 1f else .12f
                val color=b.entryColor()
                val pulse=if(animate) GalaxyPresentation.pulse(b.id,seconds) else .5f
                val crest=if(animate) pulse.pow(2.2f) else .32f
                val selected=b.id==selectedId && b.id in highlighted
                // At overview scale a few pixels cannot resolve a logo or five tips.
                // Render a light point instead of thousands of tiny stroked paths.
                if(edge<12f && !selected) {
                    drawCircle(lerp(color,Color.White,crest*.85f).copy(alpha=(.25f+.75f*crest)*alpha),maxOf(1f,edge*.35f),c)
                    return@forEach
                }
                val star=if(starTiles) bookmarkStarPath(pos,edge) else null
                if(effects || animate) {
                    val radius=edge*(if(selected) 1.35f else .95f)
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha=(.025f+.4f*crest)*alpha),Color.Transparent),c,radius),radius,c)
                }
                if(selected) {
                    listOf(.72f,1.02f,1.4f).forEach { radius -> drawCircle(color.copy(alpha=.13f),edge*radius,c,style=Stroke(1f)) }
                }
                val fill=Brush.radialGradient(listOf(color.copy(alpha=.8f*alpha),color.copy(alpha=.12f*alpha)),c,edge*.65f)
                if(star!=null) {
                    drawPath(star,fill)
                    val rimWidth=rimUnit*(.8f+.65f*crest)
                    drawPath(star,color.copy(alpha=(.025f+.25f*crest)*alpha),style=Stroke(rimWidth*3.5f))
                    drawPath(star,lerp(color,Color.White,crest*.9f).copy(alpha=(.12f+.88f*crest)*alpha),style=Stroke(rimWidth))
                } else {
                    drawRoundRect(fill,pos,Size(edge,edge),CornerRadius(edge*.15f))
                    drawRoundRect(lerp(color,Color.White,crest*.9f).copy(alpha=(.12f+.88f*crest)*alpha),pos,Size(edge,edge),CornerRadius(edge*.15f),style=Stroke(rimUnit*(.8f+.65f*crest)))
                }
                if(edge>12f) {
                    val icon=if(b.isNote) null else glyphs[b.provider]
                    if(icon!=null) {
                        val iconEdge=edge*(if(starTiles) .38f else .56f)
                        drawImage(icon,dstOffset=IntOffset((c.x-iconEdge/2).toInt(),(c.y-iconEdge/2).toInt()),dstSize=IntSize(iconEdge.toInt().coerceAtLeast(1),iconEdge.toInt().coerceAtLeast(1)),alpha=alpha)
                    } else drawIntoCanvas { canvas ->
                        paint.color=Color.White.copy(alpha=alpha).toArgb();paint.textSize=edge*.32f;paint.textAlign=Paint.Align.CENTER
                        val glyph=when { b.isNote -> "▤"; "figma.com" in b.url -> "✦"; "notion.so" in b.url -> "▤"; "anthropic.com" in b.url -> "✳"; "huggingface.co" in b.url -> "AI";else -> platformLetter(b.provider) }
                        canvas.nativeCanvas.drawText(glyph,c.x,c.y+paint.textSize*.34f,paint)
                    }
                }
                if(edge>=tapEdge*.48f && b.id in highlighted && c.x in 0f..size.width && c.y in 0f..size.height) {
                    paint.textSize=labelSize*(if(selected) 1.1f else 1f);paint.textAlign=Paint.Align.CENTER
                    val width=maxOf(edge*1.6f,tapEdge*(if(selected) 3.5f else 1.7f)).coerceAtMost(size.width*.6f)
                    val count=paint.breakText(b.title,true,width-paint.measureText("…"),null)
                    val title=b.title.take(count)+(if(count<b.title.length) "…" else "")
                    val textWidth=paint.measureText(title)
                    val label=androidx.compose.ui.geometry.Rect(c.x-textWidth/2-8,c.y+edge*.54f,c.x+textWidth/2+8,c.y+edge*.54f+labelSize*1.6f)
                    if(label.bottom<size.height-tapEdge*.65f && (selected || occupied.none { it.overlaps(label) })) {
                        occupied.add(label)
                        drawRoundRect(Space.copy(alpha=.84f),label.topLeft,label.size,CornerRadius(14f))
                        drawIntoCanvas { canvas -> paint.color=Color(0xFFE1E7F5).toArgb();canvas.nativeCanvas.drawText(title,c.x,label.top+labelSize*1.18f,paint) }
                    }
                }
            }
        }
        if(chosen!=null && chosenTile!=null) {
            val c=Offset((chosenTile.x+26)*zoom+panX,(chosenTile.y+26)*zoom+panY)
            val density=LocalDensity.current
            val bubbleWidth=with(density) { 246.dp.toPx() }
            val nodeEdge=(ageSizes[chosen.id] ?: MapGeometry.TILE)*zoom
            if(c.x in 0f..viewport.width.toFloat() && c.y in 0f..viewport.height.toFloat()) Surface(
                modifier=Modifier.offset { IntOffset((c.x-bubbleWidth/2).coerceIn(0f,(viewport.width-bubbleWidth).coerceAtLeast(0f)).toInt(),(c.y-nodeEdge*.75f-with(density){80.dp.toPx()}).coerceAtLeast(8f).toInt()) }.width(246.dp),
                color=Panel.copy(alpha=.94f),shape=MaterialTheme.shapes.large,border=androidx.compose.foundation.BorderStroke(1.dp,chosen.entryColor().copy(alpha=.6f))
            ) {
                Column(Modifier.padding(horizontal=12.dp,vertical=6.dp)) {
                    val networkSize=platformCounts[chosenPlatform] ?: 1
                    Text(if(chosen.isNote) "▤ Ghi chú văn bản" else "${if(chosen.provider==Provider.OTHER) chosenPlatform.orEmpty().removePrefix("host:") else chosen.provider.label} · $networkSize link cùng nền tảng",color=if(chosen.isNote) NoteColor else platformNetworkColor(chosenPlatform.orEmpty()),style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(if(chosen.isNote) chosen.note else chosen.url.removePrefix("https://").removePrefix("http://"),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,color=chosen.entryColor(),style=MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(if(chosen.isNote) "Không cần kiểm tra link" else "${healthSymbol(chosen.health)} ${chosen.health.label}",color=Muted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f))
                        TextButton(onClick={onOpen(chosen.id)},contentPadding=PaddingValues(4.dp)) { Text("Mở") }
                        TextButton(onClick={onDetails(chosen.id)},contentPadding=PaddingValues(4.dp)) { Text("Chi tiết") }
                    }
                }
            }
        }
        if(!previousX.isNaN()) TextButton(onClick={panX=previousX;panY=previousY;zoom=previousZoom;previousX=Float.NaN},modifier=Modifier.align(Alignment.TopStart)) { Text("Về vị trí trước") }
        Surface(modifier=Modifier.align(Alignment.BottomEnd).padding(end=16.dp,bottom=48.dp),color=Color(0xFF0D1228).copy(alpha=.78f),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=.13f))) {
            Column(Modifier.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                listOf(Triple(Icons.Default.Add,"Phóng to",{scale(1.35f)}),Triple(Icons.Default.Remove,"Thu nhỏ",{scale(1/1.35f)}),Triple(Icons.Default.CenterFocusStrong,"Xem toàn bản đồ",{fit()})).forEachIndexed { i,(icon,label,action) ->
                    if(i>0) HorizontalDivider(Modifier.width(22.dp),color=Color.White.copy(alpha=.1f))
                    IconButton(onClick=action,modifier=Modifier.size(44.dp)) { Icon(icon,label,tint=if(i==2) StitchCyan else Muted,modifier=Modifier.size(20.dp)) }
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal=12.dp,vertical=10.dp),color=Space.copy(alpha=.82f),shape=MaterialTheme.shapes.extraLarge) {
            Text("• Chụm để zoom · Chạm sao để chọn · Giữ để xem",modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.labelSmall,color=Muted)
        }
    }
}
