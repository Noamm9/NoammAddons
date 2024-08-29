package NoammAddons.commands.SkyBlockCommands

import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender


class DungeonHub: CommandBase() {
    override fun getCommandName(): String {
        return "d"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return ""
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>) {
        mc.thePlayer.sendChatMessage("/warp dungeon_hub")
    }
}