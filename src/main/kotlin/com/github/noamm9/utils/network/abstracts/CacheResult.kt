package com.github.noamm9.utils.network.abstracts

sealed interface CacheResult<out T> {
    data class Success<T>(val data: T): CacheResult<T>
    object Failed: CacheResult<Nothing>
    object NotFound: CacheResult<Nothing>
}