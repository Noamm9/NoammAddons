package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.ThreadUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
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
import kotlinx.coroutines.*
import okhttp3.Dispatcher
import java.util.concurrent.*

object WebUtils {
    private val sharedRequests = ConcurrentHashMap<String, Deferred<Result<HttpResponse>>>()
    private val httpDispatcher = Dispatcher()

    val client = HttpClient(OkHttp) {
        engine {
            config { dispatcher(httpDispatcher) }
        }
        install(UserAgent) { agent = "$MOD_NAME/$MOD_VERSION (+https://noamm.org)" }
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
        }
        install(ContentNegotiation) { json(JsonUtils.json) }
        install(ContentEncoding) {
            gzip(1.0F)
            deflate(0.9F)
        }
        
        install(WebSockets)
    }

    init {
        ThreadUtils.addShutdownHook {
            client.close()
            client.cancel()
            httpDispatcher.cancelAll()
            httpDispatcher.executorService.shutdown()
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