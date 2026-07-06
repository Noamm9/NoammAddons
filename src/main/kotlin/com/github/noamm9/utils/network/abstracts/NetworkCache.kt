package com.github.noamm9.utils.network.abstracts

interface NetworkCache<K, V> {
    fun get(key: K): CacheResult<V>
    fun addToCache(key: K, value: V)
    fun addFailedToCache(key: K)

    fun check(key: K, failMessage: String = "$key not found (cached)"): Result<V>? {
        return when (val cached = get(key)) {
            is CacheResult.Failed -> Result.failure(Exception(failMessage))
            is CacheResult.Success -> Result.success(cached.data)
            CacheResult.NotFound -> null
        }
    }
}