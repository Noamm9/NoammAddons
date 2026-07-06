package com.github.noamm9.utils.network.cache

import com.github.noamm9.config.PogObject
import com.github.noamm9.utils.network.abstracts.AbstractCache
import com.github.noamm9.utils.network.abstracts.CacheResult
import com.github.noamm9.utils.network.abstracts.CachedEntry
import com.github.noamm9.utils.network.data.DungeonStats
import java.util.concurrent.*
import kotlin.collections.set

object ProfileCache: AbstractCache<DungeonStats> {
    override var storage = PogObject("profile_cache", ConcurrentHashMap<String, CachedEntry<DungeonStats>>())
    override val EXPIRE_TIME = TimeUnit.HOURS.toMillis(1)

    override fun addToCache(key: String, data: DungeonStats) = storage.get().set(key.lowercase(), CachedEntry(data))
    override fun addFailedToCache(key: String) = storage.get().set(key.lowercase(), CachedEntry(null))

    override fun get(key: String): CacheResult<DungeonStats> {
        val entry = storage.get()[key.lowercase()] ?: return CacheResult.NotFound()
        if (isExpired(entry)) {
            storage.get().remove(key)
            return CacheResult.NotFound()
        }

        return if (entry.value == null) CacheResult.Failed() else CacheResult.Success(entry.value)
    }
}