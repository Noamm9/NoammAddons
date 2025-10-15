@file:Suppress("UNUSED_PARAMETER")

package noammaddons.features.impl

import gg.essential.api.EssentialAPI
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.*
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.FeatureManager
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.impl.dungeons.solvers.devices.AutoI4.testI4
import noammaddons.features.impl.esp.StarMobESP
import noammaddons.features.impl.general.teleport.helpers.InstantTransmissionHelper
import noammaddons.test.ModernConfigGui
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.ButtonSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.dungeonEnded
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.DungeonUtils.puzzles
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.ItemUtils.toJsonElement
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.LocationUtils.world
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScanUtils.currentRoom
import noammaddons.utils.ScanUtils.getCore
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.remove
import java.awt.Color

@Dev
object DevOptions: Feature() {
    @JvmStatic
    val devMode by ToggleSetting("Dev Mode")

    @Suppress("unused")
    val copyFeatureList by ButtonSetting("Copy Feature List") {
        GuiScreen.setClipboardString(FeatureManager.createFeatureList())
        ChatUtils.Alert(message = "Copied all feaures to clipboard")
    }

    val updateChecker by ToggleSetting("Update Checker", true)

    @JvmStatic
    val clientBranding by ToggleSetting("Client Branding")

    @JvmStatic
    val isDev get() = EssentialAPI.getMinecraftUtil().isDevelopment() || enabled

    val trackTitles by ToggleSetting("Track Titles")

    val printBlockCoords by ToggleSetting("Print Block Coords")

    val printC08 by ToggleSetting("Print C08")

    val debugServerPlayer by ToggleSetting("Show ServerPlayer")


    private val titles = mutableListOf<String>()


    @SubscribeEvent
    fun adfd(event: ItemTooltipEvent) {
        if (! isDev) return
        val sbid = event.itemStack.skyblockID ?: return
        event.toolTip?.add("SkyblockID: &6$sbid".addColor())
    }

    @SubscribeEvent
    fun t(event: RenderOverlay) {
        if (! isDev) return

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
            roofHeight: ${currentRoom?.highestBlock ?: "&cUnknown&r"}
            rotation: ${currentRoom?.rotation ?: "&cUnknown&r"}
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

        puzzles.forEachIndexed { i, puzzle ->
            drawText(puzzle.name + ": " + puzzle.state, 350, 130 + (i * 9))
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onMessage(event: MessageSentEvent) {
        if (! isDev) return
        modMessage(event.message)

        when (event.message) {
            "i4" -> {
                event.isCanceled = true
                scope.launch { testI4() }
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
                ActionUtils.leap("Noamissad")
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
                StarMobESP.checked.clear()
                StarMobESP.starMobs.clear()
            }

            "scan" -> {
                event.isCanceled = true
                DungeonScanner.scan()
                SoundUtils.Pling()
            }

            "nbt" -> {
                event.isCanceled = true
                val held = mc.thePlayer?.heldItem ?: return
                val nbt = held.tagCompound?.toJsonElement() ?: return modMessage("No NBT")
                modMessage(nbt)
            }

            "fullbright" -> {
                event.isCanceled = true
                mc.gameSettings.gammaSetting = 100000f
            }

            "swap" -> {
                event.isCanceled = true
                ActionUtils.quickSwapTo(ServerPlayer.player.getHeldItem()?.skyblockID ?: return)
            }

            "secrets" -> {
                event.isCanceled = true
                scope.launch {
                    DungeonUtils.dungeonTeammates.toList().forEach { player ->
                        val s = ProfileUtils.getSecrets(player.name)
                        modMessage("${player.name} has $s secrets")
                    }
                }
            }

            "t" -> titles.forEach(ChatUtils::modMessage)

            "cri" -> {
                openScreen(ModernConfigGui())
                event.isCanceled = true
            }

            "uuid" -> {
                modMessage("getUUID: ${ProfileUtils.getUUID(mc.session.username)}")
                modMessage("session: ${mc.session.playerID.remove("-")}")
            }

            "fr" -> {
                modMessage("fastRender: " + mc.gameSettings::class.java.getField("ofFastRender").getBoolean(mc.gameSettings))
            }

            "scanrot" -> {
                event.isCanceled = true
                DungeonInfo.dungeonList.filterIsInstance<Room>().filterNot { it.isSeparator }.forEach {
                    if (it.rotation != null) return@forEach
                    it.findRotation()
                }
            }

            "id" -> mc.objectMouseOver?.blockPos?.let { modMessage(getBlockAt(it).getBlockId()) }

            "checked" -> StarMobESP.checked.forEach { modMessage(it.name) }

        }
    }

    @SubscribeEvent
    fun renderworld(event: RenderWorld) {
        if (! debugServerPlayer) return

        ServerPlayer.player.getVec()?.let { pos ->
            val boxColor = if (ServerPlayer.player.onGround == true) Color.GREEN else Color.RED
            drawBox(pos.xCoord - 0.1, pos.yCoord - 0.1, pos.zCoord - 0.1, boxColor, true, true, 0.2, 0.2)

            ServerPlayer.player.getRotation()?.let {
                val sneakOffset = if (ServerPlayer.player.sneaking) (1.62 - 0.08) else 1.62
                val a = InstantTransmissionHelper.Vector3.fromPitchYaw(it.pitch.toDouble(), it.yaw.toDouble()).multiply(10.0)
                draw3DLine(pos.add(y = sneakOffset), pos.add(a.x, a.y + sneakOffset, a.z), Color.CYAN)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun afad(event: Event) {
        when (event) {
            is PacketEvent.Received -> when (val packet = event.packet) {
                is S45PacketTitle -> {
                    if (! trackTitles) return
                    titles.add("type: ${packet.type}, msg: ${packet.message?.formattedText}")
                }
            }

            is PacketEvent.Sent -> when (val packet = event.packet) {
                is C08PacketPlayerBlockPlacement -> {
                    if (! printC08) return
                    val pos = packet.position ?: null
                    modMessage("C08 >> stack: ${packet.stack?.displayName + "&r"} pos: $pos block: ${pos?.let { getBlockAt(it).localizedName }}")
                }
            }

            is ClickEvent.RightClickEvent -> {
                if (! printBlockCoords) return
                if (! mc.thePlayer.isSneaking) return
                val pos = mc.objectMouseOver?.blockPos ?: return

                modMessage("{\"x\": ${pos.x}, \"y\": ${pos.y}, \"z\": ${pos.z}}")
                BlockUtils.toAir(pos)
            }

            else -> {}
        }
    }
}
