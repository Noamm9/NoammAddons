package com.github.noamm9.features

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.Config
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.render.Render2D.width
import io.github.classgraph.ClassGraph

object FeatureManager {
    val hudElements = mutableListOf<HudElement>()
    val features = mutableSetOf<Feature>()

    fun registerFeatures() {
        val scanResult = ClassGraph()
            .enableAllInfo()
            .acceptPackages("com.github.noamm9")
            .ignoreClassVisibility()
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        scanResult.use { result ->
            val featureClasses = result.getSubclasses("com.github.noamm9.features.Feature")
            NoammAddons.logger.debug("ClassGraph found ${featureClasses.size} subclasses of Feature")

            featureClasses.forEach { classInfo ->
                try {
                    val clazz = classInfo.loadClass()
                    val instance = clazz.getDeclaredField("INSTANCE").get(null) as? Feature

                    instance?.let { feature ->
                        feature.initialize()
                        hudElements.addAll(feature.hudElements)
                        features.add(feature)
                        NoammAddons.logger.info("Successfully loaded feature: ${feature::class.simpleName}")
                    }
                }
                catch (e: Exception) {
                    NoammAddons.logger.error("Failed to load feature class: ${classInfo.name}", e)
                }
            }
        }

        Config.load()

        register<RenderOverlayEvent> {
            if (mc.screen == HudEditorScreen) return@register
            Resolution.refresh()
            Resolution.push(event.context)
            hudElements.forEach { if (it.shouldDraw) it.renderElement(event.context, false) }
            Resolution.pop(event.context)
        }
    }

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