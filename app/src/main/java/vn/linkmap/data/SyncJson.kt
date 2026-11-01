package vn.linkmap.data

import org.json.JSONObject
import vn.linkmap.core.*

object SyncJson {
    fun encode(record: SyncRecord): JSONObject = JSONObject().apply {
        val b = record.bookmark
        put("kind",b.kind.name);put("id", b.id); put("url", b.url); put("title", b.title); put("note", b.note); put("category", b.category)
        put("createdAt", b.createdAt); put("counter", record.revision.counter); put("device", record.revision.device)
        put("deleted", record.deleted); put("healthDevice", record.healthDevice)
        put("health", b.health.name); put("reason", b.reason); put("checkedAt", b.checkedAt)
        put("httpCode", b.httpCode ?: JSONObject.NULL); put("notFoundAt", b.notFoundAt)
    }
    fun decode(o: JSONObject): SyncRecord {
        val kind=EntryKind.valueOf(o.optString("kind","LINK"))
        val url = if(kind==EntryKind.NOTE) o.optString("url").also { require(it.isEmpty()) } else requireNotNull(Urls.normalize(o.getString("url"))) { "Sync URL không hợp lệ" }
        val id = o.getString("id")
        require(id.matches(if(kind==EntryKind.NOTE) Regex("note-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}") else Regex("url-[a-f0-9]{64}"))) { "Sync ID không hợp lệ" }
        val body=o.optString("note");require(body.length<=IncomingContent.MAX_LENGTH && (kind!=EntryKind.NOTE || body.isNotBlank()))
        val counter = o.getLong("counter"); require(counter in 0..9007199254740000L)
        val device = o.getString("device"); require(device.length in 1..128)
        val checkedAt = o.optLong("checkedAt"); require(checkedAt >= 0)
        return SyncRecord(Bookmark(id, url, o.optString("title").take(200), body,
            o.optString("category", "Cá nhân").take(50), health = runCatching { Health.valueOf(o.optString("health")) }.getOrDefault(Health.UNCHECKED),
            reason = o.optString("reason").take(500), checkedAt = checkedAt, httpCode = if (o.isNull("httpCode")) null else o.getInt("httpCode"),
            notFoundAt = o.optLong("notFoundAt"), createdAt = o.getLong("createdAt"),kind=kind), Revision(counter, device), o.optBoolean("deleted"), o.optString("healthDevice"))
    }
}
