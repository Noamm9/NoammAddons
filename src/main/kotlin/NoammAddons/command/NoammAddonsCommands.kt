package NoammAddons.command

import NoammAddons.config.Config
import NoammAddons.utils.Utils.ModMessage
import gg.essential.api.EssentialAPI
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender


class NoammAddonsCommands : CommandBase() {
    override fun getCommandName(): String {
        return "na"
    }
    override fun getCommandAliases(): List<String> {
        return listOf(
            "noammaddons",
            "na"
        )
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/noammaddons"
    }


     override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            ModMessage("Opening GUI...")
            EssentialAPI.getGuiUtil().openScreen(Config.gui())
        }
    }
}
