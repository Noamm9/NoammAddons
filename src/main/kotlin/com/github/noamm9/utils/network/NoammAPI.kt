package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.network.data.*
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.io.IOException

object NoammAPI {
    const val URL = "https://api.noamm.org"
    @Volatile private var sessionExpired = false
    @Volatile private var cooldownUntil = 0L

    suspend fun getDungeonStats(uuid: String) = apiRequest<DungeonStats>("/hypixel/dungeonstats/$uuid")
    suspend fun getSecrets(uuid: String) = apiRequest<Long>("/hypixel/secrets/$uuid")
    suspend fun getRtca(name: String) = apiRequest<RtcaData>("/hypixel/rtca/$name")
    suspend fun getStorage(uuid: String) = apiRequest<StorageData>("/hypixel/storage/$uuid")

    private suspend inline fun <reified T> apiRequest(path: String): Result<T> = run {
        if (System.currentTimeMillis() < cooldownUntil) return Result.failure(NoammAPIException.OnCooldown())
        if (ApiAuth.token == null) {
            if (ApiAuth.keyPairUnavailable && ! sessionExpired) {
                ThreadUtils.scheduledTask(6000) { sessionExpired = false }
                ThreadUtils.scheduledTask(10) {
                    ChatUtils.modMessage("§cFailed to authenticate with Mojang, your session is probably expired. §fRestart your game to fix it.")
                }
                sessionExpired = true
            }
            return Result.failure(NoammAPIException.Unauthorized("auth token is null. your session is probably expired. Restart your game to fix it."))
        }

        val res = WebUtils.get("$URL$path") { header("Authorization", "Bearer ${ApiAuth.token}") }
            .getOrElse { return@run Result.failure(NoammAPIException.ApiUnavailable("API is unreachable: ${it.message}")) }

        if (! res.status.isSuccess()) {
            val body = catch { res.bodyAsText() }.orEmpty()
            val exception = when {
                res.status == HttpStatusCode.TooManyRequests -> {
                    setCD(60)
                    NoammAPIException.RateLimited()
                }

                res.status.value >= 500 -> {
                    setCD(60)
                    NoammAPIException.ApiUnavailable()
                }

                res.status == HttpStatusCode.Unauthorized -> NoammAPIException.Unauthorized(body)
                res.status == HttpStatusCode.Forbidden -> NoammAPIException.Forbidden(body)
                else -> IOException("HTTP ${res.status.value}: ${body.take(100)}")
            }

            return@run Result.failure(exception)
        }

        return@run runCatching { res.body<T>() }
    }.onFailure {
        log(it.message, it.takeUnless { e -> e is NoammAPIException || e is IOException })
    }

    sealed class NoammAPIException(message: String): Exception(message) {
        class OnCooldown: NoammAPIException("API is on cooldown")
        class RateLimited: NoammAPIException("Hypixel API rate limit reached, try again later")
        class ApiUnavailable(message: String = "API is currently unavailable"): NoammAPIException(message)
        class Unauthorized(message: String): NoammAPIException(message)
        class Forbidden(message: String): NoammAPIException(message)
    }

    private fun setCD(i: Int) {
        cooldownUntil = System.currentTimeMillis() + i * 1000L
        log("api cooldown was set to $i" + "s")
    }

    private fun log(string: String?, e: Throwable? = null) {
        val str = "[${this.javaClass.simpleName}] ${string.orEmpty()}"
        if (e != null) NoammAddons.logger.error(str, e)
        else NoammAddons.logger.info(str)
    }
}