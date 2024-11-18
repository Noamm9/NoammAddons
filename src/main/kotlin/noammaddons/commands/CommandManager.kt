package noammaddons.commands

import net.minecraftforge.client.*
import noammaddons.commands.SkyBlockCommands.*

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
        commands.forEach(ClientCommandHandler.instance::registerCommand)
    }
}