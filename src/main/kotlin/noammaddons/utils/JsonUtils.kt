package noammaddons.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.minecraft.util.ResourceLocation
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import java.io.*
import java.lang.reflect.Type
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object JsonUtils {
    private val gson = Gson()
    private val gsonBuilder = GsonBuilder().setPrettyPrinting().create()

    fun <T> fromJson(file: File, clazz: Class<T>): T? {
        return try {
            FileReader(file).use { reader -> gson.fromJson(reader, clazz) }
        }
        catch (e: Exception) {
            println("[PogObject] Failed to parse JSON: ${e.message}")
            null
        }
    }


    fun toJson(file: File, data: Any) {
        try {
            FileWriter(file).use { writer ->
                gsonBuilder.toJson(data, writer)
            }
        }
        catch (e: Exception) {
            println("[PogObject] Failed to save JSON: ${e.message}")
        }
    }


    fun readJsonFile(resourcePath: String): JsonObject? {
        val resourceLocation = ResourceLocation(resourcePath)

        return try {
            mc.resourceManager.getResource(resourceLocation).inputStream.use { inputStream ->
                val reader = InputStreamReader(inputStream)
                Gson().fromJson(reader, JsonObject::class.java)
            }
        }
        catch (e: Exception) {
            println("[PogObject] Failed to read JSON: ${e.message}")
            null
        }
    }


    /**
     * Fetches JSON data from the specified URL with retry logic.
     *
     * @param url The URL to fetch JSON data from.
     * @param retryDelayMs The delay in milliseconds between retries. Default is 5 minutes (300,000 ms).
     * @param maxRetries The maximum number of retries. If set to -1, it will retry indefinitely. Default is -1.
     * @param callback A callback function that will be invoked with the parsed JSON data or null if retries failed.
     */
    inline fun <reified T> fetchJsonWithRetry(
        url: String,
        retryDelayMs: Long = 30_000,
        maxRetries: Int = - 1,
        crossinline callback: (T?) -> Unit
    ) {
        Thread {
            // Disable SSL certificate validation for shitty java versions
            val trustAllCerts = arrayOf<TrustManager>(object: X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

            // Ignore host name verification
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

            var attempts = 0
            while (maxRetries == - 1 || attempts < maxRetries) {
                try {
                    if (config.DevMode) println("Attempting to fetch JSON from $url (Attempt $attempts)")

                    val urlConnection = URL(url).openConnection() as HttpsURLConnection
                    urlConnection.requestMethod = "GET"
                    urlConnection.setRequestProperty("Accept-Charset", "UTF-8")

                    val reader = BufferedReader(InputStreamReader(urlConnection.inputStream, "UTF-8"))
                    val response = reader.readText()
                    reader.close()

                    if (config.DevMode) println("Response JSON: $response")

                    try {
                        val type: Type = object: TypeToken<T>() {}.type
                        val data: T = Gson().fromJson(response, type)
                        callback(data)
                        return@Thread
                    }
                    catch (e: Exception) {
                        println("Error decoding JSON: ${e.message}")
                        e.printStackTrace()
                    }

                }
                catch (e: Exception) {
                    println("Exception during request: ${e.message}")
                    e.printStackTrace()
                }

                Thread.sleep(retryDelayMs)
                attempts ++
            }

            callback(null)
        }.start()
    }

}

