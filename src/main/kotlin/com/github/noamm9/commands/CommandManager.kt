package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.catch
import io.github.classgraph.ScanResult
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

object CommandManager {
    private val commands = mutableSetOf<BaseCommand>()

    fun registerAll(result: ScanResult) {
        result.getSubclasses(BaseCommand::class.qualifiedName).forEach { ci ->
            val i = catch { ci.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand }
            i?.let(commands::add)
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
}