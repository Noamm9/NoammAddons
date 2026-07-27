package com.github.noamm9.features

import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.render.Render2D.width

object FeatureManager {
    val features = mutableSetOf<Feature>()
    val hudElements = mutableListOf<HudElement>()

    fun getFeaturesByCategory(category: CategoryType): List<Feature> {
        return features.filter { it.category == category }
    }

    fun getFeatureByName(name: String): Feature? {
        return features.find { it.name == name }
    }

    fun getHudByName(name: String): HudElement? {
        return hudElements.find { it.name == name }
    }

    fun createFeatureList(): String {
        val featureList = StringBuilder()
        for ((category, features) in features.groupBy { it.category }.entries.sortedBy { it.key.ordinal }) {
            featureList.appendLine("Category: ${category.name}")
            for (feature in features.sortedByDescending { it.name.width() }) {
                featureList.appendLine("- ${feature.name}: ${feature.description ?: ""}")
            }
            featureList.appendLine()
        }
        return featureList.toString()
    }
}