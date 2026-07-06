package com.github.noamm9.utils.network.cache

import com.github.noamm9.utils.network.abstracts.CacheResult
import com.github.noamm9.utils.network.abstracts.CachedEntry
import com.github.noamm9.utils.network.abstracts.NetworkCache
import java.util.concurrent.*

object SecretCache: NetworkCache<String, Long> {
    private val entries = ConcurrentHashMap<String, CachedEntry<Long>>()
    private val EXPIRE_TIME = TimeUnit.MINUTES.toMillis(1)

    override fun get(key: String): CacheResult<Long> {
        val lowerKey = key.lowercase()
        val entry = entries[lowerKey] ?: return CacheResult.NotFound

        if (System.currentTimeMillis() - entry.timestamp > EXPIRE_TIME) {
            entries.remove(lowerKey)
            return CacheResult.NotFound
        }

        return if (entry.value == null) CacheResult.Failed else CacheResult.Success(entry.value)
    }

    override fun addToCache(key: String, value: Long) = entries.set(key.lowercase(), CachedEntry(value))
    override fun addFailedToCache(key: String) = entries.set(key.lowercase(), CachedEntry(null))
}