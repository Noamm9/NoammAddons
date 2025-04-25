package noammaddons.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import net.minecraft.crash.CrashReport
import noammaddons.noammaddons
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.Utils.equalsOneOf
import java.io.BufferedReader
import java.lang.reflect.Type
import java.net.*
import java.security.KeyStore
import javax.net.ssl.*

object WebUtils {

    @JvmStatic
    val sslContext by lazy {
        try {
            val myKeyStore = KeyStore.getInstance("JKS")
            myKeyStore.load(this::class.java.getResourceAsStream("/ctjskeystore.jks"), "changeit".toCharArray())
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            kmf.init(myKeyStore, null)
            tmf.init(myKeyStore)
            val ctx = SSLContext.getInstance("TLS")
            ctx?.init(kmf.keyManagers, tmf.trustManagers, null)
            ctx
        }
        catch (e: Exception) {
            Logger.error("Failed to load keystore. Web requests may fail on older Java versions", e)
            null
        }
    }

    fun makeWebRequest(url: String): URLConnection {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (${noammaddons.MOD_ID})")
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = sslContext?.socketFactory ?: connection.sslSocketFactory
        }
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        return connection
    }

    inline fun <reified T> fetchJsonWithRetry(
        url: String,
        retryDelayMs: Long = 300_000,
        maxRetries: Int = - 1,
        crossinline callback: (T?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            while (maxRetries == - 1 || attempts < maxRetries) {
                val connection = makeWebRequest(url) as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    //    connection.setRequestProperty("Accept", "application/json")

                    when (connection.responseCode) {
                        403 -> Logger.warn("403 Forbidden: $url")
                        502 -> Logger.warn("502 Bad Gateway: $url")
                        503 -> Logger.warn("503 Service Unavailable: $url")
                        504 -> Logger.warn("504 Gateway Timeout: $url")
                    }

                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        val response = reader.readText()
                        val type: Type = object: TypeToken<T>() {}.type
                        val data: T = Gson().fromJson(response, type)
                        callback(data)
                        return@launch
                    }
                }
                catch (e: Exception) {
                    Logger.error("Failed to fetch data from $url", e)
                    if (connection.responseCode.equalsOneOf(403, 502, 503, 504)) return@launch
                    if (e is SocketTimeoutException) return@launch
                    mc.crashed(CrashReport("Failed to fetch data from $url", e))
                }
                finally {
                    connection.disconnect()
                }

                delay(retryDelayMs * (attempts + 1))
                attempts ++
            }
            callback(null)
        }
    }

    fun get(url: String, block: (JsonObject?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = makeWebRequest(url) as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonObject = JsonUtils.stringToJson(response)

                jsonObject.apply(block)
            }.onFailure {
                block(null)
                it.printStackTrace()
            }
        }
    }

    fun sendPostRequest(url: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val connection = makeWebRequest(url) as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
            connection.disconnect()
        }
    }
}