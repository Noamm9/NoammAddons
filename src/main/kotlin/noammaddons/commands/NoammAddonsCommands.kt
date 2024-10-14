package noammaddons.commands

import gg.essential.universal.UChat
import gg.essential.universal.UDesktop
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noammaddons.config.Config
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.features.General.AutoReaperArmorSwap.reaperSwap
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.copyToClipboard
import noammaddons.utils.ChatUtils.getCenteredText
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.sendFakeChatMessage
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.PlayerUtils.rotateSmoothly
import noammaddons.utils.ScanUtils.ScanRoom.currentRoom
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCenter
import noammaddons.utils.ScanUtils.Utils.getCore
import noammaddons.utils.UpdateUtils
import noammaddons.utils.Utils.isNull
import java.net.URI


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
             "sim" ->  if (args.size > 1) sendFakeChatMessage(args.copyOfRange(1, args.size).joinToString(" "))
                       else modMessage("&cInvalid Usage. &bUsage: /na sim [message]")
             "edit" ->  openScreen(HudEditorScreen())
             "rotate" -> {
                 if (args.size > 2) {
                     val yaw = args[1].toIntOrNull()?.toFloat() ?: return modMessage("&cInvalid yaw value")
                     val pitch = args[2].toIntOrNull()?.toFloat() ?: return modMessage("&cInvalid pitch value")
	                 val ms = args[3].toIntOrNull()?.toLong()

                     rotateSmoothly(yaw, pitch, ms ?: 1500)
	                 modMessage("""
						 &dRotating to &b$yaw, $pitch &din &b ${
							 if (ms.isNull()) "1.5"
							 else (ms!!/1000.0).toFixed(1)
						 } &dseconds
					 """.trimIndent())
	                
                 }
                 else modMessage("&cInvalid usage of command. &bUsage: /na rotate [yaw] [pitch] [ms?]")

             }
             "ras" ->  GlobalScope.launch { reaperSwap() }
	         "copy" ->  if (args.size > 1) copyToClipboard(args.copyOfRange(1, args.size).joinToString(" "))
	                    else modMessage("&cInvalid usage of command. &bUsage: /na copy [message]")
	         "roominfo" -> {
				 val roomCenter = getRoomCenter()
				 val text = """
				    roomName: ${currentRoom?.name}
				    roomCore: ${currentRoom?.cores.toString()} || ${getCore(roomCenter.x, roomCenter.z)}
				    roomType: ${currentRoom?.type}
				    roomShape: ${currentRoom?.shape}
				    roomDoors: ${currentRoom?.doors}
				    roomTotalSecrets: ${currentRoom?.secrets}
				    roomSecretsTypes: ${currentRoom?.secret_details}
				    roomSecretsCoords: ${currentRoom?.secret_coords.toString()}
				    roomCrypts: ${currentRoom?.crypts}
			    """.trimIndent()
		         
		         copyToClipboard(text)
		         clickableChat("&aRoom info copied to clipboard. &6(&fHover&6)&r", "", "&b&l$text")
	         }
	         "update" -> UpdateUtils.update()
	         "openlink" -> {
				 val link =
					 if (args.size > 1) args.copyOfRange(1, args.size).joinToString(" ")
					 else return modMessage("&cInvalid usage of command. &bUsage: /na openlink [URL]")
		    
				 UDesktop.browse(URI(link))
	         }
             else -> openScreen(Config.gui())
         }

    }

    private fun getUsage() = """&b&m${getChatBreak()?.substring(1)}
${getCenteredText("$FULL_PREFIX&r")}

&b/na &7- &oOpens the Settings GUI.
&b/na help &7- &oShows this Message.
&b/na sim [message] &7- &oSimulates a received chat message.
&b/na edit &7- &oOpens the Edit Hud GUI.
&b/na rotate [yaw] [pitch] [ms?] &7- &oRotates the player to the specified yaw and pitch.
&b/na ras &7- &oAutomatically preform Reaper Armor Swap.
&b/na copy [message] &7- &oCopies the message to the clipboard.
&b/na roominfo &7- &oShows current room info.
&b/na update &7- &oChecks for an Update.
&b/na openlink [URL] &7- &oOpens the provided URL in a web browser.

&b&m${getChatBreak()?.substring(1)}""".trimIndent().trim()

}