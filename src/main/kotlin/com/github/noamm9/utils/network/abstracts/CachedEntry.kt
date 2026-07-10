package com.github.noamm9.utils.network.abstracts

class CachedEntry<T>(val value: T?, val timestamp: Long = System.currentTimeMillis())