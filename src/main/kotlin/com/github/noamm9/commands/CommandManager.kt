package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.impl.misc.WarpShortcuts
import io.github.classgraph.ClassGraph
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object CommandManager {
    val commands = mutableSetOf<BaseCommand>()

    init {
        val scanResult = ClassGraph().enableAllInfo().acceptPackages("com.github.noamm9").ignoreClassVisibility()
            .overrideClassLoaders(Thread.currentThread().contextClassLoader).scan()

        scanResult.use { result ->
            val commandClasses = result.getSubclasses("com.github.noamm9.commands.BaseCommand")
            NoammAddons.logger.info("CommandManager found ${commandClasses.size} commands.")

            commandClasses.forEach { classInfo ->
                try {
                    val instance = classInfo.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand

                    instance?.let { command ->
                        commands.add(command)
                        NoammAddons.logger.info("Registered command: /${command.name}")
                    }
                } catch (e: Exception) {
                    NoammAddons.logger.error("Failed to register command: ${classInfo.name}", e)
                }
            }
        }
        WarpShortcuts.init()
        NoammAddons.logger.info("CommandManager initialized with ${WarpShortcuts.commands.size} commands.")
    }

    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { command ->
                val root = ClientCommandManager.literal(command.name)
                CommandNodeBuilder(root).apply { with(command) { build() } }
                dispatcher.register(root)
                commands.add(command)
                NoammAddons.logger.debug("Registered command: /${command.name}")
            }

            WarpShortcuts.commands.forEach { warpCommand ->
                val root = ClientCommandManager.literal(warpCommand.input)
                CommandNodeBuilder(root).apply { with(warpCommand) { build() } }
                dispatcher.register(root)
            }
        }
    }
}