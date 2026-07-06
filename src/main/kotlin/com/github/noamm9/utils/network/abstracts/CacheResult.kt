package com.github.noamm9.utils.network.abstracts

interface CacheResult<T> {
    data class Success<T>(val data: T): CacheResult<T>
    class Failed<T>: CacheResult<T>
    class NotFound<T>: CacheResult<T>
}