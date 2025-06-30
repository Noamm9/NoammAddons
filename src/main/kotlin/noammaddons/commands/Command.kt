package noammaddons.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noammaddons.NoammAddons
import noammaddons.utils.ChatUtils.modMessage

abstract class Command(val name: String, val aliases: List<String> = listOf(), val usage: String = ""): CommandBase() {
    protected val mc = NoammAddons.mc
    protected val hudData = NoammAddons.hudData
    protected val scope = NoammAddons.scope

    override fun getCommandName(): String {
        return name
    }

    override fun getCommandAliases(): List<String> {
        return aliases
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return usage
    }

    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        modMessage("Command not implemented")
    }
}