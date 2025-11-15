package noammaddons.config

import gg.essential.api.EssentialAPI
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.utils.JsonUtils.fromJson
import noammaddons.utils.JsonUtils.toJson
import java.io.File


class PogObject<T: Any>(fileName: String, val defaultObject: T) {
    private val dataFile = File("config/$MOD_NAME/${fileName}.json")
    private var data: T
    private var lastSavedTime = System.currentTimeMillis()
    private var currentAutosaveIntervalMillis: Long? = null

    init {
        dataFile.parentFile.mkdirs()
        this.data = loadData() ?: defaultObject.also {
            if (! dataFile.exists() || loadData() == null) save()
        }
        registerPogObject(this)
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
            null
        }
    }

    @Synchronized
    fun save() {
        try {
            toJson(dataFile, data)
            lastSavedTime = System.currentTimeMillis()
        }
        catch (_: Exception) {
        }
    }

    fun autosave(intervalMinutes: Long) {
        if (intervalMinutes > 0) this.currentAutosaveIntervalMillis = intervalMinutes * 60 * 1000L
        else this.currentAutosaveIntervalMillis = null
    }

    @Synchronized
    fun setData(newData: T) {
        if (this.data == newData) return
        this.data = newData
        save()
    }

    @Synchronized
    fun getData(): T = data

    companion object {
        private val activePogObjects = mutableListOf<PogObject<*>>()
        private var autosaveThread: Thread? = null

        init {
            EssentialAPI.getShutdownHookUtil().register { shutdown() }
            startAutosaveLoop()
        }

        @Synchronized
        private fun registerPogObject(pogObject: PogObject<*>) {
            if (! activePogObjects.contains(pogObject)) {
                activePogObjects.add(pogObject)
            }
        }

        @Synchronized
        private fun shutdown() {
            autosaveThread?.interrupt()
            val objectsToSave = ArrayList(activePogObjects)
            objectsToSave.forEach {
                try {
                    it.save()
                }
                catch (e: Exception) {
                }
            }
            activePogObjects.clear()
        }

        private fun startAutosaveLoop() {
            if (autosaveThread?.isAlive == true) return

            autosaveThread = Thread {
                try {
                    while (! Thread.currentThread().isInterrupted) {
                        Thread.sleep(10_000)
                        val currentTime = System.currentTimeMillis()

                        val objectsToProcess = ArrayList(activePogObjects)
                        objectsToProcess.forEach { pogObject ->
                            pogObject.currentAutosaveIntervalMillis?.let { interval ->
                                if (currentTime - pogObject.lastSavedTime >= interval) {
                                    val onDiskData = fromJson(pogObject.dataFile, Any::class.java)
                                    if (onDiskData != pogObject.getData()) pogObject.save()
                                    else pogObject.lastSavedTime = currentTime

                                    synchronized(pogObject) {
                                        pogObject.save()
                                    }
                                }
                            }
                        }
                    }
                }
                catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                catch (_: Exception) {
                }
            }.apply {
                isDaemon = true
                name = "PogObject-Autosave-Thread"
                start()
            }
        }
    }
}