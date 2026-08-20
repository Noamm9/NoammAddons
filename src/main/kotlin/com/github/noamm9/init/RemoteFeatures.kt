package com.github.noamm9.init

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object RemoteFeatures {
    private const val VERSION = 1

    private val config = runCatching { DataDownloader.loadJson<JsonObject>("features.json") }.onFailure {
        NoammAddons.logger.error("Failed to load remote features", it)
    }.getOrDefault(JsonObject()).also {
        val version = it["version"]?.asInt ?: 0
        if (version < VERSION) ChatUtils.modMessage("&cRemote features are outdated. Expected version &e$VERSION&c, got &e$version&c.")
    }

    fun getFeature(featureId: String?): Map<String, JsonElement> {
        val feature = config[featureId.orEmpty()] as? JsonObject ?: return emptyMap()
        return feature.entrySet().associate { it.key to it.value }
    }

    fun isDisabled(featureId: String) = config["disabled"]?.asJsonArray?.any { it.asString == featureId } == true
}