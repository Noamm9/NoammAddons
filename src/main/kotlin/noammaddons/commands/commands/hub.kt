package noammaddons.commands.commands

import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.utils.ChatUtils.sendChatMessage

object hub: Command("h") {
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        sendChatMessage("/hub")
    }
}