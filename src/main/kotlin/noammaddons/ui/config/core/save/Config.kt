package noammaddons.ui.config.core.save

import com.google.gson.*
import noammaddons.features.FeatureManager
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.MOD_NAME
import noammaddons.noammaddons.Companion.mc
import noammaddons.ui.config.ConfigGUI
import java.io.File


object Config {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val parser = JsonParser()
    var initialised = false

    private val configFile = File(mc.mcDataDir, "config/$MOD_NAME/config.json").apply {
        runCatching(::createNewFile).apply {
            onFailure { Logger.error("Error initializing config", it) }
            onSuccess { Logger.info("Successfully initialized config file path: new:$it") }
        }
    }

    fun load() {
        if (ConfigGUI.config.keys.isEmpty()) return
        runCatching {
            with(configFile.bufferedReader().use { it.readText() }) {
                if (isEmpty()) return

                val jsonArray = parser.parse(this).asJsonArray ?: return
                for (features in jsonArray) {
                    val featureObj = features?.asJsonObject ?: continue
                    val feature = FeatureManager.getFeatureByName(featureObj.get("name").asString) ?: continue
                    if (featureObj.get("enabled").asBoolean != feature.enabled) {
                        feature.toggle()
                    }

                    for (j in featureObj.get("configSettings").asJsonArray) {
                        val settingObj = j?.asJsonObject?.entrySet() ?: continue
                        val setting = feature.getSettingByName(settingObj.firstOrNull()?.key) ?: continue
                        if (setting is Savable) setting.read(settingObj.first().value)
                    }
                }
            }
        }.apply {
            onFailure { Logger.error("Error loading config", it) }
            onSuccess {
                Logger.info("Successfully loaded config")
                initialised = true
            }
        }
    }

    fun save() {
        runCatching {
            val jsonArray = JsonArray().apply {
                for (feature in FeatureManager.features) {
                    add(JsonObject().apply {
                        add("name", JsonPrimitive(feature.name))
                        add("enabled", JsonPrimitive(feature.enabled))
                        add("configSettings", JsonArray().apply {
                            for (setting in feature.configSettings) {
                                if (setting is Savable) {
                                    add(JsonObject().apply { add(setting.name, setting.write()) })
                                }
                            }
                        })
                    })
                }
            }
            configFile.bufferedWriter().use { it.write(gson.toJson(jsonArray)) }
        }.apply {
            onFailure { Logger.error("Error on saving config", it) }
            onSuccess { Logger.info("Successfully saved config") }
        }
    }
}