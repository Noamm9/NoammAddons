package com.github.noamm9.config.migrators

import com.github.noamm9.config.ConfigMigrator
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.GsonUtils.gsonArray
import com.google.gson.JsonObject

object ConfigMigratorV1ToV2: ConfigMigrator(1, 2), ISelfInit {
    private val soundFeatures = setOf(
        "Arrow Hit Sound",
        "Debuff Helper",
        "Etherwarp",
        "Leap Counter",
        "Melody Display",
        "Room Alerts",
        "Secrets",
        "Terminal Solver"
    )

    override fun init() = register(this)

    override fun migrate(root: JsonObject): JsonObject {
        val migrated = root.deepCopy()

        migrated.getAsJsonArray("config")?.forEach { featureElement ->
            val feature = featureElement.asJsonObject
            if (feature.get("name")?.asString !in soundFeatures) return@forEach
            val settings = feature.getAsJsonObject("configSettings") ?: return@forEach

            for (name in listOf("Volume", "Pitch")) {
                val value = settings.get(name) ?: continue
                if (! value.isJsonPrimitive || ! value.asJsonPrimitive.isNumber) continue
                settings.add(name, gsonArray {
                    add(value)
                    add(value)
                })
            }
        }

        migrated.addProperty("version", 2)
        return migrated
    }
}
