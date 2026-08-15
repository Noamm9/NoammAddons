@file:Suppress("UNCHECKED_CAST")

package com.github.noamm9.init

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.Config
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.ui.utils.Resolution
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.ScanResult
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import kotlin.system.measureTimeMillis

class ClassGraphInitializer {
    fun initAll() = ClassGraph().enableAllInfo().apply {
        acceptPackages(NoammAddons::class.java.`package`.name)
        overrideClassLoaders(Thread.currentThread().contextClassLoader)
    }.scan().use { scan ->
        val time = measureTimeMillis {
            scan.registerSelfInits()
            scan.registerCustomMenus()
            scan.registerCommands()
            scan.registerFeatures()
        }

        NoammAddons.logger.info("${this::class.simpleName} initialized in $time ms.")
    }

    private fun ScanResult.registerSelfInits() {
        getClassesImplementing(ISelfInit::class.java).forEach { ci ->
            (ci.getInstance<ISelfInit>()).init()
        }
    }

    private fun ScanResult.registerCustomMenus() {
        getClassesImplementing(ICustomMenu::class.java).forEach { ci ->
            ModCompatibility.customMenus.add(ci.getInstance())
        }
    }

    private fun ScanResult.registerCommands() {
        val providers = getClassesImplementing(ICommandProvider::class.java).map { it.getInstance<ICommandProvider>() }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            providers.forEach { provider ->
                val commandBuilder = CommandBuilder()
                with(provider) { commandBuilder.command() }

                commandBuilder.build().forEach { root ->
                    dispatcher.register(root as LiteralArgumentBuilder<FabricClientCommandSource>)
                }
            }
        }
    }

    private fun ScanResult.registerFeatures() {
        getSubclasses(Feature::class.java).forEach { classInfo ->
            val feature = classInfo.getInstance<Feature>().apply(Feature::initialize)
            FeatureManager.hudElements.addAll(feature.hudElements)
            FeatureManager.features.add(feature)
        }

        Config.load()

        register<RenderOverlayEvent> {
            if (mc.screen is HudEditorScreen) return@register

            Resolution.push(event.context)
            FeatureManager.hudElements.forEach { if (it.shouldDraw) it.renderElement(event.context, false) }
            Resolution.pop(event.context)
        }
    }

    private fun <T> ClassInfo.getInstance() = loadClass().getDeclaredField("INSTANCE").get(null) as T
}