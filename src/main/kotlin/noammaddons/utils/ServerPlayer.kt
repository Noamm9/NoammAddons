package noammaddons.utils

import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.noammaddons.Companion.mc


object ServerPlayer {
    data class PlayerState(
        var x: Double = - 1.0,
        var y: Double = - 1.0,
        var z: Double = - 1.0,
        var yaw: Float = 0f,
        var pitch: Float = 0f,
        var onGround: Boolean = false,
        var sneaking: Boolean = false,
        var heldHotbarSlot: Int = 0
    ) {
        val initialized get() = x != - 1.0 && y != - 1.0 && z != - 1.0

        fun getPos() = BlockPos(x, y, z)
        fun getVec() = Vec3(x, y, z)
        fun getRotation() = MathUtils.Rotation(yaw, pitch)
        fun getHeldItem() = mc.thePlayer.inventory.getStackInSlot(heldHotbarSlot)
    }

    val player = PlayerState()

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        when (val packet = event.packet) {
            is C03PacketPlayer -> onC03PacketPlayer(packet)
            is C0BPacketEntityAction -> onC0BPacketEntityAction(packet)
            is C09PacketHeldItemChange -> player.heldHotbarSlot = packet.slotId
            is S09PacketHeldItemChange -> player.heldHotbarSlot = packet.heldItemHotbarIndex
        }
    }

    private fun onC03PacketPlayer(packet: C03PacketPlayer) {
        player.onGround = packet.isOnGround

        if (packet.isMoving) {
            player.x = packet.positionX
            player.y = packet.positionY
            player.z = packet.positionZ
        }

        if (packet.rotating) {
            player.yaw = packet.yaw
            player.pitch = packet.pitch
        }
    }

    private fun onC0BPacketEntityAction(packet: C0BPacketEntityAction) {
        player.sneaking = when (packet.action) {
            C0BPacketEntityAction.Action.START_SNEAKING -> true
            C0BPacketEntityAction.Action.STOP_SNEAKING -> false
            else -> player.sneaking
        }
    }
}
