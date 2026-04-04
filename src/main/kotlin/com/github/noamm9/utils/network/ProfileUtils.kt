package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.JsonUtils.getString
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.network.cache.SecretCache
import com.github.noamm9.utils.network.cache.UuidCache
import com.github.noamm9.utils.network.data.MojangData
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

    private val apiCooldowns = ConcurrentHashMap<String, Long>()
    private val failedPaths = ConcurrentHashMap<String, Long>()

    suspend fun getUUIDbyName(name: String): Result<MojangData> {
        val uppercase = name.uppercase()

        UuidCache.getFromCache(uppercase)?.let {
            if (it == "null" || it == "FAILED") return Result.failure(Exception("$name not found or pending"))
            return Result.success(MojangData(name, it))
        }

        UuidCache.addToCache(uppercase, "null")

        for ((i, api) in uuidApis.withIndex()) {
            if (System.currentTimeMillis() < (apiCooldowns[api] ?: 0L)) continue

            val result = WebUtils.getString(api + uppercase)

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

        UuidCache.addToCache(uppercase, "FAILED")
        return Result.failure(Exception("$name not found or APIs unavailable"))
    }

    suspend fun getNameByUUID(uuid: String): Result<MojangData> {
        val cleanUuid = uuid.replace("-", "")

        val cached = UuidCache.getNameFromCache(cleanUuid)
        if (cached == "FAILED") return Result.failure(Exception("UUID not found (Cached)"))
        if (cached != null) return Result.success(MojangData(cached, cleanUuid))

        return WebUtils.getAs<MojangData>("https://sessionserver.mojang.com/session/minecraft/profile/$cleanUuid").apply {
            onSuccess { UuidCache.addToCache(it.name, it.uuid) }
            onFailure { logger.error("Failed to fetch Name for UUID $cleanUuid: ${it.message}") }
        }
    }

    private suspend inline fun <reified T> doApiRequest(path: String): T {
        val now = System.currentTimeMillis()

        if (now < (apiCooldowns["noamm"] ?: 0L)) throw IllegalStateException("API is currently rate limited globally.")
        if (now < (failedPaths[path] ?: 0L)) throw IllegalStateException("Resource is negative cached due to previous errors.")

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
                    failedPaths[path] = now + (5 * 60 * 1000)
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
            logger.warn("API Rate limit hit via headers, reset in $reset seconds.")
        }

        res.headers["x-global-remaining"]?.let { remaining ->
            if (remaining != "0") return@let
            val reset = res.headers["x-global-reset"]?.toLongOrNull() ?: 10L
            apiCooldowns["noamm"] = max(apiCooldowns["noamm"] ?: 0L, now + (reset * 1000L))
            logger.warn("Global Rate limit hit via headers.")
        }

        return WebUtils.getAs<T>(res).getOrThrow()
    }

    suspend fun getSecrets(playerName: String): Result<Long> {
        SecretCache.getFromCache(playerName)?.let { return Result.success(it) }
        return getUUIDbyName(playerName).mapCatching { mojangData ->
            doApiRequest<Long>("/secrets?uuid=${mojangData.uuid}")
        }.onSuccess { SecretCache.addToCache(playerName, it) }
    }

    suspend fun getProfile(playerName: String): Result<JsonObject> {
        ProfileCache.getFromCache(playerName)?.let { return Result.success(it) }
        return getUUIDbyName(playerName).mapCatching { mojangData ->
            doApiRequest<JsonObject>("/dungeonstats?uuid=${mojangData.uuid}")
        }.onSuccess { ProfileCache.addToCache(playerName, it) }
    }
}