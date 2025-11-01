package noammaddons.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.NoammAddons.Companion.scope
import java.io.BufferedReader
import java.lang.reflect.Type
import java.net.*
import java.security.KeyStore
import javax.net.ssl.*

object WebUtils {
    private val sslContext by lazy {
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
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ($MOD_NAME)")
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = sslContext?.socketFactory ?: connection.sslSocketFactory
        }
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        return connection
    }

    inline fun <reified T> fetchJson(url: String, crossinline callback: (T) -> Unit) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                val connection = makeWebRequest(url) as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/json")

                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        val response = reader.readText()
                        val type: Type = object: TypeToken<T>() {}.type
                        val data: T = Gson().fromJson(response, type)
                        callback(data)
                        cancel("Successful Fetch")
                        return@launch
                    }
                }
                catch (e: Exception) {
                    Logger.error("Failed to fetch data from $url", e)
                }
                finally {
                    connection.disconnect()
                }

                delay(300_000)
            }
        }
    }

    fun get(url: String, block: (JsonObject) -> Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val connection = makeWebRequest(url) as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonObject = JsonUtils.stringToJson(response)

                jsonObject.apply(block)
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun sendPostRequest(url: String, body: Any) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val connection = makeWebRequest(url) as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }
                connection.disconnect()
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun readUrl(url: String): String {
        val connection = makeWebRequest(url) as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }
}