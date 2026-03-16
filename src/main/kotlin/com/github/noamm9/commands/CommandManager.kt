package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import io.github.classgraph.ClassGraph
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object CommandManager {
    val commands = mutableSetOf<BaseCommand>()

    init {
        val result = ClassGraph()
            .enableAllInfo()
            .acceptPackages(this::class.java.`package`.name)
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        result.use {
            it.getSubclasses(BaseCommand::class.qualifiedName).forEach { ci ->
                val i = ci.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand
                i?.let(commands::add)
            }
        }
    }

    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { command ->
                val root = ClientCommandManager.literal(command.name)
                CommandNodeBuilder(root).apply { with(command) { build() } }
                dispatcher.register(root)
                NoammAddons.logger.debug("Registered command: /${command.name}")
            }
        }
    }
}