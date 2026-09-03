package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.utils.*
import com.github.noamm9.utils.GsonUtils.jsonArray
import com.github.noamm9.utils.GsonUtils.jsonObject
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object ConfigManager {
    private val configPath = FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME)
    private val configsDir = configPath.resolve("configs").toFile()
    private val defaultConfigFile = File(configPath.toFile(), "config.json")
    private val selectedConfig = PogObject<String>("currentConfig", "default")
    private var configFile = FileHandler(getConfigs()[selectedConfig.get()] ?: defaultConfigFile)
    private const val VERSION = 1

    fun getConfigs(): Map<String, File> {
        val named = configsDir.listFiles()?.associateBy { it.nameWithoutExtension } ?: emptyMap()
        return named + ("default" to defaultConfigFile)
    }

    fun createConfig(configName: String): Boolean {
        if (configName in getConfigs().keys) {
            ChatUtils.modMessage("&cThere is already a config named \"$configName\".")
            return false
        }
        configsDir.mkdirs()
        val newFile = File(configsDir, "$configName.json")
        configFile.file.copyTo(newFile)
        configFile = FileHandler(newFile)
        selectedConfig.set(configName)
        ChatUtils.modMessage("&aSuccessfully created config \"$configName\".")
        return true
    }

    fun changeConfig(configName: String) {
        val newConfigFile = getConfigs()[configName] ?: return ChatUtils.modMessage("&cNo config named \"$configName\" was found.")
        if (! newConfigFile.exists()) return ChatUtils.modMessage("&cNo config file found for \"$configName\".")
        configFile = FileHandler(newConfigFile)
        selectedConfig.set(configName)
        load()
        ChatUtils.modMessage("&aSuccessfully loaded config \"$configName\".")
    }

    fun deleteConfig(configName: String): Boolean {
        if (configName == "default") {
            ChatUtils.modMessage("&cYou cannot delete the default config.")
            return false
        }
        val file = getConfigs()[configName] ?: run {
            ChatUtils.modMessage("&cNo config found with the name \"$configName\".")
            return false
        }
        file.delete()
        ChatUtils.modMessage("&aSuccessfully deleted the config \"$configName\".")
        if (configName == selectedConfig.get()) changeConfig("default")
        return true
    }

    fun load() {
        val fileContent = configFile.read().takeUnless(String::isEmpty) ?: return
        val root = JsonParser.parseString(fileContent).asJsonObject
        val originalVersion = root.get("version")?.asInt ?: 0

        val migrated = ConfigMigrator.migrate(root, originalVersion)
        if (migrated.get("version")?.asInt != VERSION) error("[ConfigManager] unsupported config version $originalVersion")

        if (originalVersion != VERSION) {
            configFile.write(GsonUtils.gson.toJson(migrated))
            NoammAddons.logger.info("[ConfigManager] migrated config from version $originalVersion to $VERSION")
        }

        read(migrated)
    }

    fun save() = configFile.write(GsonUtils.gson.toJson(jsonObject {
        addProperty("version", VERSION)
        add("config", jsonArray {
            for (feature in FeatureManager.features) add(jsonObject {
                addProperty("name", feature.name)
                addProperty("enabled", feature.enabled)
                add("configSettings", jsonObject {
                    for (setting in feature.configSettings) {
                        if (setting !is Savable) continue
                        add(setting.jsonName, setting.write())
                    }
                })
            })
        })
        add("hud", jsonArray {
            for (hud in FeatureManager.hudElements) add(jsonObject {
                addProperty("name", hud.name)
                addProperty("x", hud.x)
                addProperty("y", hud.y)
                addProperty("scale", hud.scale)
            })
        })
    }))

    private fun read(root: JsonObject) {
        root.getAsJsonArray("config").map { it.asJsonObject }.forEach { featureElement ->
            val feature = FeatureManager.getFeatureByName(featureElement.get("name")?.asString ?: return@forEach) ?: return@forEach
            if (featureElement.get("enabled")?.asBoolean != feature.enabled) feature.toggle()
            featureElement.getAsJsonObject("configSettings").entrySet().forEach { (name, value) ->
                (feature.getSettingByName(name) as? Savable)?.read(value)
            }
        }

        root.getAsJsonArray("hud")?.forEach { hudElement ->
            val hudObj = hudElement.asJsonObject
            val hud = FeatureManager.getHudByName(hudObj.get("name")?.asString ?: return@forEach) ?: return@forEach
            hudObj.get("x")?.asFloat?.let { hud.x = it }
            hudObj.get("y")?.asFloat?.let { hud.y = it }
            hudObj.get("scale")?.asFloat?.let { hud.scale = it }
        }
    }
}