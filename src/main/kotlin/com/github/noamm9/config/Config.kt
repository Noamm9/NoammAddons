package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.utils.JsonUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object Config {
    private val configDir = FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).toFile()
    private val configFile = File(configDir, "config.json").apply {
        if (! configDir.exists()) configDir.mkdirs()
        runCatching(::createNewFile).apply {
            onFailure { logger.error("Error initializing config", it) }
            onSuccess { logger.info("Successfully initialized config file path") }
        }
    }

    @Suppress("DEPRECATION")
    fun load() {
        runCatching {
            val fileContent = configFile.bufferedReader().use { it.readText() }.takeUnless { it.isEmpty() } ?: return
            val jsonObject = JsonParser.parseString(fileContent).asJsonObject ?: return

            for (featureElement in jsonObject.getAsJsonArray("config") ?: return) {
                val featureObj = featureElement.asJsonObject
                val feature = FeatureManager.getFeatureByName(featureObj.get("name").asString) ?: continue
                if (featureObj.get("enabled").asBoolean != feature.enabled) feature.toggle()

                featureObj.getAsJsonArray("configSettings")?.forEach { settingElement ->
                    val settingEntry = settingElement.asJsonObject.entrySet().firstOrNull() ?: return@forEach
                    val setting = feature.getSettingByName(settingEntry.key)
                    if (setting is Savable) setting.read(settingEntry.value)
                }
            }

            jsonObject.getAsJsonArray("hud")?.forEach { hudElement ->
                val hudObj = hudElement.asJsonObject
                val hudInstance = FeatureManager.getHudByName(hudObj.get("name").asString) ?: return@forEach

                hudObj.get("x")?.let { hudInstance.x = it.asFloat }
                hudObj.get("y")?.let { hudInstance.y = it.asFloat }
                hudObj.get("scale")?.let { hudInstance.scale = it.asFloat }
            }

        }.apply {
            onFailure { logger.error("Error loading config", it) }
            onSuccess { logger.info("Successfully loaded config") }
        }
    }

    fun save() {
        runCatching {
            val jsonObject = JsonObject().apply {
                add("config", JsonArray().apply {
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
                })
                add("hud", JsonArray().apply {
                    for (hud in FeatureManager.hudElements) {
                        add(JsonObject().apply {
                            add("name", JsonPrimitive(hud.name))
                            add("x", JsonPrimitive(hud.x))
                            add("y", JsonPrimitive(hud.y))
                            add("scale", JsonPrimitive(hud.scale))
                        })
                    }
                })
            }

            configFile.bufferedWriter().use { it.write(JsonUtils.gsonBuilder.toJson(jsonObject)) }
        }.apply {
            onFailure { logger.error("Error on saving config", it) }
            onSuccess { logger.info("Successfully saved config") }
        }
    }
}

