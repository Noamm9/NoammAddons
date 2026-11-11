package com.github.noamm9.features.impl.misc

//#if CHEAT

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.utils.*
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.PlayerUtils.serverPitch
import com.github.noamm9.utils.PlayerUtils.serverYaw
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.*
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.Camera
import net.minecraft.network.protocol.game.*
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.concurrent.*

object ZeroPingCamera: Feature("Places your camera at the place you will be teleported to instantly.") {
    private val etherwarp by ToggleSetting("Etherwarp").withDescription("Instantly sets your camera at the Etherwarp position.")
    private val instantTransmission by ToggleSetting("Instant Transmission").withDescription("Instantly sets your camera at the Instant Transmission position.")
    private val witherImpact by ToggleSetting("Wither Impact").withDescription("Instantly sets your camera at the Wither Impact position.")
    private val resyncTimeout by SliderSetting("Resync Timeout", 500, 300, 1000, 50).showIf { etherwarp.value || instantTransmission.value || witherImpact.value }.withDescription("time in milliseconds of how long should it take for the detected teleport to time out")

    val pendingTeleports = CopyOnWriteArrayList<TeleportUtils.Prediction>()
    private var lastWitherImpact = System.currentTimeMillis()

    override fun init() {
        register<WorldChangeEvent> {
            pendingTeleports.clear()
            lastWitherImpact = System.currentTimeMillis()
        }

        register<PacketEvent.Sent> {
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            if (WorldUtils.getBlockAt(packet.hitResult.blockPos) !in TeleportUtils.TILLABLE_BLOCKS) return@register
            val tpInfo = getInfo(player.getItemInHand(packet.hand)) ?: return@register
            attemptTeleport(tpInfo, player.serverYaw, player.serverPitch)
        }

        register<PacketEvent.Sent> {
            val packet = event.packet as? ServerboundUseItemPacket ?: return@register
            val tpInfo = getInfo(player.getItemInHand(packet.hand)) ?: return@register
            attemptTeleport(tpInfo, packet.yRot, packet.xRot)
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet !is ClientboundPlayerPositionPacket) return@register
            if (pendingTeleports.isNotEmpty()) pendingTeleports.removeFirst()
        }
    }

    private fun attemptTeleport(tpInfo: TeleportUtils.Info, yaw: Float, pitch: Float) {
        if (LocationUtils.world.equalsOneOf(WorldType.Home, WorldType.Garden)) return
        if (TeleportUtils.canTeleport(yaw, pitch)) when (tpInfo.type) {
            TeleportUtils.Etherwarp -> doZeroPingEtherwarp(tpInfo, yaw, pitch)
            TeleportUtils.InstantTransmission -> doZeroPingInstantTransmission(tpInfo, yaw, pitch)
            TeleportUtils.WitherImpact -> doZeroPingWitherImpact(tpInfo, yaw, pitch)
        }
    }

    @JvmStatic
    fun cameraHook(instance: Camera, x: Double, y: Double, z: Double, original: Operation<Void>): Void? {
        if (! enabled) return original.call(instance, x, y, z)

        val simulation = pendingTeleports.lastOrNull() ?: return original.call(instance, x, y, z)
        if (! isEnabled(simulation.info.type)) return original.call(instance, x, y, z)
        val (x, y, z) = simulation.position.add(y = player.eyeHeight).destructured()

        return original.call(instance, x, y, z)
    }

    private fun teleport(prediction: TeleportUtils.Prediction) {
        ThreadUtils.scheduledTaskServer(resyncTimeout.value / 50) { pendingTeleports.remove(prediction) }
        pendingTeleports.add(prediction)
    }

    private fun doZeroPingEtherwarp(tpInfo: TeleportUtils.Info, yaw: Float? = null, pitch: Float? = null) {
        val player = player as ILocalPlayer

        val playerPos = pendingTeleports.lastOrNull()?.position ?: player.let { Vec3(it.serverX, it.serverY, it.serverZ) }
        val etherPos = EtherwarpHelper.getEtherPos(playerPos, MathUtils.getLookVec(yaw ?: player.serverYaw, pitch ?: player.serverPitch), tpInfo.distance)
        if (! etherPos.succeeded || etherPos.pos == null) return
        if (ScanUtils.getRoomFromPos(etherPos.vec !!)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        val tags = WorldUtils.getStateAt(etherPos.pos).tags().toList()
        val prediction = if (tags.containsOneOf(BlockTags.FENCES, BlockTags.WALLS, BlockTags.FENCE_GATES)) etherPos.vec.add(0.5, 2.05, 0.5) else etherPos.vec.add(0.5, 1.05, 0.5)
        teleport(TeleportUtils.Prediction(prediction, tpInfo))
    }

    private fun doZeroPingInstantTransmission(tpInfo: TeleportUtils.Info, yaw: Float? = null, pitch: Float? = null) {
        val player = player as ILocalPlayer

        val playerPos = pendingTeleports.lastOrNull()?.position ?: Vec3(player.serverX, player.serverY, player.serverZ)
        val pos = InstantTransmissionHelper.predictTeleport(tpInfo.distance, playerPos, yaw ?: player.serverYaw, pitch ?: player.serverPitch) ?: return
        if (ScanUtils.getRoomFromPos(pos)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return
        teleport(TeleportUtils.Prediction(pos, tpInfo))
    }

    private fun doZeroPingWitherImpact(tpInfo: TeleportUtils.Info, yaw: Float? = null, pitch: Float? = null) {
        if (System.currentTimeMillis() - lastWitherImpact <= 125) return // ~8 CPS limit
        lastWitherImpact = System.currentTimeMillis()
        doZeroPingInstantTransmission(tpInfo, yaw, pitch)
    }

    fun isEnabled(type: Int) = when (type) {
        TeleportUtils.Etherwarp -> etherwarp.value
        TeleportUtils.InstantTransmission -> instantTransmission.value
        TeleportUtils.WitherImpact -> witherImpact.value
        else -> false
    }

    private fun getInfo(stack: ItemStack?) = TeleportUtils.getInfo(stack)?.takeIf { isEnabled(it.type) }
}
//#endif