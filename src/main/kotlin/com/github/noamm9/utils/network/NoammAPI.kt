package com.github.noamm9.utils.network

import com.github.noamm9.utils.network.data.*
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

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
        if (! res.status.isSuccess()) return Result.failure(Exception("HTTP ${res.status.value}: ${res.bodyAsText()}"))

        return runCatching { res.body() }
    }
}