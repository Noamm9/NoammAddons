// this is where I checked stuff before making them into a feature
// apart from that useless file
// may also contain some silent features

package noammaddons

import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat
import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.*
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.events.*
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.dungeons.esp.StarMobESP
import noammaddons.features.dungeons.solvers.devices.AutoI4.testi4
import noammaddons.noammaddons.Companion.ahData
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.dungeonEnded
import noammaddons.utils.DungeonUtils.dungeonStarted
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
import noammaddons.utils.Utils.send
import org.lwjgl.opengl.GL11
import java.awt.Color


object TestGround {
    private var a = false
    private var sent = false

    @SubscribeEvent
    fun adfd(event: ItemTooltipEvent) {
        if (! (config.DevMode || EssentialAPI.getMinecraftUtil().isDevelopment())) return
        val sbid = event.itemStack.SkyblockID ?: return
        event.toolTip?.add("SkyblockID: &6$sbid".addColor())
    }

    @SubscribeEvent
    @Suppress("Unused_parameter")
    fun t(event: RenderOverlay) {
        if (! (config.DevMode || EssentialAPI.getMinecraftUtil().isDevelopment())) return
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
            150f, 100f, 1f,
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

            "cri" -> {
                listOf(
                    "&6&l[&b&lN&d&lA&6&l]&r &e[B&e]&r &6Noamm&f:&r &r&64-5 Rooms&r&r &f| &r&r&b15 Secrets&r&r &f|&r&r &c0 Deaths&r",
                    "&6&l[&b&lN&d&lA&6&l]&r &e[A&e]&r &4DontAskHoax&f:&r &r&61-4 Rooms&r&r &f|&r &r&b1 Secrets&r&r &f| &r&r &c0 Deaths&r",
                    "&6&l[&b&lN&d&lA&6&l]&r &e[T&e]&r &2ddavid9&f:&r &r&63-5 Rooms&r&r &f|&r &r&b7 Secrets&r&r &f|&r &r&c0 Deaths&r",
                    "&6&l[&b&lN&d&lA&6&l]&r &e[H&e]&r &5saintszdxdss&f:&r &r&65 Rooms&r&r &f|&r&r &b3 Secrets&r&r &f|&r&r &c0 Deaths&r",
                    "&6&l[&b&lN&d&lA&6&l]&r &e[M&e]&r &3Chastille&f:&r &r&61-4 Rooms&r&r &f|&r&r &b4 Secrets&r&r &f|&r&r &c0 Deaths&r",
                ).forEach(UChat::chat)
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
        registerFeatures(false)
        setTimeout(500) {
            if (mc.currentScreen !is GuiDownloadTerrain) return@setTimeout
            mc.currentScreen = null
        }
    }

    var fuckingBitch = false

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

    @SubscribeEvent
    fun afad(event: Event) {
        if (event is WorldUnloadEvent) {
            ScoreboardUtils.sidebarLines = emptyList()
        }

        /*
                if (event is ClickEvent.RightClickEvent) {
                    if (! mc.thePlayer.isSneaking) return
                    val pos = mc.objectMouseOver?.blockPos ?: return

                    modMessage("{\"x\": ${pos.x}, \"y\": ${pos.y}, \"z\": ${pos.z}}")
                    BlockUtils.toAir(pos)
                }*/
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        val (x, y, z) = mc.thePlayer?.renderVec?.destructured() ?: return
        GlStateManager.pushMatrix()
        GlStateManager.translate(- x, - y, - z)

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix)
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix)

        GlStateManager.popMatrix()
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportDims)
    }
}

/*
object ghg: Feature() {
    var showWishMessage = false
    var wishTimer = 0

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! showWishMessage) return
        drawCenteredText(
            config.ItemRarityColor.toString(),
            mc.getWidth() / 2f,
            mc.getHeight() / 2f - mc.getHeight() / 4f
        )
    }


    val wishMessages = listOf(
        "⚠ Maxor is enraged! ⚠",
        "[BOSS] Goldor: You have done it, you destroyed the factory…"
    )

    @SubscribeEvent
    fun onChat(event: Chat) {
        val message = event.component.noFormatText
        if (! config.healerWish || config.healerWishTitle.isEmpty()) return
        if (message in wishMessages) {
            showWishMessage = true
            wishTimer = 10
            return
        }

        if (message.matches(Regex(".+ Wish healed you for .+ health and granted you an absorption shield with .+ health!"))) {
            showWishMessage = false
            wishTimer = 0
        }
    }

    init {
        loop(1000) {
            if (! showWishMessage) return@loop
            if (wishTimer > 0) wishTimer -= 1
            showWishMessage = wishTimer >= - 1
        }
    }
}
*/
