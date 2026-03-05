package com.github.noamm9.utils.network.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.TimeUnit

object ProfileCache {
    private val cache: Cache<String, JsonObject> = CacheBuilder.newBuilder()
        .maximumSize(100).expireAfterAccess(5, TimeUnit.MINUTES).build()

    fun addToCache(name: String, profile: JsonObject) = cache.put(name.lowercase(), profile)
    fun getFromCache(name: String) = cache.getIfPresent(name.lowercase())
}