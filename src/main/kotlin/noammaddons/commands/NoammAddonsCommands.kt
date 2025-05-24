package noammaddons.commands

import gg.essential.universal.UChat
import gg.essential.universal.UDesktop.browse
import net.minecraft.client.gui.GuiScreen
import net.minecraft.command.ICommandSender
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.features.impl.dungeons.AutoPotion.potionName
import noammaddons.features.impl.hud.TpsDisplay.getTps
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.ui.config.ConfigGUI
import noammaddons.utils.ActionUtils.changeMask
import noammaddons.utils.ActionUtils.getPotion
import noammaddons.utils.ActionUtils.leap
import noammaddons.utils.ActionUtils.quickSwapTo
import noammaddons.utils.ActionUtils.reaperSwap
import noammaddons.utils.ActionUtils.rodSwap
import noammaddons.utils.ActionUtils.rotateSmoothly
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.getCenteredText
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.sendFakeChatMessage
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.ScanUtils.currentRoom
import noammaddons.utils.ScanUtils.getCore
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.UpdateUtils
import noammaddons.utils.Utils.openDiscordLink
import java.net.URI


object NoammAddonsCommands: Command("na", listOf("noammaddons, noamm, noam, noamaddons")) {
    override fun getCommandUsage(sender: ICommandSender): String {
        return Usage()
    }

    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        if (args.isEmpty()) openScreen(ConfigGUI)
        else when (args[0].lowercase()) {

            "help" -> UChat.chat(Usage())

            "sim" -> if (args.size > 1) sendFakeChatMessage(args.copyOfRange(1, args.size).joinToString(" "))
            else modMessage("&cInvalid Usage. &bUsage: /na sim [message]")

            "edit" -> openScreen(HudEditorScreen)

            "rotate" -> {
                val yaw: Float
                val pitch: Float
                val ms: Long?

                when (args.size) {
                    3 -> {
                        yaw = args[1].toIntOrNull()?.toFloat() ?: return modMessage("&cInvalid yaw value")
                        pitch = args[2].toIntOrNull()?.toFloat() ?: return modMessage("&cInvalid pitch value")
                        ms = null
                    }

                    4 -> {
                        yaw = args[1].toIntOrNull()?.toFloat() ?: return modMessage("&cInvalid yaw value")
                        pitch = args[2].toIntOrNull()?.toFloat() ?: return modMessage("&cInvalid pitch value")
                        ms = args[3].toIntOrNull()?.toLong()
                    }

                    else -> return modMessage("&cInvalid usage of command. &bUsage: /na rotate [yaw] [pitch] [ms?]")
                }

                rotateSmoothly(Rotation(yaw, pitch), ms ?: 1500L)
                modMessage(
                    """
						&dRotating to &b$yaw, $pitch &din &b${
                        if (ms == null) "1.5"
                        else (ms / 1000.0).toFixed(1)
                    } &dseconds
					 """.trimIndent()
                )
            }

            "ras" -> reaperSwap()

            "copy" -> if (args.size > 1) GuiScreen.setClipboardString(args.copyOfRange(1, args.size).joinToString(" "))
            else modMessage("&cInvalid usage of command. &bUsage: /na copy [message]")

            "roominfo" -> {
                val rc = getRoomCenterAt(mc.thePlayer.position)
                val text = """
				    roomName: ${currentRoom?.data?.name ?: "&cUnknown&r"}
				    roomCore: ${currentRoom?.data?.cores} || ${getCore(rc.x, rc.z)}
				    roomType: ${currentRoom?.data?.type?.name ?: "&cUnknown&r"}
				    roomTotalSecrets: ${currentRoom?.data?.secrets ?: 0}
				    roomCrypts: ${currentRoom?.data?.crypts ?: 0}
			    """.trimIndent()

                GuiScreen.setClipboardString(text)
                clickableChat("&aRoom info copied to clipboard. &6(&fHover&6)&r", "", "&b&l$text")
            }

            "update" -> UpdateUtils.update()

            "openlink" -> browse(
                URI(
                    if (args.size > 1) args.copyOfRange(1, args.size).joinToString("")
                    else return modMessage("&cInvalid usage of command. &bUsage: /na openlink [URL]")
                )
            )

            "tps" -> modMessage(getTps())

            "potion" -> getPotion(
                if (args.size > 1) args.copyOfRange(1, args.size).joinToString(" ")
                else potionName
            )

            "discord" -> openDiscordLink()

            "swapmask" -> changeMask()

            "rodswap" -> rodSwap()

            "quickswap" -> quickSwapTo(args.getOrNull(1)?.uppercase() ?: return)

            "leap" -> {
                if (args.size <= 1) return modMessage("&cInvalid usage of command. &bUsage: /na leap [name]")

                leap(args[1].lowercase())
            }

            "holdclick" -> {
                val type = args.getOrNull(1)?.uppercase() ?: "RIGHT"

                if (type !in listOf("RIGHT", "LEFT", "MIDDLE")) return modMessage("&cInvalid usage of command. &bUsage: /na holdclick [LEFT, RIGHT, MIDDLE]")
                holdClick(true, type)
            }

            else -> modMessage("&cInvalid usage of command. &bUsage: /na <command>")
        }
    }

    fun Usage(): String {
        val separator = "&b&m${getChatBreak()?.substring(1)}\n"

        return buildString {
            append(separator)
            append(getCenteredText("$FULL_PREFIX&r")).append("\n\n")
            commandsList.forEach { append(it).append("\n") }
            append("\n").append(separator)
        }
    }

    val commandsList = listOf(
        "&b/na &7- &oOpens the Settings GUI.",
        "&b/na help &7- &oShows this Message.",
        "&b/na sim [message] &7- &oSimulates a received chat message.",
        "&b/na edit &7- &oOpens the Edit Hud GUI.",
        "&b/na rotate [yaw] [pitch] [ms?] &7- &oRotates the player to the specified yaw and pitch.",
        "&b/na ras &7- &oAutomatically preform Reaper Armor Swap.",
        "&b/na leap [name] &7- &oLeaps to the specified player.",
        "&b/na swapmask &7- &oAutomatically preform Mask Swap.",
        "&b/na rodswap &7- &oAutomatically preform Rod Swap.",
        "&b/na quickswap [ItemID] &7- &oAutomatically equip the item in the EQ menu",
        "&b/na holdclick [LEFT, RIGHT, MIDDLE] &7- &oHolds the Mouse buttom for you.",
        "&b/na potion &7- &oAutomatically get a Potion from the Potion Bag.",
        "&b/na tps &7- &oShows the current server's TPS.",
        "&b/na copy [message] &7- &oCopies the message to the clipboard.",
        "&b/na roominfo &7- &oShows current room info.",
        "&b/na update &7- &oChecks for an Update.",
        "&b/na openlink [URL] &7- &oOpens the provided URL in a web browser.",
        "&b/na discord &7- &oOpens link to my Discord Server."
    )
}