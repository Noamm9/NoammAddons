package noammaddons.config

import gg.essential.api.EssentialAPI
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.MOD_NAME
import noammaddons.utils.JsonUtils.fromJson
import noammaddons.utils.JsonUtils.toJson
import noammaddons.utils.ThreadUtils.loop
import java.io.File


class PogObject<T: Any>(fileName: String, private val defaultObject: T) {
    private val dataFile = File("config/$MOD_NAME/${fileName}.json")
    private var data: T = loadData() ?: defaultObject
    private val autosaveIntervals = mutableMapOf<PogObject<*>, Long>()
    private var lastSavedTime = System.currentTimeMillis()

    init {
        dataFile.parentFile.mkdirs()
        autosave(5)
    }

    private fun loadData(): T? {
        return try {
            if (dataFile.exists()) {
                val loadedData = fromJson(dataFile, defaultObject::class.java)
                if (loadedData != null && loadedData::class.java == defaultObject::class.java) {
                    loadedData
                }
                else {
                    Logger.info("[PogObject] Data type mismatch, loading defaults.")
                    null
                }
            }
            else {
                Logger.info("[PogObject] No existing data found, loading defaults.")
                null
            }
        }
        catch (e: Exception) {
            Logger.info("[PogObject]: ${this.javaClass.simpleName} Error loading data: ${e.message}")
            null
        }
    }

    @Synchronized
    fun save() {
        try {
            toJson(dataFile, data)
            Logger.info("[PogObject]: ${this.javaClass.simpleName} Data saved successfully.")
        }
        catch (e: Exception) {
            Logger.info("[PogObject]: ${this.javaClass.simpleName}: Failed to save data: ${e.message}")
            e.printStackTrace()
        }
    }

    fun autosave(intervalMinutes: Long = 5) {
        scheduleSave(this, intervalMinutes)
    }

    fun setData(newData: T) {
        data = newData
    }

    fun getData(): T = data

    private fun scheduleSave(pogObject: PogObject<*>, intervalMinutes: Long) {
        autosaveIntervals[pogObject] = intervalMinutes * 1000 * 60
    }

    init {
        EssentialAPI.getShutdownHookUtil().register {
            autosaveIntervals.keys.forEach { it.save() }
        }

        loop(10_000) {
            val currentTime = System.currentTimeMillis()
            autosaveIntervals.forEach { (pogObject, interval) ->
                if (currentTime - pogObject.lastSavedTime < interval) return@forEach
                if (fromJson(pogObject.dataFile, defaultObject::class.java) == pogObject.data) return@forEach

                pogObject.save()
                pogObject.lastSavedTime = currentTime
            }
        }
    }
}