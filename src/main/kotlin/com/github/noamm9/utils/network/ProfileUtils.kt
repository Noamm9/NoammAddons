package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.network.cache.*
import com.github.noamm9.utils.network.data.DungeonStats
import com.github.noamm9.utils.network.data.MojangData
import com.github.noamm9.websocket.WebSocket
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.io.IOException
import java.util.*

object ProfileUtils {
    private const val mowojang = "https://mowojang.matdoes.dev/"

    suspend fun getUUIDbyName(name: String): Result<MojangData> {
        val key = name.lowercase()
        MojangCache.check(key)?.let { return it }

        val result = WebUtils.get(mowojang + key).mapCatching { response ->
            if (! response.status.isSuccess() || response.status.value == 204) throw IOException("$name not found")
            val mojangData = response.body<MojangData>()
            MojangCache.addToCache(mojangData)
            return@mapCatching mojangData
        }

        if (result.isFailure) MojangCache.addFailedToCache(key)
        return result
    }

    suspend fun getMojangBatched(uuids: Collection<UUID>): Result<List<MojangData>> = runCatching {
        val cached = uuids.map { it to MojangCache.getOrNull(it.toString()) }

        val (hits, misses) = cached.partition { it.second != null }
        val data = hits.mapNotNull { it.second }
        if (misses.isEmpty()) return Result.success(data)

        val missingData = run {
            val response = WebUtils.client.post(mowojang) {
                header(HttpHeaders.ContentType, "application/json")
                setBody(buildJsonArray { misses.forEach { add(it.first.toString()) } })
            }

            if (! response.status.isSuccess() || response.status.value == 204) return@run null
            val result = response.body<List<MojangData>>()
            for (r in result) MojangCache.addToCache(r)
            return@run result
        }.orEmpty()

        return Result.success(data + missingData)
    }

    suspend fun getSecrets(playerName: String): Result<Long> {
        val name = playerName.lowercase()
        if (name == mc.user.name.lowercase() && DungeonListener.thePlayer?.isDead == false) {
            return runCatching { getSecretsCMD() }
        }

        SecretCache.check(name)?.let { return it }

        return getUUIDbyName(name).mapCatching { mojangData ->
            NoammAPI.getSecrets(mojangData.uuid).getOrThrow()
        }.apply {
            onSuccess { SecretCache.addToCache(name, it) }
            onFailure { error ->
                if (error is NoammAPI.NoammAPIException) return@onFailure
                SecretCache.addFailedToCache(name)
            }
        }
    }

    suspend fun getProfile(playerName: String): Result<DungeonStats> {
        val name = playerName.lowercase()
        ProfileCache.check(name)?.let { return it }

        return getUUIDbyName(name).mapCatching { mojangData ->
            NoammAPI.getDungeonStats(mojangData.uuid).getOrThrow()
        }.apply {
            onSuccess { ProfileCache.addToCache(name, it) }
            onFailure { error ->
                if (error is NoammAPI.NoammAPIException) return@onFailure
                ProfileCache.addFailedToCache(name)
            }
        }
    }

    // usually I don't like running commands in the background
    // but this one seems to behave exactly like /locraw.
    // meaning it does not affect the message spam cooldown
    private suspend fun getSecretsCMD(): Long {
        if (! LocationUtils.inSkyblock) error("Not in Skyblock")
        _totalSecrets = null
        chatListener.register()
        ChatUtils.sendCommand("/secretcount")
        ThreadUtils.setTimeout(5000) { chatListener.unregister() }
        while (chatListener.isActive) delay(50)
        return _totalSecrets ?: error("No secrets found")
    }

    private val regex = Regex("^\\w+: \\d+$")
    private var _totalSecrets: Long? = null
    private val chatListener = EventBus.listener<ChatMessageEvent> {
        if (event.unformattedText == "Secret Counts:") return@listener event.cancel()
        if (! event.unformattedText.matches(regex)) return@listener
        event.isCanceled = true

        if (event.unformattedText.substringBefore(":") != mc.user.name) return@listener
        _totalSecrets = event.unformattedText.substringAfter(": ").toLongOrNull()
        WebSocket.send(mapOf("type" to "secretcount", "secrets" to _totalSecrets))
        ThreadUtils.scheduledTaskServer(5) { listener.unregister() }
    }
}