package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.utils.JsonUtils
import kotlinx.serialization.json.*
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object Config {
    private val configDir = FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).toFile()
    private val configFile = File(configDir, "config.json").apply {
        if (! configDir.exists()) configDir.mkdirs()
        runCatching(::createNewFile).apply {
            onSuccess { logger.info("Successfully initialized config file path") }
            onFailure { logger.error("Error initializing config", it) }
        }
    }

    fun load() {
        val fileContent = configFile.readText().takeUnless(String::isEmpty) ?: return
        val root = JsonUtils.json.parseToJsonElement(fileContent).jsonObject

        root["config"]?.jsonArray?.forEach { featureElement ->
            val featureObj = featureElement.jsonObject
            val feature = FeatureManager.getFeatureByName(featureObj["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach) ?: return@forEach
            if (featureObj["enabled"]?.jsonPrimitive?.booleanOrNull != feature.enabled) feature.toggle()

            featureObj["configSettings"]?.jsonArray?.forEach { settingElement ->
                val entry = settingElement.jsonObject.entries.firstOrNull() ?: return@forEach
                val setting = feature.getSettingByName(entry.key)
                if (setting is Savable) setting.read(entry.value)
            }
        }

        root["hud"]?.jsonArray?.forEach { hudElement ->
            val hudObj = hudElement.jsonObject
            val hud = FeatureManager.getHudByName(hudObj["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach) ?: return@forEach
            hudObj["x"]?.jsonPrimitive?.floatOrNull?.let { hud.x = it }
            hudObj["y"]?.jsonPrimitive?.floatOrNull?.let { hud.y = it }
            hudObj["scale"]?.jsonPrimitive?.floatOrNull?.let { hud.scale = it }
        }
    }

    fun save() = configFile.writeText(JsonUtils.json.encodeToString(buildJsonObject {
        putJsonArray("config") {
            for (feature in FeatureManager.features) {
                addJsonObject {
                    put("name", feature.name)
                    put("enabled", feature.enabled)
                    putJsonArray("configSettings") {
                        for (setting in feature.configSettings) {
                            if (setting is Savable) {
                                addJsonObject { put(setting.saveKey ?: setting.name, setting.write()) }
                            }
                        }
                    }
                }
            }
        }
        putJsonArray("hud") {
            for (hud in FeatureManager.hudElements) {
                addJsonObject {
                    put("name", hud.name)
                    put("x", hud.x)
                    put("y", hud.y)
                    put("scale", hud.scale)
                }
            }
        }
    }))
}