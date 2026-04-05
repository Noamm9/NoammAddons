package com.github.noamm9.utils.network.cache

import java.util.concurrent.ConcurrentHashMap

object UuidCache {
    private val uuidByName = ConcurrentHashMap<String, String>()
    private val nameByUuid = ConcurrentHashMap<String, String>()

    fun addToCache(name: String, uuid: String) {
        val lowerName = name.lowercase()
        val cleanUuid = uuid.replace("-", "")

        if (cleanUuid != "FAILED") nameByUuid[cleanUuid] = name
        uuidByName[lowerName] = cleanUuid
    }

    fun getFromCache(name: String): String? = uuidByName[name.lowercase()]
    fun getNameFromCache(uuid: String): String? = nameByUuid[uuid.replace("-", "")]
}