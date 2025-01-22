// this is where I checked stuff before making them into a feature
// apart from that useless file

package noammaddons

import gg.essential.api.EssentialAPI
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.MessageSentEvent
import noammaddons.events.RenderOverlay
import noammaddons.events.WorldLoadPostEvent
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.dungeons.AutoI4.testi4
import noammaddons.noammaddons.Companion.ahData
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.ActionUtils
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.LocationUtils.WorldName
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.PartyUtils.partyLeader
import noammaddons.utils.PartyUtils.partyMembers
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderUtils.drawLine
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScanUtils.ScanRoom.getRoom
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCenter
import noammaddons.utils.ScanUtils.ScanRoom.getRoomComponent
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCorner
import noammaddons.utils.ScanUtils.Utils.getCore
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send
import java.awt.Color


object TestGround {
    private var a = false
    private var sent = false

    @SubscribeEvent
    fun t(e: RenderOverlay) {
        if (! (config.DevMode || EssentialAPI.getMinecraftUtil().isDevelopment())) return
        GlStateManager.pushMatrix()
        val scale = 2f / mc.getScaleFactor()
        GlStateManager.scale(scale, scale, scale)


        drawText(
            "indungeons: $inDungeons \n dungeonfloor: $dungeonFloor \n inboss: $inBoss \n inSkyblock: $inSkyblock \n onHypixel: $onHypixel \n F7Phase: $F7Phase \n P3Section: $P3Section \n WorldName: $WorldName",
            200f, 10f
        )

        drawText(
            """
			getCore: ${getCore(getRoomCenter().x, getRoomCenter().z)}
			currentRoom: ${getRoom()?.name}
			getRoomComponent: ${getRoomComponent()}
			getRoomCorner: ${getRoomCorner()}
			getRoomCenter: ${getRoomCenter()}
		""".trimIndent(),
            100f, 100f, 1.5f,
            Color.CYAN
        )


        drawLine(Color.CYAN, 0f, 0f, 100f, 100f, 3f)

        drawText(
            """Party Leader: $partyLeader
				| Party Size: ${partyMembers.size}
				| Party Members: ${
                partyMembers.joinToString("\n") {
                    "(name: ${it.first} || hasEntity: ${it.second != null})"
                }
            }
			""".trimMargin(),
            20f, 200f, 1.5f,
            Color.PINK
        )
        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onMessage(event: MessageSentEvent) {
        if (! (config.DevMode || EssentialAPI.getMinecraftUtil().isDevelopment())) return

        modMessage(event.message)

        when (event.message) {
            "i4" -> {
                event.isCanceled = true
                scope.launch {
                    testi4()
                }
            }

            "chat" -> {
                event.isCanceled = true
                setTimeout(1000) { sendChatMessage("/play sb") }
            }

            "e" -> {
                println(
                    mc.theWorld.loadedEntityList
                        .filterIsInstance<EntityArmorStand>()
                        .filter { it.getDistanceToEntity(mc.thePlayer) < 5 }
                )
            }

            "leap" -> {
                event.isCanceled = true
                ActionUtils.leap("noamm9")
            }

            "ah" -> {
                event.isCanceled = true
                modMessage(ahData["TERMINATOR"]?.toInt())
            }

            "test" -> {
                event.isCanceled = true
                scope.launch { registerFeatures() }
            }

            "slayer" -> {
                event.isCanceled = true
            }
        }
    }


    @SubscribeEvent
    fun handlePartyCommands(event: MessageSentEvent) {
        if (! onHypixel) return
        val msg = event.message.removeFormatting().lowercase()

        if (msg == "/p invite accept") {
            event.isCanceled = true
            a = true
            sent = false
            setTimeout(250) { a = false }
            modMessage("&bAccepting Bugged Party Invite...")
            return
        }

        if (a && ! sent) {
            if (msg.startsWith("/p invite ") || msg.startsWith("/party accept ")) {
                event.isCanceled = true

                val modifiedMessage = msg
                    .replace("/party accept ", "/p join ")
                    .replace("/p invite ", "/p join ")

                sendChatMessage(modifiedMessage)
                sent = true
            }
        }

        if (msg.equalsOneOf("/pll", "/pl")) {
            event.isCanceled = true
            C01PacketChatMessage("/pl").send()
        }
    }

    @SubscribeEvent
    fun wtf(event: WorldLoadPostEvent) {
        registerFeatures(false)
        setTimeout(500) {
            if (mc.currentScreen is GuiDownloadTerrain) {
                mc.currentScreen = null
            }
        }
    }


    @SubscribeEvent
    fun afad(event: Event) {
    }
}