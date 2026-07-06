package com.github.noamm9.utils.network.abstracts

import com.github.noamm9.config.PogObject
import java.util.concurrent.*

interface AbstractCache<T> {
    var storage: PogObject<ConcurrentHashMap<String, CachedEntry<T>>>
    val EXPIRE_TIME: Long

    /**
     * Cache a successful lookup.
     */
    fun addToCache(key: String, data: T): Any?

    /**
     * Cache a failed lookup to prevent continuous API calls (Negative Caching).
     */
    fun addFailedToCache(key: String): Any?

    /**
     * Retrieve an entry from the cache
     */
    fun get(key: String): CacheResult<T>

    /**
     * Check whether a value is already cached. and returns that value if cached or null
     */
    fun check(key: String, message: String = "$key not found (cached)"): Result<T>? {
        return when (val cached = get(key)) {
            is CacheResult.Failed -> Result.failure(Exception(message))
            is CacheResult.Success -> Result.success(cached.data)
            else -> null
        }
    }

    fun isExpired(entry: CachedEntry<*>) = System.currentTimeMillis() - entry.timestamp > EXPIRE_TIME
    fun cleanupExpired() = if (storage.get().values.removeIf(::isExpired)) storage.save() else Any()
}