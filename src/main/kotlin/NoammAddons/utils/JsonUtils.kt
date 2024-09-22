package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.minecraft.util.ResourceLocation
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*

object JsonUtils {
    private val gson = Gson()

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
            FileWriter(file).use { writer -> gson.toJson(data, writer) }
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
        } catch (e: Exception) {
            println("[PogObject] Failed to read JSON: ${e.message}")
            null
        }
    }


    /**
     * Fetches JSON data from a given URL with retry mechanism, running on a separate thread to avoid blocking the main thread.
     *
     * @param url the URL to fetch JSON data from
     * @param retryDelayMs the delay in milliseconds between retries (default: 5 minutes)
     * @param maxRetries the maximum number of retries (default: -1, meaning retry indefinitely)
     * @param callback the callback function to deliver the result, or null if the retries fail
     */
     inline fun <reified T> fetchJsonWithRetry(
        url: String,
        retryDelayMs: Long = 5 * 60 * 1000,
        maxRetries: Int = -1,
        crossinline callback: (T?) -> Unit
    ) {
        Thread {
            val client = OkHttpClient()
            var attempts = 0

            while (maxRetries == -1 || attempts < maxRetries) {
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) return@Thread callback(null)
                    val json = response.body?.string() ?: return@Thread callback(null)

                    val data: T = Json.decodeFromString(json)

            //        PogObject(Regex("\\/([A-Za-z0-9]+)(?=\\.json\$)").find(url)?.destructured!!.component1(), JsonObject().javaClass)
                    callback(data)
                    return@Thread

                } catch (e: Exception) {
                    e.printStackTrace()
                    Thread.sleep(retryDelayMs)
                }
                attempts++
            }
            callback(null)
        }.start()
    }
}

