package com.github.noamm9.utils.network.cache

import com.github.noamm9.config.PogObject
import com.github.noamm9.utils.network.abstracts.CacheResult
import com.github.noamm9.utils.network.abstracts.CachedEntry
import com.github.noamm9.utils.network.abstracts.NetworkCache
import com.github.noamm9.utils.network.data.DungeonStats
import java.util.concurrent.*

object ProfileCache: NetworkCache<String, DungeonStats> {
    private val storage = PogObject("profile_cache", ConcurrentHashMap<String, CachedEntry<DungeonStats>>())
    private val EXPIRE_TIME = TimeUnit.HOURS.toMillis(1)

    override fun addToCache(key: String, value: DungeonStats) = storage.get().set(key.lowercase(), CachedEntry(value))
    override fun addFailedToCache(key: String) = storage.get().set(key.lowercase(), CachedEntry(null))

    override fun get(key: String): CacheResult<DungeonStats> {
        val entry = storage.get()[key.lowercase()] ?: return CacheResult.NotFound

        if (System.currentTimeMillis() - entry.timestamp > EXPIRE_TIME) {
            storage.get().remove(key)
            return CacheResult.NotFound
        }

        return if (entry.value == null) CacheResult.Failed else CacheResult.Success(entry.value)
    }
}