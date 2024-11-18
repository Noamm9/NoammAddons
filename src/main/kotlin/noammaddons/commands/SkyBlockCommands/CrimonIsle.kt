package noammaddons.commands.SkyBlockCommands

import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.utils.ChatUtils.sendChatMessage


object CrimonIsle : Command("nether") {
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        sendChatMessage("/warp nether")
    }
}