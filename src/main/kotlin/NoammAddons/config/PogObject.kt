// Package declaration
package NoammAddons.config

import NoammAddons.NoammAddons.Companion.MOD_NAME
import NoammAddons.NoammAddons.Companion.mc
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.world.WorldEvent.Unload
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader


class PogObject<T : Any>(fileName: String, private val defaultObject: T) {
    private val dataFile = File("config/$MOD_NAME/${fileName}.json")
    private var data: T = loadData() ?: defaultObject

    init {
        dataFile.parentFile.mkdirs()
        autosave(5)
    }

    private fun loadData(): T? {
        return if (dataFile.exists()) {
            JsonHelper.fromJson(dataFile, defaultObject::class.java)
        } else {
            println("[PogObject] No existing data found, loading defaults.")
            null
        }
    }

    fun save() {
        JsonHelper.toJson(dataFile, data)
        println("[PogObject] Data saved successfully.")
    }

    fun autosave(intervalMinutes: Long = 5) {
        EventHandlers.scheduleSave(this, intervalMinutes)
    }

    fun updateData(newData: T) {
        data = newData
    }

    fun getData(): T = data


    object JsonHelper {
        val gson = Gson()

        // Load JSON data from file
        fun <T> fromJson(file: File, clazz: Class<T>): T? {
            return try {
                FileReader(file).use { reader -> gson.fromJson(reader, clazz) }
            } catch (e: Exception) {
                println("[PogObject] Failed to parse JSON: ${e.message}")
                null
            }
        }

        // Save JSON data to file
        fun toJson(file: File, data: Any) {
            try {
                FileWriter(file).use { writer -> gson.toJson(data, writer) }

            } catch (e: Exception) {
                println("[PogObject] Failed to save JSON: ${e.message}")
            }
        }

        fun readJsonFile(resourcePath: String): JsonObject? {
            // Specify the path to the JSON file, e.g., "yourmodid:folder/file.json"
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
    }

    object EventHandlers {
        private val autosaveIntervals = mutableMapOf<PogObject<*>, Long>()

        // Schedule saving of the PogObject data at specified intervals
        fun scheduleSave(pogObject: PogObject<*>, intervalMinutes: Long) {
            autosaveIntervals[pogObject] = intervalMinutes * 60 * 20
        }

        // Save data on game unload event
        @SubscribeEvent
        fun onGameUnload(event: Unload) {
            autosaveIntervals.keys.forEach { it.save() }
        }

        // Save data on specific tick intervals
        @SubscribeEvent
        fun onTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            autosaveIntervals.forEach { (pogObject, interval) ->
                if (System.currentTimeMillis() % interval != 0L) return
                pogObject.save()
            }
        }
    }
}


