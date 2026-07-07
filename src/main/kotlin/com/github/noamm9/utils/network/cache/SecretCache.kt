package com.github.noamm9.utils.network.cache

import com.github.noamm9.utils.network.abstracts.CacheResult
import com.github.noamm9.utils.network.abstracts.CachedEntry
import com.github.noamm9.utils.network.abstracts.NetworkCache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.*

object SecretCache: NetworkCache<String, Long> {
    private val entries = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build<String, CachedEntry<Long>>()

    override fun get(key: String): CacheResult<Long> {
        val entry = entries.getIfPresent(key.lowercase()) ?: return CacheResult.NotFound
        return if (entry.value == null) CacheResult.Failed else CacheResult.Success(entry.value)
    }

    override fun addToCache(key: String, value: Long) = entries.put(key.lowercase(), CachedEntry(value))
    override fun addFailedToCache(key: String) = entries.put(key.lowercase(), CachedEntry(null))
}