package NoammAddons.commands

import NoammAddons.config.Config
import NoammAddons.utils.ChatUtils.getChatBreak
import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat
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
        return getUsage()
    }

     override fun processCommand(sender: ICommandSender, args: Array<String>) {
         if (args.isEmpty()) return EssentialAPI.getGuiUtil().openScreen(Config.gui())
         else when (args[0].lowercase()) {
             "help" -> UChat.chat(getUsage())
             "sim" -> if (args.size > 1) UChat.chat(args.copyOfRange(1, args.size).joinToString(" "))
             else -> EssentialAPI.getGuiUtil().openScreen(Config.gui())
         }

    }

    private fun getUsage() = """
        &b&m${getChatBreak()}
        &b/na &7- &oOpens the Settings GUI.
        &b/na help &7- &oShows this Message.
        &b/na sim [message] &7- &oSimulates a received chat message.
        &b&m${getChatBreak()}
    """.trimIndent()

}
