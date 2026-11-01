package vn.linkmap.data

import android.content.Context
import org.json.JSONArray
import vn.linkmap.core.*

object DemoLibrary {
    fun load(context: Context, now:Long=System.currentTimeMillis()):List<Bookmark> {
        val items=context.assets.open("demo-bookmarks.json").bufferedReader().use { JSONArray(it.readText()) }
        return (0 until items.length()).map { i ->
            val o=items.getJSONObject(i)
            // First five mirror the reference's central constellation. The rest span two years.
            val days=if(i<5) listOf(0,28,85,160,320)[i] else (i*37)%730
            Bookmark("demo-$i",o.getString("url"),o.getString("title"),
                note="Link demo tích hợp trong APK. Ngày thêm và trạng thái là minh họa; không đồng bộ vào tài khoản.",
                category=o.getString("category"),slot=i,
                health=when { i%17==0 && i!=0 -> Health.UNAVAILABLE; i%11==0 && i!=0 -> Health.UNCERTAIN; i%23==0 && i!=0 -> Health.UNCHECKED;else -> Health.REACHABLE },
                reason="Trạng thái minh họa của bộ 100 link demo, không phải kết quả HTTP.",
                createdAt=now-days*86_400_000L)
        }
    }
}
