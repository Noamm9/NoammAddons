package noammaddons.config

import gg.essential.api.EssentialAPI
import noammaddons.noammaddons.Companion.MOD_NAME
import noammaddons.utils.JsonUtils
import noammaddons.config.EventHandlers.scheduleSave
import noammaddons.utils.ThreadUtils.runEvery
import java.io.*


class PogObject<T : Any>(fileName: String, private val defaultObject: T) {
    private val dataFile = File("config/$MOD_NAME/${fileName}.json")
    private var data: T = loadData() ?: defaultObject

    init {
        dataFile.parentFile.mkdirs()
        autosave(5)
    }

    private fun loadData(): T? {
        return if (dataFile.exists()) {
            JsonUtils.fromJson(dataFile, defaultObject::class.java)
        } else {
            println("[PogObject] No existing data found, loading defaults.")
            null
        }
    }

    fun save() {
        JsonUtils.toJson(dataFile, data)
        println("[PogObject] Data saved successfully.")
    }

    fun autosave(intervalMinutes: Long = 5) {
        scheduleSave(this, intervalMinutes)
    }

    fun updateData(newData: T) {
        data = newData
    }

    fun getData(): T = data
}


object EventHandlers {
    private val autosaveIntervals = mutableMapOf<PogObject<*>, Long>()

    fun scheduleSave(pogObject: PogObject<*>, intervalMinutes: Long) {
        autosaveIntervals[pogObject] = intervalMinutes * 60 * 20
    }
	
	init {
		EssentialAPI.getShutdownHookUtil().register { autosaveIntervals.keys.forEach { it.save() } }
		
		runEvery(1) {
			autosaveIntervals.forEach { (pogObject, interval) ->
				if (System.currentTimeMillis() % interval != 0L) return@forEach
				pogObject.save()
			}
		}
	}
}


