package com.github.noamm9.config.migrators

import com.github.noamm9.config.ConfigMigrator
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.GsonUtils.jsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object ConfigMigratorV0ToV1: ConfigMigrator(0, 1), ISelfInit {
    override fun init() = register(this)

    override fun migrate(root: JsonObject): JsonObject {
        val newRoot = jsonObject { addProperty("version", 1) }
        val config = JsonArray()

        root.getAsJsonArray("config")?.forEach { featureElement ->
            val oldFeature = featureElement.asJsonObject
            val newFeature = JsonObject()
            newFeature.addProperty("name", oldFeature.get("name")?.asString ?: return@forEach)
            oldFeature.get("enabled")?.let { newFeature.add("enabled", it) }

            val settings = JsonObject()
            oldFeature.getAsJsonArray("configSettings")?.forEach { settingElement ->
                val entry = settingElement.asJsonObject.entrySet().firstOrNull() ?: return@forEach
                settings.add(entry.key, entry.value)
            }
            newFeature.add("configSettings", settings)

            config.add(newFeature)
        }
        newRoot.add("config", config)

        root.get("hud")?.let { newRoot.add("hud", it) }

        return newRoot
    }
}