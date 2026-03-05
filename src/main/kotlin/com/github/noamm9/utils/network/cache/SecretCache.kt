package com.github.noamm9.utils.network.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

object SecretCache {
    private val cache: Cache<String, Long> = CacheBuilder.newBuilder()
        .maximumSize(100).expireAfterAccess(1, TimeUnit.MINUTES).build()

    fun addToCache(name: String, secrets: Long) = cache.put(name.lowercase(), secrets)
    fun getFromCache(name: String) = cache.getIfPresent(name.lowercase())
}