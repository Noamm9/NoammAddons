package com.github.noamm9.utils.network.cache

import com.github.noamm9.utils.network.abstracts.CacheResult
import com.github.noamm9.utils.network.abstracts.CachedEntry
import com.github.noamm9.utils.network.abstracts.NetworkCache
import com.github.noamm9.utils.network.data.DungeonStats
import com.google.common.cache.CacheBuilder
import java.util.concurrent.*

object ProfileCache: NetworkCache<String, DungeonStats> {
    private val storage = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build<String, CachedEntry<DungeonStats>>()

    override fun addToCache(key: String, value: DungeonStats) = storage.put(key.lowercase(), CachedEntry(value))
    override fun addFailedToCache(key: String) = storage.put(key.lowercase(), CachedEntry(null))

    override fun get(key: String): CacheResult<DungeonStats> {
        val entry = storage.getIfPresent(key.lowercase()) ?: return CacheResult.NotFound
        return if (entry.value == null) CacheResult.Failed else CacheResult.Success(entry.value)
    }
}