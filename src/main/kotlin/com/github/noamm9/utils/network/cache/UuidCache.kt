package com.github.noamm9.utils.network.cache

import com.github.noamm9.config.PogObject
import java.util.concurrent.TimeUnit

object UuidCache {
    private data class CacheData(
        val uuidByName: MutableMap<String, CachedEntry> = mutableMapOf(),
        val nameByUuid: MutableMap<String, CachedEntry> = mutableMapOf()
    )

    private val storage = PogObject("uuid_cache", CacheData())
    private val EXPIRE_TIME = TimeUnit.HOURS.toMillis(24)

    fun addToCache(name: String, uuid: String) {
        val lowerName = name.lowercase()
        val cleanUuid = uuid.replace("-", "")
        if (cleanUuid == "FAILED") return

        val entry = CachedEntry(name, cleanUuid, System.currentTimeMillis())
        val data = storage.getData()

        data.uuidByName[lowerName] = entry
        data.nameByUuid[cleanUuid] = entry
    }

    fun getFromCache(name: String): String? {
        val lowerName = name.lowercase()
        val data = storage.getData()
        val entry = data.uuidByName[lowerName] ?: return null

        if (entry.isExpired()) {
            data.uuidByName.remove(lowerName)
            data.nameByUuid.remove(entry.uuid)
            return null
        }

        return entry.uuid
    }

    fun getNameFromCache(uuid: String): String? {
        val cleanUuid = uuid.replace("-", "")
        val data = storage.getData()
        val entry = data.nameByUuid[cleanUuid] ?: return null

        if (entry.isExpired()) {
            data.nameByUuid.remove(cleanUuid)
            data.uuidByName.remove(entry.name.lowercase())
            return null
        }

        return entry.name
    }

    private fun CachedEntry.isExpired() = System.currentTimeMillis() - timestamp > EXPIRE_TIME
    private data class CachedEntry(val name: String, val uuid: String, val timestamp: Long)
}