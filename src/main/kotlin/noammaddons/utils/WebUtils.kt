package noammaddons.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.scope
import java.io.BufferedReader
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
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = sslContext?.socketFactory ?: connection.sslSocketFactory
        }
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return connection
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
                connection.responseCode
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