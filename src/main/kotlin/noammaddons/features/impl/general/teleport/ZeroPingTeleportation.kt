package noammaddons.features.impl.general.teleport

import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.general.teleport.core.*
import noammaddons.features.impl.general.teleport.helpers.EtherwarpHelper
import noammaddons.features.impl.general.teleport.helpers.InstantTransmissionHelper
import noammaddons.ui.config.core.impl.SeperatorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ItemUtils.extraAttributes
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.LocationUtils.WorldType.*
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send
import kotlin.math.abs


object ZeroPingTeleportation: Feature("Instantly Teleport without waiting for the server's teleport packet.") {
    private val aote by ToggleSetting("Instant Transmission")
    private val etherwarp by ToggleSetting("Etherwarp")
    private val witherImpact by ToggleSetting("Wither Impact")
    private val _s by SeperatorSetting("Keep Motion").addDependency { ! aote && ! etherwarp && ! witherImpact }
    private val aoteKm by ToggleSetting("Instant Transmission ").addDependency { ! aote }
    private val etherwarpKm by ToggleSetting("Etherwarp ").addDependency { ! etherwarp }
    private val witherImpactKm by ToggleSetting("Wither Impact ").addDependency { ! witherImpact }

    private val interactableBlocks = setOf(146, 54, 130, 154, 118, 69, 77, 143, 96, 167)

    private const val MAX_PENDING_TELEPORTS = 3
    private const val MAX_FAILED_TELEPORTS = 3
    private const val FAIL_TIMEOUT = 20_000L

    private val withinTolerance = fun(n1: Float, n2: Float) = abs(n1 - n2) < 1e-4
    private val pendingTeleports = mutableListOf<TeleportPrediction>()
    private val failedTeleports = mutableListOf<Long>()

    private var witherImpactLastUse = System.currentTimeMillis()
    private var hasSoulFlow = true


