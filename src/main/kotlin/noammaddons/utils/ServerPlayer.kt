package noammaddons.utils

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.PacketEvent


object ServerPlayer {
    class PlayerState {
        var x: Double? = null
        var y: Double? = null
        var z: Double? = null
        var yaw: Float? = null
        var pitch: Float? = null
        var onGround: Boolean? = null
        var sneaking: Boolean = false
        var heldHotbarSlot: Int? = null

        fun getPos(): BlockPos? {
            return BlockPos(x ?: return null, y ?: return null, z ?: return null)
        }

        fun getVec(): Vec3? {
            return Vec3(x ?: return null, y ?: return null, z ?: return null)
        }

        fun getRotation(): MathUtils.Rotation? {
            return MathUtils.Rotation(yaw ?: return null, pitch ?: return null)
        }

        fun getHeldItem(): ItemStack? {
            return mc.thePlayer.inventory.getStackInSlot(heldHotbarSlot ?: return null)
        }

        fun reset() {
            x = null
            y = null
            z = null
            yaw = null
            pitch = null
            onGround = null
            sneaking = false
            heldHotbarSlot = null
        }
    }

    val player = PlayerState()

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        when (val packet = event.packet) {
            is C03PacketPlayer -> onC03PacketPlayer(packet)
            is C0BPacketEntityAction -> onC0BPacketEntityAction(packet)
            is C09PacketHeldItemChange -> player.heldHotbarSlot = packet.slotId
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) = player.reset()

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) = player.reset()

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
