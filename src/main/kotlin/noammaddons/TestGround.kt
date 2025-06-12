package noammaddons

import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.*
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.events.*
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.impl.dungeons.solvers.devices.AutoI4.testi4
import noammaddons.features.impl.esp.StarMobESP
import noammaddons.noammaddons.Companion.ahData
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.ui.clickgui.ClickGuiScreen
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.dungeonEnded
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.LocationUtils.world
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils2D.modelViewMatrix
import noammaddons.utils.RenderUtils2D.projectionMatrix
import noammaddons.utils.RenderUtils2D.viewportDims
import noammaddons.utils.ScanUtils.currentRoom
import noammaddons.utils.ScanUtils.getCore
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.send
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3


object TestGround {
    private val titles = mutableListOf<String>()
    private var fuckingBitch = false
    private var sent = false
    private var a = false

    @SubscribeEvent
    fun adfd(event: ItemTooltipEvent) {
        if (! DevOptions.devMode) return
        val sbid = event.itemStack.SkyblockID ?: return
        event.toolTip?.add("SkyblockID: &6$sbid".addColor())
    }

    @SubscribeEvent
    @Suppress("Unused_parameter")
    fun t(event: RenderOverlay) {
        if (! DevOptions.isDev) return

        GlStateManager.pushMatrix()
        val scale = 2f / mc.getScaleFactor()
        GlStateManager.scale(scale, scale, scale)

        drawText(
            listOf(
                "dungeonStarted: $dungeonStarted",
                "dungeonEnded: $dungeonEnded",
                "indungeons: $inDungeon",
                "dungeonfloor: $dungeonFloor",
                "dungeonfloorNumber: $dungeonFloorNumber",
                "inboss: $inBoss",
                "inSkyblock: $inSkyblock",
                "onHypixel: $onHypixel",
                "F7Phase: $F7Phase",
                "P3Section: $P3Section",
                "WorldName: $world"
            ).joinToString("\n"),
            200f, 10f
        )

        val rc = getRoomCenterAt(mc.thePlayer.position)

        drawText(
            """
			getCore: ${getCore(rc.x, rc.z)}
			currentRoom: ${currentRoom?.data?.name ?: "&cUnknown&r"}
			getRoomCenter: $rc
		""".trimIndent(),
            150f, 130f, 1f,
            Color.CYAN
        )

        drawText(
            """Party Leader: ${PartyUtils.leader}
				| Party Size: ${PartyUtils.size}
				| Party Members: ${"\n" + PartyUtils.members.entries.joinToString("\n")}
			""".trimMargin(),
            20f, 200f, 1f,
            Color.PINK
        )

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onMessage(event: MessageSentEvent) {
        if (! DevOptions.isDev) return
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
                event.isCanceled = true
                modMessage(
                    mc.theWorld.loadedEntityList
                        .filterIsInstance<EntityArmorStand>()
                        .filter { it.getDistanceToEntity(mc.thePlayer) < 5 }
                )
            }

            "leap" -> {
                event.isCanceled = true
                ActionUtils.leap("WebbierAmoeba0")
            }

            "ah" -> {
                event.isCanceled = true
                modMessage(ahData["TERMINATOR"]?.toInt())
            }

            "test" -> {
                event.isCanceled = true
                scope.launch {
                    registerFeatures()
                }
                SoundUtils.chipiChapa()
            }

            "esp" -> {
                event.isCanceled = true
                StarMobESP.starMobs.clear()
                StarMobESP.checked.clear()
            }

            "scan" -> {
                event.isCanceled = true
                DungeonScanner.scan()
                SoundUtils.Pling()
            }

            "nbt" -> {
                event.isCanceled = true
                val held = mc.thePlayer?.heldItem ?: return
                val nbt = held.getSubCompound("ExtraAttributes", false) ?: return
                modMessage(nbt)
            }

            "fullbright" -> {
                event.isCanceled = true
                mc.gameSettings.gammaSetting = 100000f
            }

            "swap" -> {
                event.isCanceled = true
                ActionUtils.quickSwapTo(ServerPlayer.player.getHeldItem()?.SkyblockID ?: return)
            }

            "secrets" -> {
                event.isCanceled = true
                CoroutineScope(Dispatchers.IO).launch {
                    DungeonUtils.dungeonTeammates.toList().forEach { player ->
                        val s = ProfileUtils.getSecrets(player.name)
                        modMessage("${player.name} has $s secrets")
                    }
                }
            }

            "t" -> titles.forEach(::modMessage)

            "cri" -> {
                openScreen(ClickGuiScreen)
                event.isCanceled = true
            }

            "uuid" -> {
                modMessage("getUUID: ${ProfileUtils.getUUID(mc.session.username)}")
                modMessage("session: ${mc.session.playerID.remove("-")}")
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
        setTimeout(500) {
            if (mc.currentScreen !is GuiDownloadTerrain) return@setTimeout
            mc.currentScreen = null
        }
    }

    @SubscribeEvent
    fun fucklocraw(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        if (event.isLocal) return
        fuckingBitch = true
        setTimeout(5000) { fuckingBitch = false }
    }

    @SubscribeEvent
    fun fuckLocraw2(event: PacketEvent.Sent) {
        if (! fuckingBitch) return
        val packet = event.packet as? C01PacketChatMessage ?: return
        if (packet.message != "/locraw") return
        debugMessage("Cancelling /locraw")
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlaterInteract(e: AttackEntityEvent) {
        if (! inDungeon) return
        if (e.entityPlayer != mc.thePlayer) return
        if (DungeonUtils.dungeonTeammates.none { it.entity == e.target }) return
        e.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun afad(event: Event) {
        when (event) {
            is WorldUnloadEvent -> ScoreboardUtils.sidebarLines = emptyList()
            is PacketEvent.Received -> with(event.packet) {
                if (! DevOptions.trackTitles) return
                if (this !is S45PacketTitle) return@with
                titles.add("type: $type, msg: ${message?.formattedText}")
            }

            is ClickEvent.RightClickEvent -> {
                if (! DevOptions.printBlockCoords) return
                if (! mc.thePlayer.isSneaking) return
                val pos = mc.objectMouseOver?.blockPos ?: return

                modMessage("{\"x\": ${pos.x}, \"y\": ${pos.y}, \"z\": ${pos.z}}")
                BlockUtils.toAir(pos)
            }

            else -> {}
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        val (x, y, z) = mc.thePlayer?.renderVec?.destructured() ?: return
        GlStateManager.pushMatrix()
        GlStateManager.translate(- x, - y, - z)

        glGetFloat(GL_MODELVIEW_MATRIX, modelViewMatrix)
        glGetFloat(GL_PROJECTION_MATRIX, projectionMatrix)

        GlStateManager.popMatrix()
        glGetInteger(GL_VIEWPORT, viewportDims)
    }
}