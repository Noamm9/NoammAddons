package noammaddons.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noammaddons.noammaddons
import noammaddons.utils.ChatUtils.modMessage

abstract class Command(val name: String, val aliases: List<String> = listOf(), val usage: String = ""): CommandBase() {
    protected val mc = noammaddons.mc
    protected val hudData = noammaddons.hudData
    protected val scope = noammaddons.scope

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