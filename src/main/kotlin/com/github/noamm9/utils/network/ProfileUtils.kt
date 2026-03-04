package com.github.noamm9.utils.network

import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.network.cache.UuidCache
import com.github.noamm9.utils.network.data.MojangData
import kotlinx.serialization.json.JsonObject

object ProfileUtils {
    suspend fun getUUIDbyName(name: String): Result<MojangData> {
        UuidCache.getFromCache(name)?.let { return Result.success(MojangData(name, it)) }
        return WebUtils.get<MojangData>("https://api.minecraftservices.com/minecraft/profile/lookup/name/$name")
            .onSuccess { UuidCache.addToCache(name, it.uuid) }
    }

    suspend fun getNameByUUID(uuid: String): Result<MojangData> {
        val cleanUuid = uuid.replace("-", "")
        UuidCache.getNameFromCache(cleanUuid)?.let { return Result.success(MojangData(it, cleanUuid)) }
        return WebUtils.get<MojangData>("https://sessionserver.mojang.com/session/minecraft/profile/$cleanUuid")
            .onSuccess { UuidCache.addToCache(it.name, it.uuid) }
    }

    suspend fun getSecrets(playerName: String): Result<Long> {
        return getUUIDbyName(playerName).mapCatching { mojangData ->
            WebUtils.get<Long>("https://api.noamm.org/secrets?uuid=${mojangData.uuid}").getOrThrow()
        }
    }

    suspend fun getProfile(playerName: String): Result<JsonObject> {
        ProfileCache.getFromCache(playerName)?.let { return Result.success(it) }
        return getUUIDbyName(playerName).mapCatching { mojangData ->
            WebUtils.get<JsonObject>("https://api.noamm.org/dungeonstats?uuid=${mojangData.uuid}").getOrThrow()
        }.onSuccess { ProfileCache.addToCache(playerName, it) }
    }
}