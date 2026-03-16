package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_VERSION
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.JsonUtils
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object WebUtils {
    private val USER_AGENT = "NoammAddons/$MOD_VERSION${if (NoammAddons.isDev) "-dev" else ""} (+https://noamm.org)"
    private const val PRIVATE_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val TIMEOUT = 10_000
    private val SUCCESS_RANGE = 200..299

    private val threadCounter = AtomicInteger(1)
    val networkDispatcher = Executors.newFixedThreadPool(5) {
        Thread(it, "${NoammAddons.MOD_NAME}-Net-${threadCounter.getAndIncrement()}").apply {
            isDaemon = true
        }
    }.asCoroutineDispatcher()

    fun prepareConnection(url: String): HttpURLConnection {
        if (mc.isSameThread) throw Exception("Cannot make network request on main thread")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty(
            "User-Agent",
            if (url.contains("api.hypixel.net", true)) PRIVATE_USER_AGENT else USER_AGENT
        )
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        return connection
    }

    suspend fun get(url: String): Result<HttpResponse> = withContext(networkDispatcher) {
        runCatching {
            val connection = prepareConnection(url)
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json; charset=UTF-8")
            handleResponse(connection)
        }
    }

    suspend fun post(url: String, body: Any?): Result<HttpResponse> = withContext(networkDispatcher) {
        runCatching {
            val connection = prepareConnection(url)
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json; charset=UTF-8")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            handleResponse(connection)
        }
    }

    suspend fun getString(url: String) = withContext(networkDispatcher) {
        runCatching {
            val res = get(url).getOrThrow()
            if (res.code !in SUCCESS_RANGE) throw IllegalStateException("HTTP ${res.code}: ${res.data}")
            res.data
        }
    }

    suspend inline fun <reified T> getAs(url: String): Result<T> = runCatching {
        return getString(url).mapCatching {
            JsonUtils.json.decodeFromString<T>(it)
        }
    }

    inline fun <reified T> getAs(res: HttpResponse): Result<T> = runCatching {
        JsonUtils.json.decodeFromString<T>(res.data)
    }

    suspend fun downloadBytes(url: String): Result<ByteArray> = withContext(networkDispatcher) {
        runCatching {
            val connection = prepareConnection(url)
            connection.requestMethod = "GET"

            val code = connection.responseCode
            val stream = if (code in SUCCESS_RANGE) connection.inputStream else connection.errorStream

            if (code !in SUCCESS_RANGE) throw IllegalStateException("HTTP $code")

            stream.use { it.readBytes() }
        }
    }

    private fun handleResponse(connection: HttpURLConnection): HttpResponse {
        val code = connection.responseCode
        val stream = if (code in SUCCESS_RANGE) connection.inputStream
        else connection.errorStream ?: connection.inputStream
        val data = stream.bufferedReader().use(BufferedReader::readText)
        val headers = connection.headerFields
            .filterKeys { it != null }
            .mapValues { (_, values) -> values.joinToString(", ") }
        return HttpResponse(code, data, headers)
    }

    data class HttpResponse(val code: Int, val data: String, val headers: Map<String, String>)
}