package noammaddons.utils

import noammaddons.noammaddons.Companion.mc
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import net.minecraft.util.ResourceLocation
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
	 * Fetches JSON data from the specified URL with retry logic.
	 *
	 * @param url The URL to fetch JSON data from.
	 * @param retryDelayMs The delay in milliseconds between retries. Default is 5 minutes (300,000 ms).
	 * @param maxRetries The maximum number of retries. If set to -1, it will retry indefinitely. Default is -1.
	 * @param callback A callback function that will be invoked with the parsed JSON data or null if retries failed.
	 */
	inline fun <reified T> fetchJsonWithRetry(
		url: String,
		retryDelayMs: Long = 5 * 60 * 1000,
		maxRetries: Int = -1,
		crossinline callback: (T?) -> Unit
	) {
		Thread {
			var attempts = 0
			
			while (maxRetries == -1 || attempts < maxRetries) {
				try {
					println("Attempting to fetch JSON from $url (Attempt $attempts)")
					
					val urlConnection = URL(url).openConnection() as HttpURLConnection
					urlConnection.requestMethod = "GET"
					urlConnection.setRequestProperty("Accept-Charset", "UTF-8")
					
					val reader = BufferedReader(InputStreamReader(urlConnection.inputStream, "UTF-8"))
					val response = reader.readText()
					reader.close()
					
					
					try {
						val gson = Gson()
						val data: T = gson.fromJson(response, T::class.java)
						callback(data)
						return@Thread
					} catch (e: Exception) {
						println("Error decoding JSON: ${e.message}")
						e.printStackTrace()
					}
				} catch (e: Exception) {
					println("Exception during request: ${e.message}")
					e.printStackTrace()
				}
				
				Thread.sleep(retryDelayMs)
				attempts++
			}
			
			callback(null)
		}.start()
	}
}

