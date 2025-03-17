package noammaddons.commands

import net.minecraftforge.client.ClientCommandHandler
import noammaddons.commands.commands.*
import noammaddons.mixins.AccessorCommandHandler
import noammaddons.noammaddons.Companion.config

object CommandManager {
    val commands = setOf(
        NoammAddonsCommands,
        CrimonIsle,
        DungeonHub,
        End,
        Skyblock,
        hub
    )

    fun registerCommands() {
        commands.forEach(::registerCommand)
        if (config.pvCommand) registerCommand(ProfileViewerCommand)
    }

    fun registerCommand(command: Command) = ClientCommandHandler.instance.registerCommand(command)

    fun unregisterCommand(command: Command) {
        val handler = ClientCommandHandler.instance as? AccessorCommandHandler ?: return
        val commandMap = handler.commandMap
        val key = commandMap.entries.find { it.value == command }?.key ?: return
        commandMap.remove(key)
    }
}