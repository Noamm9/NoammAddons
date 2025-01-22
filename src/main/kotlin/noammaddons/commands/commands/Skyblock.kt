package noammaddons.commands.commands

import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.utils.ChatUtils.sendChatMessage


object Skyblock: Command("sb") {
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        sendChatMessage("/play sb")
    }
}