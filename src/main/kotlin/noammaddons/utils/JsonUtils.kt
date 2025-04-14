package noammaddons.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import net.minecraft.crash.CrashReport
import net.minecraft.util.ResourceLocation
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.Utils.equalsOneOf
import java.io.*
import java.lang.reflect.Type
import java.net.*
import java.security.KeyStore
import javax.net.ssl.*


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
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ($MOD_ID)")
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
        scope.launch {
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
        scope.launch {
            runCatching {
                val connection = makeWebRequest(url) as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonObject = stringToJson(response)

                jsonObject.apply(block)
            }.onFailure {
                block(null)
                it.printStackTrace()
            }
        }
    }

    fun sendPostRequest(url: String, body: String) {
        ThreadUtils.runOnNewThread {
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


