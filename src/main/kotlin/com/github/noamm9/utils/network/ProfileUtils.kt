package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.JsonUtils.getString
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.network.cache.SecretCache
import com.github.noamm9.utils.network.cache.UuidCache
import com.github.noamm9.utils.network.data.MojangData
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object ProfileUtils {
    const val BASE_URL = "https://api.noamm.org"

    private val uuidApis = listOf(
        "https://playerdb.co/api/player/minecraft/",
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
        "https://api.mojang.com/users/profiles/minecraft/",
    )

    private val sharedRequests = ConcurrentHashMap<String, Deferred<*>>()
    private val apiCooldowns = ConcurrentHashMap<String, Long>()

    suspend fun getUUIDbyName(name: String): Result<MojangData> {
        UuidCache.getFromCache(name)?.let {
            if (it == "null" || it == "FAILED") return Result.failure(Exception("$name not found or pending"))
            return Result.success(MojangData(name, it))
        }

        UuidCache.addToCache(name, "null")

        for ((i, api) in uuidApis.withIndex()) {
            if (System.currentTimeMillis() < (apiCooldowns[api] ?: 0L)) continue

            val result = WebUtils.getString(api + name)

            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: ""

                if (errorMsg.contains("429")) {
                    apiCooldowns[api] = System.currentTimeMillis() + (5 * 60 * 1000)
                    logger.warn("API Rate Limited (429): $api - Switching to fallback.")
                    continue
                }
                else if (errorMsg.contains("404") || errorMsg.contains("204")) break
                else continue
            }

            val responseString = result.getOrNull() ?: continue
            val response = runCatching { JsonUtils.stringToJson(responseString).jsonObject }.getOrNull() ?: continue

            val uuid = if (i == 0) response.getObj("player")?.getString("id") else response.getString("id")
            val fetchedName = if (i == 0) response.getObj("player")?.getString("username") else response.getString("name")

            if (uuid.isNullOrBlank() || fetchedName.isNullOrBlank()) continue

            UuidCache.addToCache(fetchedName, uuid)
            return Result.success(MojangData(fetchedName, uuid))
        }

        UuidCache.addToCache(name, "FAILED")
        return Result.failure(Exception("$name not found or APIs unavailable"))
    }

    suspend fun getNameByUUID(uuid: String): Result<MojangData> {
        val cached = UuidCache.getNameFromCache(uuid)
        if (cached == "FAILED") return Result.failure(Exception("UUID not found (Cached)"))
        if (cached != null) return Result.success(MojangData(cached, uuid))

        return WebUtils.getAs<MojangData>("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
            .onSuccess { UuidCache.addToCache(it.name, it.uuid) }
    }

    suspend fun getSecrets(playerName: String): Result<Long> {
        val name = playerName.uppercase()
        return awaitSharedRequest("SECRETS", name) {
            SecretCache.getFromCache(name)?.let { return@awaitSharedRequest Result.success(it) }
            getUUIDbyName(name).mapCatching { mojangData ->
                doApiRequest<Long>("/secrets?uuid=${mojangData.uuid}")
            }.onSuccess { SecretCache.addToCache(name, it) }
        }
    }

    suspend fun getProfile(playerName: String): Result<JsonObject> {
        val name = playerName.uppercase()
        return awaitSharedRequest("PROFILE", name) {
            ProfileCache.getFromCache(name)?.let { return@awaitSharedRequest Result.success(it) }
            getUUIDbyName(name).mapCatching { mojangData ->
                doApiRequest<JsonObject>("/dungeonstats?uuid=${mojangData.uuid}")
            }.onSuccess { ProfileCache.addToCache(name, it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> awaitSharedRequest(type: String, name: String, request: suspend () -> Result<T>): Result<T> {
        val key = "$type:${name.uppercase()}"

        (sharedRequests[key] as? Deferred<Result<T>>)?.let { return it.await() }

        val deferred = scope.async(start = CoroutineStart.LAZY) { request() }
        deferred.invokeOnCompletion { sharedRequests.remove(key, deferred) }

        val active = sharedRequests.putIfAbsent(key, deferred) as? Deferred<Result<T>> ?: deferred
        if (active === deferred) deferred.start()

        return active.await()
    }

    private suspend inline fun <reified T> doApiRequest(path: String): T {
        val now = System.currentTimeMillis()

        if (now < (apiCooldowns["noamm"] ?: 0L)) throw IllegalStateException("API is currently rate limited globally.")
        if (now < (apiCooldowns[path] ?: 0L)) throw IllegalStateException("Resource is negative cached.")

        val resResult = WebUtils.get("$BASE_URL$path")

        if (resResult.isFailure) {
            val exception = resResult.exceptionOrNull()
            val msg = exception?.message ?: ""

            when {
                msg.contains("429") -> {
                    apiCooldowns["noamm"] = now + 60 * 1000
                    logger.warn("Received 429 from Noamm API. Entering global cooldown for 1m.")
                }

                msg.contains("404") || msg.contains("500") || msg.contains("502") || msg.contains("503") -> {
                    apiCooldowns[path] = now + (5 * 60 * 1000)
                    logger.warn("API Error ($msg) for path $path. Negative caching for 5m.")
                }
            }
            throw exception ?: Exception("Unknown API error")
        }

        val res = resResult.getOrThrow()

        res.headers["x-ratelimit-remaining"]?.let { remaining ->
            if (remaining != "0") return@let
            val reset = res.headers["x-ratelimit-reset"]?.toLongOrNull() ?: 10L
            apiCooldowns["noamm"] = max(apiCooldowns["noamm"] ?: 0L, now + (reset * 1000L))
        }

        return WebUtils.getAs<T>(res).getOrThrow()
    }
}