package vn.linkmap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val StitchCyan=Color(0xFF38BDF8)
private val StitchGlass=Color(0xFF0D1228)
private val StitchBorder=Color.White.copy(alpha=.13f)

@Composable fun ConstellationIcon(modifier:Modifier=Modifier,color:Color=StitchCyan) {
    Canvas(modifier) {
        val a=Offset(size.width*.25f,size.height*.72f)
        val b=Offset(size.width*.75f,size.height*.72f)
        val c=Offset(size.width*.5f,size.height*.22f)
        val radius=size.width*.105f
        drawLine(color,a,c,size.width*.07f);drawLine(color,b,c,size.width*.07f)
        listOf(a,b,c).forEach { drawCircle(StitchGlass,radius*1.25f,it);drawCircle(color,radius,it,style=Stroke(size.width*.07f)) }
    }
}

@Composable fun StitchBrandHeader(initials:String,hasAttention:Boolean,onAttention:()->Unit,onProfile:()->Unit,onView:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=52.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(StitchCyan,Color(0xFF6864F6),Color(0xFF22D3EE)))).padding(1.5.dp)
            .clip(RoundedCornerShape(11.dp)).background(StitchGlass).clickable(onClick=onView).semantics { contentDescription="Bản đồ / danh sách" },contentAlignment=Alignment.Center) {
            ConstellationIcon(Modifier.size(24.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Row { Text("Link",fontWeight=FontWeight.Bold,fontSize=19.sp);Text("Map",color=StitchCyan,fontWeight=FontWeight.Bold,fontSize=19.sp) }
                Surface(color=StitchCyan.copy(alpha=.12f),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,StitchCyan.copy(alpha=.3f))) {
                    Text("GALAXY",color=Color(0xFF7DD3FC),fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=5.dp,vertical=1.dp),maxLines=1)
                }
            }
            Text("Knowledge Universe",fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        IconButton(onClick=onAttention,modifier=Modifier.size(48.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(StitchGlass.copy(alpha=.7f))
                .drawBehind { drawRoundRect(StitchBorder,cornerRadius=androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),style=Stroke(1.dp.toPx())) },contentAlignment=Alignment.Center) {
                Icon(Icons.Outlined.NotificationsNone,"Link cần chú ý",tint=Muted,modifier=Modifier.size(20.dp))
                if(hasAttention) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(5.dp).background(StitchCyan,RoundedCornerShape(8.dp)))
            }
        }
        IconButton(onClick=onProfile,modifier=Modifier.size(48.dp)) {
            Surface(shape=RoundedCornerShape(12.dp),color=StitchGlass.copy(alpha=.7f),border=BorderStroke(1.dp,StitchCyan.copy(alpha=.3f)),modifier=Modifier.size(36.dp)) {
                Box(Modifier.padding(5.dp).clip(RoundedCornerShape(7.dp)).background(Brush.linearGradient(listOf(StitchCyan,Color(0xFF6366F1)))).semantics { contentDescription="Hồ sơ người dùng" },contentAlignment=Alignment.Center) {
                    Text(initials,color=Color(0xFF071329),fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
                }
            }
        }
    }
}

@Composable fun StitchSearch(query:String,onQuery:(String)->Unit,onFilter:()->Unit,onCenter:()->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Surface(color=StitchGlass.copy(alpha=.68f),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,StitchBorder),modifier=Modifier.weight(1f)) {
            Row(Modifier.heightIn(min=52.dp).padding(start=14.dp,end=3.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Search,null,tint=StitchCyan,modifier=Modifier.size(20.dp));Spacer(Modifier.width(10.dp))
                BasicTextField(query,onQuery,singleLine=true,textStyle=TextStyle(color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Medium),cursorBrush=Brush.linearGradient(listOf(StitchCyan,StitchCyan)),modifier=Modifier.weight(1f).padding(vertical=12.dp),decorationBox={ field ->
                    Box { if(query.isBlank()) Text("Tìm vì sao & liên kết tri thức…",fontSize=13.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis);field() }
                })
                IconButton(onClick=onFilter,modifier=Modifier.size(44.dp)) { Icon(Icons.Default.FilterList,"Bộ lọc",tint=Muted,modifier=Modifier.size(18.dp)) }
            }
        }
        OutlinedIconButton(onClick=onCenter,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,StitchBorder),colors=IconButtonDefaults.outlinedIconButtonColors(containerColor=StitchGlass.copy(alpha=.68f)),modifier=Modifier.size(52.dp)) {
            Icon(Icons.Default.Navigation,"Về tâm thiên hà",tint=StitchCyan,modifier=Modifier.size(22.dp))
        }
    }
}

@Composable fun StitchBottomBar(tab:Int,onTab:(Int)->Unit,onAdd:()->Unit) {
    Row(Modifier.navigationBarsPadding().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(modifier=Modifier.weight(1f),shape=RoundedCornerShape(24.dp),color=StitchGlass.copy(alpha=.72f),border=BorderStroke(1.dp,StitchBorder)) {
            Row(Modifier.selectableGroup().padding(horizontal=5.dp)) {
                listOf("Bản đồ","Thư viện","Cài đặt").forEachIndexed { i,label ->
                    Column(Modifier.weight(1f).heightIn(min=64.dp).clip(RoundedCornerShape(18.dp))
                        .drawBehind { if(tab==i) drawRect(Brush.radialGradient(listOf(StitchCyan.copy(alpha=.2f),Color.Transparent),center, size.maxDimension*.6f)) }
                        .selectable(selected=tab==i,onClick={onTab(i)},role=Role.Tab).padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        val tint=if(tab==i) StitchCyan else Muted
                        when(i) {
                            0 -> ConstellationIcon(Modifier.size(22.dp).semantics { contentDescription=label },tint)
                            1 -> Icon(Icons.Outlined.FolderOpen,label,tint=tint,modifier=Modifier.size(22.dp))
                            else -> Icon(Icons.Default.Tune,label,tint=tint,modifier=Modifier.size(22.dp).rotate(90f))
                        }
                        Text(label,color=if(tab==i) Color(0xFFBAE6FD) else Muted,fontSize=11.sp,fontWeight=if(tab==i) FontWeight.SemiBold else FontWeight.Medium,maxLines=1)
                    }
                }
            }
        }
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(StitchCyan,Color(0xFF6366F1),Color(0xFF22D3EE)))).padding(1.5.dp)
            .clip(RoundedCornerShape(22.dp)).background(StitchGlass.copy(alpha=.94f)).clickable(role=Role.Button,onClick=onAdd)
            .semantics { contentDescription="Thêm vào LinkMap" },contentAlignment=Alignment.Center) {
            Icon(Icons.Default.Add,null,tint=Color(0xFFBAE6FD),modifier=Modifier.size(30.dp))
        }
    }
}
