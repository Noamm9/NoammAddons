package com.github.noamm9.utils.network.cache

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

object SecretCache {
    private val cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build<String, Long>()

    fun addToCache(name: String, secrets: Long) = cache.put(name.lowercase(), secrets)
    fun getFromCache(name: String) = cache.getIfPresent(name.lowercase())
}