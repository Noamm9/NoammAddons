package com.github.noamm9.utils.network.cache

import com.github.noamm9.utils.network.data.DungeonStats
import com.google.common.cache.CacheBuilder
import java.util.concurrent.*

object ProfileCache {
    private val cache = CacheBuilder.newBuilder().expireAfterAccess(120, TimeUnit.MINUTES).build<String, DungeonStats>()

    fun addToCache(name: String, profile: DungeonStats) = cache.put(name.lowercase(), profile)
    fun getFromCache(name: String) = cache.getIfPresent(name.lowercase())
}