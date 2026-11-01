package vn.linkmap.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import vn.linkmap.LinkMapApplication

@Composable fun GoogleSyncPanel(app:LinkMapApplication) {
    val state by app.sync.state.collectAsStateWithLifecycle()
    val activity=LocalActivity.current as? ComponentActivity ?: return
    val scope=rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    val authorization=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { app.sync.authorizationResult(activity,it.data) }
    Text("Google & đồng bộ",style=MaterialTheme.typography.titleLarge)
    Text(state.profile?.let { "${it.name}\n${it.email}" } ?: "Đăng nhập để đồng bộ liên kết và ghi chú qua Google Drive. Các thiết bị cần bản ứng dụng hỗ trợ ghi chú.",color=Muted)
    Text(state.message,color=Blue,style=MaterialTheme.typography.bodySmall)
    if(state.profile==null) {
        Button(onClick={scope.launch{app.sync.signIn(activity)}},enabled=!state.busy&&app.sync.configured){Text("Đăng nhập bằng Google")}
        if(!app.sync.configured)Text("Đăng nhập Google chưa được cấu hình cho bản build này. Thư viện trên máy vẫn hoạt động.",color=Muted,style=MaterialTheme.typography.bodySmall)
    } else {
        Button(onClick={scope.launch{app.sync.enableDrive(activity){authorization.launch(it)}}},enabled=!state.busy){Text(if(state.driveEnabled)"Kết nối lại Google Drive" else "Bật đồng bộ Drive")}
        OutlinedButton(onClick={app.sync.trySync()},enabled=state.driveEnabled&&!state.busy){Text("Đồng bộ ngay")}
        TextButton(onClick={scope.launch {
            try { message="Đã thêm ${app.sync.importGuest()} mục từ thư viện trên máy" }
            catch(e:Exception){message=e.message ?: "Chưa nhập được thư viện; dữ liệu trên máy vẫn được giữ"}
        }},enabled=!state.busy){Text("Đưa thư viện trên máy vào tài khoản này")}
        TextButton(onClick={scope.launch{app.sync.signOut(activity)}},enabled=!state.busy){Text("Đăng xuất")}
        Text("Dữ liệu đồng bộ nằm trong Drive của tài khoản này. Thư viện khách được giữ riêng.",color=Muted,style=MaterialTheme.typography.bodySmall)
        Text("Tự đồng bộ khi mở app, khi có thay đổi và mỗi phút trong lúc sử dụng. Quyền Drive đã cấp được kết nối lại tự động khi có thể.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
    if(message.isNotEmpty())Text(message,color=Muted)
    HorizontalDivider()
}
