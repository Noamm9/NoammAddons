package com.github.noamm9.init

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils
import com.google.gson.JsonElement

object RemoteFeatures {
    private const val EXPECTED_VERSION = 1

    private val config by lazy {
        runCatching { DataDownloader.loadJson<Map<String, JsonElement>>("features.json") }
            .onFailure { NoammAddons.logger.error("Failed to load remote features", it) }
            .getOrDefault(emptyMap())
            .also {
                val version = it["version"]?.asInt ?: 0
                if (version < EXPECTED_VERSION) ChatUtils.modMessage("&cRemote features are outdated. Expected version &e$EXPECTED_VERSION&c, got &e$version&c.")
            }
    }

    fun getFeature(featureId: String): Map<String, JsonElement> {
        val feature = config[featureId]?.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return emptyMap()
        return feature.entrySet().associate { it.key to it.value }
    }

    fun isDisabled(featureId: String) = config["disabled"]?.asJsonArray?.any { it.asString == featureId } == true
}
