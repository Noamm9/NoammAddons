package com.github.noamm9.utils.network.cache

import java.util.concurrent.ConcurrentHashMap

object UuidCache {
    private val cache = ConcurrentHashMap<String, String>()

    fun addToCache(name: String, uuid: String) = cache.put(name.lowercase(), uuid.replace("-", ""))
    fun getFromCache(name: String) = cache[name.lowercase()]
    fun getNameFromCache(uuid: String) = uuid.replace("-", "").let { id -> cache.entries.find { it.value == id } }?.key
}