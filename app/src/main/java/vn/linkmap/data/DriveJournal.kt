package vn.linkmap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class DriveJournal {
    private val client=OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).build()
    private val jsonType="application/json; charset=utf-8".toMediaType()
    private fun request(token:String,url:String,body:RequestBody?=null):String {
        val builder=Request.Builder().url(url).header("Authorization","Bearer $token")
        if(body!=null)builder.post(body)
        return client.newCall(builder.build()).execute().use { response ->
            if(!response.isSuccessful)throw java.io.IOException("Google Drive HTTP ${response.code}; dữ liệu trên máy được giữ nguyên")
            val bytes=response.body?.source()?.let { source -> source.request(5L*1024*1024+1); source.buffer.clone().readByteArray() } ?: byteArrayOf()
            require(bytes.size<=5*1024*1024) { "Nhật ký Drive vượt 5 MB" }
            bytes.toString(Charsets.UTF_8)
        }
    }
    suspend fun profile(token:String):JSONObject=withContext(Dispatchers.IO) {
        JSONObject(request(token,"https://www.googleapis.com/oauth2/v3/userinfo"))
    }
    suspend fun sync(token:String,subject:String,store:BookmarkStore)=withContext(Dispatchers.IO) {
        require(profile(token).getString("sub")==subject) { "Tài khoản Drive không trùng tài khoản LinkMap" }
        var page="";var pages=0
        do {
            require(++pages<=100) { "Nhật ký Drive quá lớn; dừng an toàn" }
            val url=HttpUrl.Builder().scheme("https").host("www.googleapis.com").addPathSegments("drive/v3/files")
                .addQueryParameter("spaces","appDataFolder")
                .addQueryParameter("q","trashed = false and (appProperties has { key='linkmapProtocol' and value='1' } or appProperties has { key='linkmapProtocol' and value='2' })")
                .addQueryParameter("fields","nextPageToken,files(id,size)").addQueryParameter("pageSize","1000")
            if(page.isNotEmpty())url.addQueryParameter("pageToken",page)
            val data=JSONObject(request(token,url.build().toString()));val files=data.optJSONArray("files") ?: JSONArray()
            for(i in 0 until files.length()) {
                val f=files.getJSONObject(i);val id=f.getString("id")
                if(store.seen(id))continue
                require(f.optLong("size")<=5*1024*1024)
                val batch=JSONObject(request(token,"https://www.googleapis.com/drive/v3/files/$id?alt=media"))
                require(batch.getString("protocol") in setOf("linkmap.journal/1","linkmap.journal/2"))
                val records=batch.getJSONArray("records");require(records.length()<=1000)
                val parsed=(0 until records.length()).map { SyncJson.decode(records.getJSONObject(it)) }
                store.mergeRemote(id,parsed)
            }
            page=data.optString("nextPageToken")
        }while(page.isNotEmpty())
        repeat(200) {
            val pending=store.pending();if(pending.isEmpty())return@withContext
            // Bound batches by bytes as well as item count: notes can contain 64,000 characters.
            val records=JSONArray();val selected=mutableListOf<Pair<Long,String>>()
            var bytes=0
            for(entry in pending) { val size=entry.second.toByteArray(Charsets.UTF_8).size; if(bytes+size>3*1024*1024) break;records.put(JSONObject(entry.second));selected.add(entry);bytes+=size }
            require(selected.isNotEmpty())
            val batch=JSONObject().put("protocol","linkmap.journal/2").put("records",records)
            val meta=JSONObject().put("name","linkmap-${UUID.randomUUID()}.json").put("mimeType","application/json")
                .put("parents",JSONArray().put("appDataFolder")).put("appProperties",JSONObject().put("linkmapProtocol","2"))
            val body=MultipartBody.Builder().setType("multipart/related".toMediaType())
                .addPart(meta.toString().toRequestBody(jsonType)).addPart(batch.toString().toRequestBody(jsonType)).build()
            val result=JSONObject(request(token,"https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",body))
            store.mergeRemote(result.getString("id"),selected.map { SyncJson.decode(JSONObject(it.second)) })
            store.acknowledge(selected.map { it.first })
        }
    }
}
