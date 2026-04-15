package com.github.noamm9.utils.network.cache

import com.google.common.cache.CacheBuilder
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.TimeUnit

object ProfileCache {
    private val cache = CacheBuilder.newBuilder().expireAfterAccess(120, TimeUnit.MINUTES).build<String, JsonObject>()

    fun addToCache(name: String, profile: JsonObject) = cache.put(name.lowercase(), profile)
    fun getFromCache(name: String): JsonObject? = cache.getIfPresent(name.lowercase())
}