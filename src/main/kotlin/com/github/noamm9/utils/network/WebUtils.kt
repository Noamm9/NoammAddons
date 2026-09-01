package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.utils.JsonUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.WebSocketDeflateExtension
import kotlinx.coroutines.*
import java.util.concurrent.*
import java.util.zip.*

object WebUtils {
    private val sharedRequests = ConcurrentHashMap<String, Deferred<Result<HttpResponse>>>()

    val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 10_000

            extensions {
                install(WebSocketDeflateExtension) {
                    compressionLevel = Deflater.BEST_SPEED
                }
            }
        }
        install(UserAgent) { agent = "$MOD_NAME/$MOD_VERSION (+https://noamm.org)" }
        install(HttpTimeout) { connectTimeoutMillis = 10_000 }
        install(ContentNegotiation) { json(JsonUtils.json) }
        install(ContentEncoding) {
            gzip(1.0F)
            deflate(0.9F)
        }
    }

    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}): Result<HttpResponse> {
        while (true) {
            sharedRequests[url]?.let { return it.await() }

            val deferred = scope.async(start = CoroutineStart.LAZY) { runCatching { client.get(url, block) } }
            sharedRequests.putIfAbsent(url, deferred) ?: run {
                deferred.invokeOnCompletion { sharedRequests.remove(url, deferred) }
                deferred.start()
                return deferred.await()
            }
        }
    }

    suspend inline fun <reified T> getAs(url: String) = get(url).mapCatching {
        if (! it.status.isSuccess()) error("HTTP ${it.status.value}-${it.status.description}: ${it.bodyAsText()}")
        it.body<T>()
    }
}