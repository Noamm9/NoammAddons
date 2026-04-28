package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.utils.JsonUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

object WebUtils {
    val client = HttpClient(OkHttp) {
        install(WebSockets) { pingIntervalMillis = 30_000 }
        install(UserAgent) { agent = "$MOD_NAME/$MOD_VERSION (+https://noamm.org)" }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
        install(ContentNegotiation) {
            json(JsonUtils.json)
        }
        expectSuccess = false
    }

    suspend fun get(url: String) = runCatching { client.get(url) }
    suspend inline fun <reified T> getAs(url: String) = get(url).mapCatching {
        if (it.status.value !in 200 .. 299) error("HTTP ${it.status.value}-${it.status.description}: ${it.bodyAsText()}")
        it.body<T>()
    }

    suspend fun post(url: String, body: Any) = runCatching {
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
}