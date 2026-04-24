package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.JsonUtils.getString
import com.github.noamm9.utils.containsOneOf
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.network.cache.SecretCache
import com.github.noamm9.utils.network.cache.UuidCache
import com.github.noamm9.utils.network.data.DungeonStats
import com.github.noamm9.utils.network.data.MojangData
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.jsonObject
import java.util.*
import java.util.concurrent.*

object ProfileUtils {
    private const val BASE_URL = "https://api.noamm.org"

    private val nameToUuidApis = listOf(
        "https://playerdb.co/api/player/minecraft/",
        "https://mowojang.matdoes.dev/",
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
        "https://api.mojang.com/users/profiles/minecraft/",
        "https://mc-api.io/uuid/",
    )

    val uuidToNameApis = listOf(
        "https://playerdb.co/api/player/minecraft/",
        "https://mowojang.matdoes.dev/",
        "https://sessionserver.mojang.com/session/minecraft/profile/",
        "https://mc-api.io/name/",
    )

    private val sharedRequests = ConcurrentHashMap<String, Deferred<*>>()
    private val apiCooldowns = ConcurrentHashMap<String, Long>()

    suspend fun getUUIDbyName(name: String): Result<MojangData> {
        val lowerName = name.lowercase()

        UuidCache.getFromCache(lowerName)?.let {
            if (it == "FAILED") return Result.failure(Exception("$name not found (cached)"))
            return Result.success(MojangData(name, it))
        }

        return awaitSharedRequest("UUID", lowerName) {
            UuidCache.getFromCache(lowerName)?.let {
                if (it == "FAILED") return@awaitSharedRequest Result.failure(Exception("$name not found"))
                return@awaitSharedRequest Result.success(MojangData(name, it))
            }

            for ((i, api) in nameToUuidApis.withIndex()) {
                if (System.currentTimeMillis() < (apiCooldowns[api] ?: 0L)) continue

                val result = WebUtils.getString(api + lowerName)
                if (result.isFailure) {
                    val msg = result.exceptionOrNull()?.message ?: ""
                    if (msg.contains("429")) {
                        apiCooldowns[api] = System.currentTimeMillis() + (5 * 60 * 1000)
                        continue
                    }
                    if (msg.containsOneOf("404", "204")) break
                    continue
                }

                val response = runCatching { JsonUtils.stringToJson(result.getOrThrow()).jsonObject }.getOrNull() ?: continue
                val uuid = if (i == 0) response.getObj("player")?.getString("id") else response.getString("id")
                val fetchedName = if (i == 0) response.getObj("player")?.getString("username") else response.getString("name") ?: name

                if (uuid.isNullOrBlank() || fetchedName.isNullOrBlank()) continue

                val cleanUuid = uuid.replace("-", "")
                UuidCache.addToCache(fetchedName, cleanUuid)
                return@awaitSharedRequest Result.success(MojangData(fetchedName, cleanUuid))
            }

            Result.failure<MojangData>(Exception("$name not found")).also { UuidCache.addToCache(lowerName, "FAILED") }
        }
    }

    suspend fun getNameByUUID(uuid: UUID): Result<MojangData> {
        val key = uuid.toString().replace("-", "")

        UuidCache.getNameFromCache(key)?.let {
            if (it == "FAILED") return Result.failure(Exception("UUID not found"))
            return Result.success(MojangData(it, key))
        }

        return awaitSharedRequest("NAME", key) {
            UuidCache.getNameFromCache(key)?.let {
                if (it == "FAILED") return@awaitSharedRequest Result.failure(Exception("$key not found"))
                return@awaitSharedRequest Result.success(MojangData(it, key))
            }

            for ((i, api) in uuidToNameApis.withIndex()) {
                if (System.currentTimeMillis() < (apiCooldowns[api] ?: 0L)) continue

                val result = WebUtils.getString(api + key)
                if (result.isFailure) {
                    val msg = result.exceptionOrNull()?.message ?: ""
                    if (msg.contains("429")) {
                        apiCooldowns[api] = System.currentTimeMillis() + (5 * 60 * 1000)
                        continue
                    }
                    if (msg.containsOneOf("404", "204")) break
                    continue
                }

                val response = runCatching { JsonUtils.stringToJson(result.getOrThrow()).jsonObject }.getOrNull() ?: continue
                val uuid = if (i == 0) response.getObj("player")?.getString("id") else response.getString("id") ?: key
                val fetchedName = if (i == 0) response.getObj("player")?.getString("username") else response.getString("name")

                if (uuid.isNullOrBlank() || fetchedName.isNullOrBlank()) continue

                UuidCache.addToCache(fetchedName, uuid)
                return@awaitSharedRequest Result.success(MojangData(fetchedName, uuid))
            }

            Result.failure<MojangData>(Exception("$key not found")).also { UuidCache.addToCache("FAILED", key) }
        }
    }

    suspend fun getSecrets(playerName: String): Result<Long> {
        val name = playerName.lowercase()
        SecretCache.getFromCache(name)?.let { return Result.success(it) }

        return awaitSharedRequest("SECRETS", name) {
            SecretCache.getFromCache(name)?.let { return@awaitSharedRequest Result.success(it) }
            getUUIDbyName(name).mapCatching { mojangData ->
                doApiRequest<Long>("/hypixel/secrets/${mojangData.uuid}")
            }.onSuccess { SecretCache.addToCache(name, it) }
        }
    }

    suspend fun getProfile(playerName: String): Result<DungeonStats> {
        val name = playerName.lowercase()
        ProfileCache.getFromCache(name)?.let { return Result.success(it) }

        return awaitSharedRequest("PROFILE", name) {
            ProfileCache.getFromCache(name)?.let { return@awaitSharedRequest Result.success(it) }
            getUUIDbyName(name).mapCatching { mojangData ->
                doApiRequest<DungeonStats>("/hypixel/dungeonstats/${mojangData.uuid}")
            }.onSuccess { ProfileCache.addToCache(name, it) }.onFailure { it.printStackTrace() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> awaitSharedRequest(type: String, name: String, request: suspend () -> Result<T>): Result<T> {
        val key = "$type:${name.lowercase()}"

        while (true) {
            sharedRequests[key]?.let { return it.await() as Result<T> }
            val deferred = scope.async(start = CoroutineStart.LAZY) { request() }
            sharedRequests.putIfAbsent(key, deferred) ?: run {
                deferred.invokeOnCompletion { sharedRequests.remove(key, deferred) }
                deferred.start()
                return deferred.await()
            }
        }
    }

    private suspend inline fun <reified T> doApiRequest(path: String): T {
        val now = System.currentTimeMillis()
        if (now < (apiCooldowns["noamm"] ?: 0L)) throw IllegalStateException("API global cooldown")
        if (now < (apiCooldowns[path] ?: 0L)) throw IllegalStateException("Path negative cached")

        val resResult = WebUtils.get("$BASE_URL$path")
        if (resResult.isFailure) {
            val error = resResult.exceptionOrNull()
            val msg = error?.message ?: ""
            when {
                msg.contains("429") -> apiCooldowns["noamm"] = now + 60_000
                msg.containsOneOf("404", "500", "502", "503", "403") -> apiCooldowns[path] = now + 300_000
            }

            throw error ?: Exception("API Error")
        }

        return WebUtils.getAs<T>(resResult.getOrThrow()).getOrThrow()
    }
}