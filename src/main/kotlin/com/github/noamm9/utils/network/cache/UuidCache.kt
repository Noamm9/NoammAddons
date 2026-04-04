package com.github.noamm9.utils.network.cache

import java.util.concurrent.ConcurrentHashMap

object UuidCache {
    private val uuidByName = ConcurrentHashMap<String, String>()
    private val nameByUuid = ConcurrentHashMap<String, String>()

    fun addToCache(name: String, uuid: String): String? {
        val normalizedName = name.lowercase()
        val normalizedUuid = uuid.replace("-", "")

        if (normalizedUuid != "null" && normalizedUuid != "FAILED") {
            nameByUuid[normalizedUuid] = name
        }

        return uuidByName.put(normalizedName, normalizedUuid)
    }

    fun getFromCache(name: String) = uuidByName[name.lowercase()]

    fun getNameFromCache(uuid: String) = nameByUuid[uuid.replace("-", "")]
}