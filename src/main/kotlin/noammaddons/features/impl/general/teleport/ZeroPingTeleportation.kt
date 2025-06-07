package noammaddons.features.impl.general.teleport

import net.minecraft.init.Blocks.*
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.DevOptions
import noammaddons.ui.config.core.impl.SeperatorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send
import kotlin.math.abs


object ZeroPingTeleportation: Feature("Instantly Teleport without waiting for the server's teleport packet.") {
    data class TeleportPrediction(val rotation: MathUtils.Rotation, val position: Vec3)
    data class TeleportInfo(val distance: Double, val type: Types, val keepMotion: Boolean = false) {
        companion object {
            enum class Types { Etherwarp, InstantTransmission, WitherImpact }
        }
    }

    private const val MAX_PENDING_TELEPORTS = 3
    private const val MAX_FAILED_TELEPORTS = 3
    private const val FAIL_TIMEOUT = 30_000L

    private val withinTolerance = fun(n1: Float, n2: Float) = abs(n1 - n2) < 1e-4
    private val pendingTeleports = mutableListOf<TeleportPrediction>()
    private val failedTeleports = mutableListOf<Long>()
    private var hasSoulFlow = true

    private var witherImpactLastUse = System.currentTimeMillis()

    private val aote = ToggleSetting("Instant Transmission")
    private val etherwarp = ToggleSetting("Etherwarp")
    private val witherImpact = ToggleSetting("Wither Impact")
    private val s = SeperatorSetting("Keep Motion").addDependency { ! aote.value && ! etherwarp.value && ! witherImpact.value }
    private val aoteKm = ToggleSetting("Instant Transmission ").addDependency(aote)
    private val etherwarpKm = ToggleSetting("Etherwarp ").addDependency(etherwarp)
    private val witherImpactKm = ToggleSetting("Wither Impact ").addDependency(witherImpact)

    override fun init() = addSettings(aote, etherwarp, witherImpact, s, aoteKm, etherwarpKm, witherImpactKm)

    private fun teleport(prediction: TeleportPrediction, keepMotion: Boolean) = ThreadUtils.scheduledTask(0) {
        val (x, y, z) = prediction.position.destructured()
        val (yaw, pitch) = prediction.rotation

        mc.thePlayer.setPosition(x, y, z)
        C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, mc.thePlayer.onGround).send()
        if (! keepMotion) mc.thePlayer.setVelocity(.0, .0, .0)
        else mc.thePlayer.setVelocity(mc.thePlayer.motionX, .0, mc.thePlayer.motionZ)
    }

    private fun onTeleportFail() {
        val id = System.currentTimeMillis()
        failedTeleports.add(id)
        ThreadUtils.setTimeout(FAIL_TIMEOUT) { failedTeleports.remove(id) }
        pendingTeleports.clear()
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
        if (failedTeleports.size == MAX_FAILED_TELEPORTS) return
        if (LocationUtils.world == LocationUtils.WorldType.Home) return
        if (LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss) return
        if (ActionBarParser.currentMana < ActionBarParser.maxMana * 0.1) return
        if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return
        if (getBlockAt(packet.position).equalsOneOf(trapped_chest, chest, ender_chest, hopper)) return
        if (LocationUtils.isInHubCarnival()) return
        val tpInfo = getTeleportInfo(packet) ?: return

        when (tpInfo.type) {
            TeleportInfo.Companion.Types.Etherwarp -> doZeroPingEtherwarp(tpInfo)
            TeleportInfo.Companion.Types.InstantTransmission -> doZeroPingInstantTransmission(tpInfo)
            TeleportInfo.Companion.Types.WitherImpact -> doZeroPingWitherImpact(tpInfo)
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

            if (DevOptions.devMode) {
                modMessage("yaw: $yaw")
                modMessage("pitch: $pitch")
                modMessage("x: $x")
                modMessage("y: $y")
                modMessage("z: $z")
            }

            yaw && pitch && x && y && z
        }

        if (! isMatchingPacket) onTeleportFail()
        else event.isCanceled = true
    }


    @SubscribeEvent
    fun onChat(event: Chat) = with(event.component.noFormatText) {
        if (! etherwarp.value) return
        if (lowercase() != "not enough soulflow!") return
        hasSoulFlow = false
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        hasSoulFlow = true
    }

    private fun doZeroPingEtherwarp(tpInfo: TeleportInfo) {
        val playerPos = ServerPlayer.player.getVec()
        val playerRot = ServerPlayer.player.getRotation()
        val etherPos = EtherwarpHelper.getEtherPos(playerPos, playerRot, tpInfo.distance)

        if (! etherPos.succeeded) return
        val pos = etherPos.pos ?: return

        if (getBlockAt(pos).equalsOneOf(chest, ender_chest, trapped_chest)) return
        if (ScanUtils.getRoomFromPos(pos)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        playerRot.yaw %= 360
        if (playerRot.yaw < 0) playerRot.yaw += 360

        val prediction = TeleportPrediction(playerRot, etherPos.vec !!.add(0.5, 1.05, 0.5))
        if (! mc.isSingleplayer) pendingTeleports.add(prediction)
        teleport(prediction, tpInfo.keepMotion)
    }

    private fun doZeroPingInstantTransmission(tpInfo: TeleportInfo) {
        val playerPos = ServerPlayer.player.getVec()
        val playerRot = ServerPlayer.player.getRotation()

        val pos = InstantTransmissionPredictor.predictTeleport(
            tpInfo.distance,
            playerPos.xCoord,
            playerPos.yCoord,
            playerPos.zCoord,
            playerRot.yaw.toDouble(),
            playerRot.pitch.toDouble()
        ) ?: return

        if (ScanUtils.getRoomFromPos(pos)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        playerRot.yaw %= 360

        val prediction = TeleportPrediction(playerRot, pos)
        if (! mc.isSingleplayer) pendingTeleports.add(prediction)
        teleport(prediction, tpInfo.keepMotion)
    }

    private fun doZeroPingWitherImpact(tpInfo: TeleportInfo) {
        if (System.currentTimeMillis() - witherImpactLastUse <= 125) return // 8 CPS limit
        witherImpactLastUse = System.currentTimeMillis()

        doZeroPingInstantTransmission(tpInfo)
    }


    private fun getTeleportInfo(packet: C08PacketPlayerBlockPlacement): TeleportInfo? {
        val heldItem = packet.stack ?: return null
        val sbId = heldItem.SkyblockID ?: return null

        if (sbId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val nbt = heldItem.getSubCompound("ExtraAttributes", false)
            val tuners = nbt?.getByte("tuned_transmission")?.toInt() ?: 0
            if (ServerPlayer.player.sneaking && nbt?.getByte("ethermerge") == 1.toByte()) {
                if (! etherwarp.value) return null
                if (! hasSoulFlow) return null

                return TeleportInfo(
                    distance = 57.0 + tuners,
                    type = TeleportInfo.Companion.Types.Etherwarp,
                    keepMotion = etherwarpKm.value
                )
            }
            else {
                if (! aote.value) return null
                return TeleportInfo(
                    distance = 8.0 + tuners,
                    type = TeleportInfo.Companion.Types.InstantTransmission,
                    keepMotion = aoteKm.value
                )
            }
        }
        else if (PlayerUtils.isHoldingWitherImpact(heldItem)) {
            if (! witherImpact.value) return null
            return TeleportInfo(
                distance = 10.0,
                type = TeleportInfo.Companion.Types.WitherImpact,
                keepMotion = witherImpactKm.value
            )
        }
        return null
    }
}
