package vn.linkmap.core

import java.security.MessageDigest

data class Revision(val counter: Long, val device: String) : Comparable<Revision> {
    override fun compareTo(other: Revision): Int = counter.compareTo(other.counter).takeIf { it != 0 } ?: device.compareTo(other.device)
}
data class SyncRecord(val bookmark: Bookmark, val revision: Revision, val deleted: Boolean = false, val healthDevice: String = "")

object SyncRules {
    fun id(url: String): String = "url-" + MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun merge(a: SyncRecord?, b: SyncRecord): SyncRecord {
        if (a == null) return b
        require(a.bookmark.id == b.bookmark.id)
        val winner = if (a.revision >= b.revision) a else b
        // Health is an independent observation and can never restore content or overwrite an edit.
        if(winner.bookmark.isNote) return winner.copy(healthDevice="")
        val health = listOf(a, b).filter { !it.bookmark.isNote && it.bookmark.url == winner.bookmark.url }
            .maxWith(compareBy<SyncRecord> { it.bookmark.checkedAt }.thenBy { it.healthDevice }.thenBy { it.revision })
        return winner.copy(bookmark = winner.bookmark.copy(
            health = health.bookmark.health, reason = health.bookmark.reason, checkedAt = health.bookmark.checkedAt,
            httpCode = health.bookmark.httpCode, notFoundAt = health.bookmark.notFoundAt
        ), healthDevice = health.healthDevice)
    }
}
