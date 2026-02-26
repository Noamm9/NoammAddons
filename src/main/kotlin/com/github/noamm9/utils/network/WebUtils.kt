package com.github.noamm9.utils.network

import com.github.noamm9.NoammAddons
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
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val TIMEOUT = 10_000

    private val threadCounter = AtomicInteger(1)
    val networkDispatcher = Executors.newFixedThreadPool(5) {
        Thread(it, "${NoammAddons.MOD_NAME}-Net-${threadCounter.getAndIncrement()}").apply {
            isDaemon = true
        }
    }.asCoroutineDispatcher()

    fun prepareConnection(url: String): HttpURLConnection {
        if (mc.isSameThread) throw Exception("Cannot make network request on main thread")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        return connection
    }

    suspend fun getString(url: String) = withContext(networkDispatcher) {
        runCatching {
            val connection = prepareConnection(url)
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            handleResponse(connection)
        }
    }

    suspend inline fun <reified T> get(url: String): Result<T> = runCatching {
        return getString(url).mapCatching {
            JsonUtils.json.decodeFromString<T>(it)
        }
    }

    suspend fun post(url: String, body: Any): Result<String> = withContext(networkDispatcher) {
        runCatching {
            val connection = prepareConnection(url)
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            handleResponse(connection)
        }
    }

    suspend fun downloadBytes(url: String): Result<ByteArray> = withContext(networkDispatcher) {
        runCatching {
            val connection = prepareConnection(url)
            connection.requestMethod = "GET"

            val code = connection.responseCode
            val stream = if (code in 200 .. 299) connection.inputStream else connection.errorStream

            if (code !in 200 .. 299) throw IllegalStateException("HTTP $code")

            stream.use { it.readBytes() }
        }
    }

    private fun handleResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200 .. 299) connection.inputStream
        else connection.errorStream ?: connection.inputStream
        val response = stream.bufferedReader().use(BufferedReader::readText)
        if (code !in 200 .. 299) throw IllegalStateException("HTTP $code: $response")
        return response
    }
}