package vn.linkmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import android.content.Intent
import vn.linkmap.ui.LinkMapRoot
import vn.linkmap.ui.LinkMapTheme

class MainActivity : ComponentActivity() {
    private val command=mutableStateOf<Pair<Long,Intent>?>(null)
    private val app get() = application as LinkMapApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        command.value=System.nanoTime() to intent
        setContent { LinkMapTheme { LinkMapRoot(app,command.value) } }
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);setIntent(intent);command.value=System.nanoTime() to intent }
    override fun onStart() { super.onStart(); app.entered() }
    override fun onStop() { app.left(); super.onStop() }
}