    private fun teleport(prediction: TeleportPrediction) = ThreadUtils.scheduledTask(0) {
        val (x, y, z) = prediction.position.destructured()
        val (yaw, pitch) = prediction.rotation
        val keepMotion = prediction.info.keepMotion

        mc.thePlayer.setPosition(x, y, z)
        C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, mc.thePlayer.onGround).send()
        if (keepMotion) mc.thePlayer.motionY = .0
        else mc.thePlayer.setVelocity(.0, .0, .0)
    }

    private fun onTeleportFail() {
        val id = System.currentTimeMillis()
        failedTeleports.add(id)
        ThreadUtils.setTimeout(FAIL_TIMEOUT) { failedTeleports.remove(id) }
        pendingTeleports.clear()
        modMessage("&bZPT >> &cFailed &e${failedTeleports.size}/$MAX_FAILED_TELEPORTS")
        if (failedTeleports.size >= MAX_FAILED_TELEPORTS) {
            modMessage(
                "&bZPT >> &cDetected &6${failedTeleports.size}&c failed teleports. Stopping the feature for &e${
                    (FAIL_TIMEOUT - (System.currentTimeMillis() - failedTeleports[0]) / 1000) / 1000
                } &fseconds."
            )
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onC08PacketPlayerBlockPlacement(event: PacketEvent.Sent) {
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        if (packet.placedBlockDirection != 255) return
        if (pendingTeleports.size == MAX_PENDING_TELEPORTS) return
        if (LocationUtils.world.equalsOneOf(Home, Garden)) return
        if (LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss) return
        if (mc.thePlayer.isRiding) return
        if (ActionBarParser.currentMana < ActionBarParser.maxMana * 0.1) return
        if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return
        runCatching { if ((mc.objectMouseOver.blockPos?.let { getBlockAt(it).getBlockId() } ?: 0) in interactableBlocks) return }
        if (LocationUtils.isInHubCarnival()) return
        val tpInfo = getTeleportInfo(packet) ?: return

        when (tpInfo.type) {
            TeleportType.Etherwarp -> doZeroPingEtherwarp(tpInfo)
            TeleportType.InstantTransmission -> doZeroPingInstantTransmission(tpInfo)
            TeleportType.WitherImpact -> doZeroPingWitherImpact(tpInfo)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onS08PacketPlayerPosLook(event: PacketEvent.Received) {
        val packet = event.packet as? S08PacketPlayerPosLook ?: return
        if (pendingTeleports.isEmpty()) return
        val prediction = pendingTeleports.removeFirst()

        val isMatchingPacket = run {
            val yaw = withinTolerance(prediction.rotation.yaw, packet.yaw) || packet.yaw == 0f
            val pitch = withinTolerance(prediction.rotation.pitch, packet.pitch) || packet.pitch == 0f
            val x = packet.x == prediction.position.xCoord
            val y = packet.y == prediction.position.yCoord
            val z = packet.z == prediction.position.zCoord
            yaw && pitch && x && y && z
        }

        if (! isMatchingPacket) onTeleportFail()
        else event.isCanceled = true
    }


    @SubscribeEvent
    fun onChat(event: Chat) = with(event.component.noFormatText) {
        if (! etherwarp) return
        if (lowercase() != "not enough soulflow!") return
        hasSoulFlow = false
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        hasSoulFlow = true
    }

    private fun doZeroPingEtherwarp(tpInfo: TeleportInfo) {
        val playerPos = ServerPlayer.player.getVec() ?: return
        val playerRot = ServerPlayer.player.getRotation() ?: return
        val etherPos = EtherwarpHelper.getEtherPos(playerPos, playerRot, tpInfo.distance).takeIf { it.succeeded && it.pos != null } ?: return

        if (ScanUtils.getRoomFromPos(etherPos.pos !!)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        playerRot.yaw %= 360
        if (playerRot.yaw < 0) playerRot.yaw += 360

        val prediction = TeleportPrediction(playerRot, etherPos.vec !!.add(0.5, 1.05, 0.5), tpInfo)
        if (! mc.isSingleplayer) pendingTeleports.add(prediction)
        teleport(prediction)
    }

    private fun doZeroPingInstantTransmission(tpInfo: TeleportInfo) {
        val playerPos = ServerPlayer.player.getVec() ?: return
        val playerRot = ServerPlayer.player.getRotation() ?: return

        val pos = InstantTransmissionHelper.predictTeleport(tpInfo.distance, playerPos, playerRot) ?: return
        if (ScanUtils.getRoomFromPos(pos)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        playerRot.yaw %= 360

        val prediction = TeleportPrediction(playerRot, pos, tpInfo)
        if (! mc.isSingleplayer) pendingTeleports.add(prediction)
        teleport(prediction)
    }

    private fun doZeroPingWitherImpact(tpInfo: TeleportInfo) {
        if (System.currentTimeMillis() - witherImpactLastUse <= 125) return // 8 CPS limit
        witherImpactLastUse = System.currentTimeMillis()
        doZeroPingInstantTransmission(tpInfo)
    }


    private fun getTeleportInfo(packet: C08PacketPlayerBlockPlacement): TeleportInfo? {
        val heldItem = packet.stack ?: return null
        val sbId = heldItem.skyblockID ?: return null

        if (sbId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val nbt = heldItem.extraAttributes
            val tuners = nbt?.getByte("tuned_transmission")?.toInt() ?: 0
            if (ServerPlayer.player.sneaking && nbt?.getByte("ethermerge") == 1.toByte()) {
                if (! etherwarp) return null
                if (! hasSoulFlow) return null

                return TeleportInfo(
                    distance = 57.0 + tuners,
                    type = TeleportType.Etherwarp,
                    keepMotion = etherwarpKm
                )
            }
            else {
                if (! aote) return null
                return TeleportInfo(
                    distance = 8.0 + tuners,
                    type = TeleportType.InstantTransmission,
                    keepMotion = aoteKm
                )
            }
        }
        else if (PlayerUtils.isHoldingWitherImpact(heldItem)) {
            if (! witherImpact) return null
            return TeleportInfo(
                distance = 10.0,
                type = TeleportType.WitherImpact,
                keepMotion = witherImpactKm
            )
        }
        return null
    }
}