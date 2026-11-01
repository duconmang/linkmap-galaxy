package vn.linkmap

import android.accounts.Account
import android.content.Intent
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import vn.linkmap.data.DriveJournal
import vn.linkmap.data.BookmarkStore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GoogleProfile(val subject:String,val email:String,val name:String)
data class GoogleSyncState(val profile:GoogleProfile?=null,val driveEnabled:Boolean=false,val busy:Boolean=false,val message:String="Chưa đăng nhập Google")

@OptIn(kotlinx.coroutines.FlowPreview::class)
class GoogleSyncController(private val app:LinkMapApplication) {
    private val mutable=MutableStateFlow(GoogleSyncState())
    val state=mutable.asStateFlow()
    private val prefs=app.getSharedPreferences("google-identity",0)
    private val drive=DriveJournal()
    private var token:String?=null
    private var expires=0L
    private var syncJob:Job?=null
    private var refreshJob:Job?=null
    private var resolution:CancellableContinuation<AuthorizationResult>?=null
    val configured get()=BuildConfig.GOOGLE_WEB_CLIENT_ID.endsWith(".apps.googleusercontent.com")
    init {
        app.scope.launch {
            app.stores.collectLatest { store -> store.changes.debounce(1500).collect { trySync() } }
        }
    }
    suspend fun restore() {
        val raw=prefs.getString("profile",null) ?: return
        runCatching {
            val o=JSONObject(raw);val p=GoogleProfile(o.getString("sub"),o.getString("email"),o.getString("name"))
            app.openAccount(p.subject);mutable.value=GoogleSyncState(p,message="Thư viện đã lưu • kết nối Google để đồng bộ")
            trySync()
        }
    }
    suspend fun signIn(activity:ComponentActivity) {
        if(!configured){mutable.value=mutable.value.copy(message="Google Login chưa được cấu hình cho bản build này");return}
        mutable.value=mutable.value.copy(busy=true,message="Đang đăng nhập Google…")
        try {
            val option=GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
            val response=CredentialManager.create(activity).getCredential(activity,GetCredentialRequest(listOf(option)))
            val credential=GoogleIdTokenCredential.createFrom(response.credential.data)
            // The Google SDK delivers this credential. Subject is only a local-cache key; Drive separately verifies its token via Google userinfo.
            val claims=JSONObject(String(Base64.decode(credential.idToken.split('.')[1],Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
            require(claims.getString("aud")==BuildConfig.GOOGLE_WEB_CLIENT_ID)
            val p=GoogleProfile(claims.getString("sub"),credential.id,credential.displayName ?: credential.id)
            refreshJob?.cancelAndJoin();syncJob?.cancelAndJoin();token=null;expires=0
            app.openAccount(p.subject)
            prefs.edit().putString("profile",JSONObject().put("sub",p.subject).put("email",p.email).put("name",p.name).toString()).apply()
            mutable.value=GoogleSyncState(p,message="Đã đăng nhập • bấm Bật đồng bộ để cấp quyền Drive")
        } catch(e:CancellationException){throw e}
        catch(e:Exception){mutable.value=mutable.value.copy(message="Chưa đăng nhập được (${e.javaClass.simpleName}). Kiểm tra OAuth Client ID, SHA-1 và Google Play services.")}
        finally{mutable.value=mutable.value.copy(busy=false)}
        trySync()
    }
    suspend fun enableDrive(activity:ComponentActivity,launch:(IntentSenderRequest)->Unit) {
        val p=mutable.value.profile ?: return
        mutable.value=mutable.value.copy(busy=true,message="Đang kết nối Google Drive…")
        try {
            val request=AuthorizationRequest.builder().setAccount(Account(p.email,"com.google"))
                .setRequestedScopes(listOf(Scope("openid"),Scope("https://www.googleapis.com/auth/userinfo.email"),Scope("https://www.googleapis.com/auth/userinfo.profile"),Scope("https://www.googleapis.com/auth/drive.appdata"))).build()
            var result=Identity.getAuthorizationClient(activity).authorize(request).awaitResult()
            if(result.hasResolution())result=suspendCancellableCoroutine { continuation ->
                resolution=continuation
                continuation.invokeOnCancellation { resolution=null }
                launch(IntentSenderRequest.Builder(requireNotNull(result.pendingIntent).intentSender).build())
            }
            val access=requireNotNull(result.accessToken)
            require(drive.profile(access).getString("sub")==p.subject) { "Tài khoản Drive không trùng tài khoản LinkMap" }
            token=access;expires=System.currentTimeMillis()+45*60*1000
            prefs.edit().putBoolean("drive:${p.subject}",true).apply()
            mutable.value=mutable.value.copy(driveEnabled=true,message="Đã cấp quyền Drive")
        }catch(e:CancellationException){throw e}
        catch(e:Exception){mutable.value=mutable.value.copy(message=e.message ?: "Chưa cấp quyền Drive")}
        finally{mutable.value=mutable.value.copy(busy=false)}
        trySync()
    }
    fun authorizationResult(activity:ComponentActivity,data:Intent?) {
        val continuation=resolution ?: return;resolution=null
        try { continuation.resume(Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)) }
        catch(e:Exception){continuation.resumeWithException(e)}
    }
    fun trySync() {
        val p=mutable.value.profile ?: return
        if(!app.online())return
        if(token==null || System.currentTimeMillis()>=expires) {
            token=null
            if(prefs.getBoolean("drive:${p.subject}",false)) refreshAccess(p)
            return
        }
        val access=token ?: return
        if(!mutable.value.driveEnabled||mutable.value.busy||syncJob?.isActive==true)return
        val store=app.store
        syncJob=app.scope.launch {
            mutable.value=mutable.value.copy(busy=true,message="Đang đồng bộ Drive…")
            var succeeded=false
            try {
                drive.sync(access,p.subject,store)
                succeeded=true
                mutable.value=mutable.value.copy(message=store.meta("syncConflict") ?: "Đã đồng bộ • ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date())}")
                app.checkDaily()
            } catch(e:CancellationException){throw e}
            catch(e:Exception){
                if(e.message?.contains("401")==true) {
                    Identity.getAuthorizationClient(app).clearToken(ClearTokenRequest.builder().setToken(access).build())
                    token=null;mutable.value=mutable.value.copy(driveEnabled=false)
                }
                mutable.value=mutable.value.copy(message=e.message ?: "Chưa đồng bộ được; dữ liệu trên máy vẫn an toàn")
            }
            finally{
                mutable.value=mutable.value.copy(busy=false)
                if(succeeded&&store.pending().isNotEmpty())app.scope.launch { delay(2000); if(token==access)trySync() }
            }
        }
    }
    // Reuse an existing grant; never open a consent dialog from a lifecycle/network callback.
    private fun refreshAccess(profile:GoogleProfile) {
        if(refreshJob?.isActive==true || mutable.value.busy)return
        refreshJob=app.scope.launch {
            mutable.value=mutable.value.copy(busy=true,message="Đang kết nối lại Drive…")
            var connected=false
            try {
                val request=AuthorizationRequest.builder().setAccount(Account(profile.email,"com.google"))
                    .setRequestedScopes(listOf(Scope("openid"),Scope("https://www.googleapis.com/auth/userinfo.email"),Scope("https://www.googleapis.com/auth/userinfo.profile"),Scope("https://www.googleapis.com/auth/drive.appdata"))).build()
                val result=Identity.getAuthorizationClient(app).authorize(request).awaitResult()
                if(mutable.value.profile?.subject!=profile.subject)return@launch
                if(result.hasResolution()) {
                    mutable.value=mutable.value.copy(driveEnabled=false,message="Cần kết nối lại Google Drive để đồng bộ")
                } else {
                    val access=requireNotNull(result.accessToken)
                    require(drive.profile(access).getString("sub")==profile.subject)
                    token=access;expires=System.currentTimeMillis()+45*60*1000
                    connected=true
                    mutable.value=mutable.value.copy(driveEnabled=true,message="Đã kết nối lại Drive")
                }
            } catch(e:CancellationException){throw e}
            catch(_:Exception) {mutable.value=mutable.value.copy(driveEnabled=false,message="Chưa kết nối được Drive • dữ liệu trên máy vẫn được giữ")}
            finally {mutable.value=mutable.value.copy(busy=false)}
            if(connected)trySync()
        }
    }
    suspend fun signOut(activity:ComponentActivity) {
        refreshJob?.cancelAndJoin();syncJob?.cancelAndJoin();token=null;expires=0
        runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
        prefs.edit().remove("profile").apply();mutable.value=GoogleSyncState();app.openAccount(null)
    }
    suspend fun importGuest():Int {
        require(mutable.value.profile!=null)
        val guest=BookmarkStore(app);guest.load()
        return try { app.store.importJson(guest.exportJson()).also { trySync() } } finally { guest.close() }
    }
}
private suspend fun <T> Task<T>.awaitResult():T=suspendCancellableCoroutine { c ->
    addOnSuccessListener { if(c.isActive)c.resume(it) };addOnFailureListener { if(c.isActive)c.resumeWithException(it) }
    addOnCanceledListener { c.cancel() }
}
