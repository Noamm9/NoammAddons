package noammaddons.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import net.minecraft.util.ResourceLocation
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.ChatUtils.errorMessage
import java.io.*
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import kotlin.reflect.jvm.jvmName


object JsonUtils {
    private val gson = Gson()
    private val gsonBuilder = GsonBuilder().setPrettyPrinting().create()
    val json = Json { ignoreUnknownKeys = true }

    fun JsonObject.getObj(key: String) = this[key]?.jsonObject
    fun JsonObject.getString(key: String) = this[key]?.jsonPrimitive?.content
    fun JsonObject.getInt(key: String) = this[key]?.jsonPrimitive?.int
    fun JsonObject.getDouble(key: String) = this[key]?.jsonPrimitive?.double


    fun stringToJson(s: String): JsonObject {
        return try {
            parseToJsonElement(s).jsonObject
        }
        catch (e: Exception) {
            e.printStackTrace()
            mc.shutdown()
            throw UnsupportedEncodingException("Failed to parse JSON: ${e.message}")
        }
    }


    fun <T> fromJson(file: File, clazz: Class<T>): T? {
        return try {
            FileReader(file).use { reader -> gson.fromJson(reader, clazz) }
        }
        catch (e: Exception) {
            println("[PogObject] Failed to parse JSON: Type: ${clazz.javaClass.simpleName} ${e.message}")
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
    @OptIn(DelicateCoroutinesApi::class)
    inline fun <reified T> fetchJsonWithRetry(
        url: String,
        retryDelayMs: Long = 300_000,
        maxRetries: Int = - 1,
        crossinline callback: (T?) -> Unit
    ) {
        scope.launch {
            val trustAllCerts = arrayOf<TrustManager>(object: X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

            var attempts = 0
            while (maxRetries == - 1 || attempts < maxRetries) {
                try {
                    val urlConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36")
                    }

                    if (urlConnection.responseCode == 403) println(urlConnection.responseMessage + "|| $url")

                    urlConnection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        val response = reader.readText()
                        // println("Response JSON: $response")

                        val type: Type = object: TypeToken<T>() {}.type
                        val data: T = Gson().fromJson(response, type)
                        callback(data)
                        return@launch
                    }

                }
                catch (e: Exception) {
                    e.printStackTrace()

                    errorMessage(
                        listOf(
                            "&cFailed to fetch data!",
                            "&cURL: &b$url",
                            "&e${e::class.qualifiedName ?: e::class.jvmName}: ${e.message ?: "Unknown"}",
                        )
                    )
                }

                delay(retryDelayMs)
                attempts ++
            }

            callback(null)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun get(url: String, block: (JsonObject) -> Unit) {
        scope.launch {
            runCatching {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36")
                connection.setRequestProperty("Accept", "application/json")

                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonObject = stringToJson(response)

                jsonObject.apply(block)
            }.onFailure { catch ->
                catch.printStackTrace()

                errorMessage(
                    listOf(
                        "&cFailed to fetch data!",
                        "&cURL: &b$url",
                        "&e${catch::class.qualifiedName ?: catch::class.jvmName}: ${catch.message ?: "Unknown"}"
                    )
                )
            }
        }
    }

}


