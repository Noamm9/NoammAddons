package com.github.noamm9.features.impl.misc

//#if CHEAT

import com.github.noamm9.config.types.MultiCheckboxSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.utils.*
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.PlayerUtils.serverPitch
import com.github.noamm9.utils.PlayerUtils.serverYaw
import com.github.noamm9.utils.Utils.send
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.EtherwarpHelper
import com.github.noamm9.utils.items.InstantTransmissionHelper
import com.github.noamm9.utils.items.TeleportUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.Camera
import net.minecraft.network.protocol.game.*
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.concurrent.*

object NoRotate: Feature("Prevents the server from snapping back your head when teleporting.") {
    private val tpItems by MultiCheckboxSetting("Teleport Items", mutableMapOf(Pair("Etherwarp", false), Pair("Instant Transmission", false), Pair("Wither Impact", false)))
    val zeroPingCamera by MultiCheckboxSetting("Zero Ping Camera", mutableMapOf(Pair("Etherwarp", false), Pair("Instant Transmission", false), Pair("Wither Impact", false))).withDescription("Instantly sets your camera at the teleport position.")
    private val resyncTimeout by SliderSetting("Resync Timeout", 500, 300, 1000, 50).showIf { zeroPingCamera.value.values.any { it } }.withDescription("time in miliseconds of how long should it take for the detected teleport to time out")

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
            val packet = event.packet as? ClientboundPlayerPositionPacket ?: return@register
            if (pendingTeleports.isEmpty()) return@register
            pendingTeleports.removeFirst()

            val old = PositionMoveRotation.of(player)
            val new = PositionMoveRotation.calculateAbsolute(old, packet.change, packet.relatives)

            player.setPos(new.position())
            player.deltaMovement = new.deltaMovement()

            val newOldPos = PositionMoveRotation.calculateAbsolute(
                PositionMoveRotation(player.oldPosition(), Vec3.ZERO, player.yRotO, player.xRotO), packet.change(), packet.relatives()
            )

            player.xo = newOldPos.position().x.also { player.xOld = it }
            player.yo = newOldPos.position().y.also { player.yOld = it }
            player.zo = newOldPos.position().z.also { player.zOld = it }

            ServerboundAcceptTeleportationPacket(packet.id).send()
            ServerboundMovePlayerPacket.PosRot(player.x, player.y, player.z, new.yRot, new.xRot, false, false).send()

            (player as ILocalPlayer).setLastYaw(new.yRot)
            (player as ILocalPlayer).setLastPitch(new.xRot)

            event.isCanceled = true
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

        val config = zeroPingCamera.value.values.toList()
        val simulation = pendingTeleports.lastOrNull() ?: return original.call(instance, x, y, z)
        if (! config[simulation.info.type]) return original.call(instance, x, y, z)
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

    private fun getInfo(stack: ItemStack?): TeleportUtils.Info? {
        val info = TeleportUtils.getInfo(stack) ?: return null
        return when (info.type) {
            TeleportUtils.Etherwarp -> if (tpItems.value["Etherwarp"] !!) info else null
            TeleportUtils.InstantTransmission -> if (tpItems.value["Instant Transmission"] !!) info else null
            TeleportUtils.WitherImpact -> if (tpItems.value["Wither Impact"] !!) info else null
            else -> null
        }
    }
}
//#endif