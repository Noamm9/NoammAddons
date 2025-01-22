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
        val commandName = command.commandName.lowercase()
        if (! commandMap.containsKey(commandName)) return
        if (commandMap[commandName] != command) return
        commandMap.remove(commandName)
    }
}