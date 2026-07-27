package com.github.noamm9.init

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.config.Config
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager.features
import com.github.noamm9.features.FeatureManager.hudElements
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.ui.utils.Resolution
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.ScanResult
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import kotlin.system.measureTimeMillis

class ClassGraphInitializer {
    private val scan = ClassGraph().enableAllInfo().apply {
        acceptPackages(NoammAddons::class.java.`package`.name)
        overrideClassLoaders(Thread.currentThread().contextClassLoader)
    }.scan()

    fun initAll() = scan.use { scan ->
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
            (ci.getInstance() as? ISelfInit)?.init()
        }
    }

    private fun ScanResult.registerCustomMenus() {
        getClassesImplementing(ICustomMenu::class.java).forEach { ci ->
            val instance = ci.getInstance() as? ICustomMenu
            instance?.let(ModCompatibility.customMenus::add)
        }
    }

    private fun ScanResult.registerCommands() {
        val commands = mutableSetOf<BaseCommand>()

        getSubclasses(BaseCommand::class.qualifiedName).forEach { ci ->
            val command = ci.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand
            command?.let(commands::add)
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { command ->
                val roots = mutableListOf(ClientCommands.literal(command.name))
                command.aliases.forEach { roots.add(ClientCommands.literal(it)) }
                roots.forEach { root ->
                    CommandNodeBuilder(root).apply { with(command) { build() } }
                    dispatcher.register(root)
                }

                NoammAddons.logger.debug("Registered command: /${command.name}")
            }
        }
    }

    private fun ScanResult.registerFeatures() {
        getSubclasses(Feature::class.java).forEach { classInfo ->
            try {
                val instance = classInfo.loadClass().getDeclaredField("INSTANCE").get(null) as? Feature

                instance?.let { feature ->
                    feature.initialize()
                    hudElements.addAll(feature.hudElements)
                    features.add(feature)
                }
            }
            catch (e: Exception) {
                NoammAddons.logger.error("Failed to load feature class: ${classInfo.name}", e)
            }
        }

        Config.load()

        register<RenderOverlayEvent> {
            if (mc.screen is HudEditorScreen) return@register

            Resolution.refresh()
            Resolution.push(event.context)
            hudElements.forEach { if (it.shouldDraw) it.renderElement(event.context, false) }
            Resolution.pop(event.context)
        }
    }

    private fun ClassInfo.getInstance() = loadClass().getDeclaredField("INSTANCE").get(null)
}