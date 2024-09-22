package NoammAddons.commands

import NoammAddons.config.Config
import NoammAddons.config.EditGui.HudEditorScreen
import NoammAddons.events.Chat
import NoammAddons.features.General.AutoReaperArmorSwap.reaperSwap
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.getChatBreak
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.GuiUtils.openScreen
import NoammAddons.utils.PlayerUtils.rotateSmoothly
import gg.essential.universal.UChat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
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

     @OptIn(DelicateCoroutinesApi::class)
     override fun processCommand(sender: ICommandSender, args: Array<String>) {
         if (args.isEmpty()) openScreen(Config.gui())

         else when (args[0].lowercase()) {
             "help" ->  UChat.chat(getUsage())
             "sim" ->  if (args.size > 1) sendFakeChatMessage(args.copyOfRange(1, args.size).joinToString(" ")) else UChat.chat(getUsage())
             "edit" ->  openScreen(HudEditorScreen())
             "rotate" -> {
                 if (args.size > 2) {
                     val yaw = args[1].toDoubleOrNull() ?: return modMessage("&cInvalid yaw value")
                     val pitch = args[2].toDoubleOrNull() ?: return modMessage("&cInvalid pitch value")

                     rotateSmoothly(yaw.toFloat(), pitch.toFloat(), 2000)
                     modMessage("&dRotating to &b$yaw, $pitch")

                 }
                 else modMessage("&cInvalid usage of command. &bUsage: /na rotate [yaw] [pitch]")

             }
             "ras" ->  GlobalScope.launch { reaperSwap() }

             else -> openScreen(Config.gui())
         }

    }

    private fun getUsage() = """
        &b&m${getChatBreak()}
        &b/na &7- &oOpens the Settings GUI.
        &b/na help &7- &oShows this Message.
        &b/na sim [message] &7- &oSimulates a received chat message.
        &b/na edit &7- &oOpens the Edit Hud GUI.
        &b/na rotate [yaw] [pitch] &7- &oRotates the player to the specified yaw and pitch.
        &b/na ras &7- &oAutomatically preform Reaper Armor Swap.
        &b&m${getChatBreak()}
    """.trimIndent()

    private fun sendFakeChatMessage(message: String) {
		val formattedMessage = message.addColor()
        modMessage(formattedMessage)
        MinecraftForge.EVENT_BUS.post(ClientChatReceivedEvent(0.toByte(), ChatComponentText(formattedMessage)))
	    MinecraftForge.EVENT_BUS.post(Chat(ChatComponentText(formattedMessage)))
    }


}
