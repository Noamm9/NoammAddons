package NoammAddons.commands

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.Config
import NoammAddons.config.EditGui.ElementsManager
import NoammAddons.config.EditGui.HudEditorScreen
import NoammAddons.utils.ChatUtils.getChatBreak
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.GuiUtils.openScreen
import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge


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
         return if (args.isEmpty()) mc.openScreen(Config.gui())
         else when (args[0].lowercase()) {
             "help" -> UChat.chat(getUsage())
             "sim" -> if (args.size > 1) sendFakeChatMessage(args.copyOfRange(1, args.size).joinToString(" ")) else UChat.chat(getUsage())
             "edit" -> mc.openScreen(HudEditorScreen(ElementsManager.elements))
             else -> mc.openScreen(Config.gui())
         }

    }

    private fun getUsage() = """
        &b&m${getChatBreak()}
        &b/na &7- &oOpens the Settings GUI.
        &b/na help &7- &oShows this Message.
        &b/na sim [message] &7- &oSimulates a received chat message.
        &b/na editgui &7- &oOpens the Edit Hud GUI.
        &b&m${getChatBreak()}
    """.trimIndent()

    private fun sendFakeChatMessage(message: String) {
        val chatComponent = ChatComponentText(message)
        modMessage(message)
        val event = ClientChatReceivedEvent(0.toByte(), chatComponent)
        MinecraftForge.EVENT_BUS.post(event)
    }


}
