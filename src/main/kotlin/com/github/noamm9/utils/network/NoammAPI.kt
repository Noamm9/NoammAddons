package com.github.noamm9.utils.network

import com.github.noamm9.utils.network.data.*
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.io.IOException

object NoammAPI {
    const val BASE_URL = "https://api.noamm.org"

    suspend fun getDungeonStats(uuid: String) = apiRequest<DungeonStats>("/hypixel/dungeonstats/$uuid")
    suspend fun getSecrets(uuid: String) = apiRequest<Long>("/hypixel/secrets/$uuid")
    suspend fun getRtca(name: String) = apiRequest<RtcaData>("/hypixel/rtca/$name")
    suspend fun getStorage(uuid: String) = apiRequest<StorageData>("/hypixel/storage/$uuid")

    private suspend inline fun <reified T> apiRequest(path: String): Result<T> {
        val result = WebUtils.get("$BASE_URL$path") { ApiAuth.token?.let { header("Authorization", "Bearer $it") } }
        if (result.isFailure) return Result.failure(result.exceptionOrNull() !!)

        val res = result.getOrThrow()
        if (! res.status.isSuccess()) {
            val exception = when (res.status) {
                HttpStatusCode.TooManyRequests -> NoammAPIException.RateLimited()
                HttpStatusCode.BadGateway -> NoammAPIException.ApiUnavailable()
                else -> IOException("HTTP ${res.status.value}: ${res.bodyAsText()}")
            }

            return Result.failure(exception)
        }

        return runCatching { res.body() }
    }

    sealed class NoammAPIException(message: String): Exception(message) {
        class RateLimited: NoammAPIException("Hypixel API rate limit reached, try again later")
        class ApiUnavailable(message: String = "API is currently unavailable"): NoammAPIException(message)
    }
}