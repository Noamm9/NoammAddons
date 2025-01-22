package noammaddons.commands.commands

import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.utils.ChatUtils.sendChatMessage


object DungeonHub: Command("d", listOf("dung")) {
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        sendChatMessage("/warp dungeon_hub")
    }
}