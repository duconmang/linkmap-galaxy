package vn.linkmap.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable fun CosmicModal(title:String,subtitle:String,enabled:Boolean,onDismiss:()->Unit,footer:@Composable ()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest={if(enabled) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=enabled,dismissOnClickOutside=enabled)) {
        val view=LocalView.current
        val blurRadius=with(LocalDensity.current) {16.dp.roundToPx()}
        DisposableEffect(view,blurRadius) {
            val window=(view.parent as? DialogWindowProvider)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(.64f)
            if(Build.VERSION.SDK_INT>=31 && window!=null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes=window.attributes.apply {blurBehindRadius=blurRadius}
            }
            onDispose {if(Build.VERSION.SDK_INT>=31) window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)}
        }
        val shape=RoundedCornerShape(26.dp)
        Box(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=20.dp),contentAlignment=Alignment.Center) {
            Column(Modifier.widthIn(max=480.dp).fillMaxWidth().heightIn(max=LocalConfiguration.current.screenHeightDp.dp*.86f)
                .shadow(28.dp,shape,ambientColor=StitchCyan.copy(alpha=.16f),spotColor=StitchCyan.copy(alpha=.18f))
                .shadow(12.dp,shape,ambientColor=Color.Black,spotColor=Color.Black)
                .clip(shape).background(Brush.linearGradient(listOf(Color(0xFF102033),Color(0xFF0D1226))))
                .border(BorderStroke(1.dp,Brush.linearGradient(listOf(StitchCyan.copy(alpha=.38f),Color(0xFF7481E8).copy(alpha=.22f),StitchCyan.copy(alpha=.2f)))),shape)
                .padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).shadow(12.dp,RoundedCornerShape(14.dp),spotColor=StitchCyan.copy(alpha=.45f))
                        .background(Brush.linearGradient(listOf(StitchCyan,Color(0xFF6366F1))),RoundedCornerShape(14.dp)).padding(1.5.dp)
                        .background(Color(0xFF09182B),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.Link,null,tint=Color(0xFF77DDFF),modifier=Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(title,fontWeight=FontWeight.Bold,fontSize=19.sp,color=Color(0xFFF0F5FF))
                        Text(subtitle,color=Muted,fontSize=11.sp,lineHeight=16.sp)
                    }
                    IconButton(onClick=onDismiss,enabled=enabled,modifier=Modifier.size(40.dp)) {
                        Surface(shape=RoundedCornerShape(20.dp),color=Color.Black.copy(alpha=.15f),border=BorderStroke(1.dp,Color.White.copy(alpha=.1f))) {
                            Icon(Icons.Default.Close,"Đóng hộp nhập",tint=Muted,modifier=Modifier.padding(6.dp).size(18.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
                footer()
            }
        }
    }
}
