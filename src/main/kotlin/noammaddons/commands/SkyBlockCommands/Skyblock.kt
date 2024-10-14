package noammaddons.commands.SkyBlockCommands

import noammaddons.noammaddons.Companion.mc
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noammaddons.utils.ChatUtils.sendChatMessage


class Skyblock: CommandBase() {
    override fun getCommandName(): String {
        return "sb"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return ""
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>) {
	    sendChatMessage("/play sb")
    }
}