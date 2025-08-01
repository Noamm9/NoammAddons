package noammaddons.features.impl.misc

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.ScanUtils.currentRoom
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send

object NoRotate: Feature("Stop server packets from rotating you") {
    private val keepMotion by ToggleSetting("Keep Motion")

    private var doneLoadingTerrain = false

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        doneLoadingTerrain = false
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldLoadPostEvent) {
        setTimeout(1000) { doneLoadingTerrain = true }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onPacket(event: PacketEvent.Received) {
        val packet = event.packet as? S08PacketPlayerPosLook ?: return
        if (mc.isIntegratedServerRunning) return
        if (! doneLoadingTerrain) return
        val player = mc.thePlayer ?: return
        if (currentRoom?.data?.name == "Teleport Maze") return // can break some mods
        val updatedPosition = calculateNewPosition(player, packet)
        val updatedRotation = calculateNewRotation(player, packet)
        if (player.heldItem?.skyblockID.equalsOneOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP")) return
        event.setCanceled(true)

        player.setPosition(updatedPosition.xCoord, updatedPosition.yCoord, updatedPosition.zCoord)
        C03PacketPlayer.C06PacketPlayerPosLook(
            player.posX, player.entityBoundingBox.minY, player.posZ, updatedRotation.yaw, updatedRotation.pitch, false
        ).send()
    }


    private fun calculateNewPosition(player: EntityPlayerSP, packet: S08PacketPlayerPosLook): Vec3 {
        val flags = packet.func_179834_f()
        var x = packet.x
        var y = packet.y
        var z = packet.z

        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.X)) x += player.posX
        else if (! keepMotion) player.motionX = 0.0
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Y)) y += player.posY
        else player.motionY = 0.0
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Z)) z += player.posZ
        else if (! keepMotion) player.motionZ = 0.0

        return Vec3(x, y, z)
    }

    private fun calculateNewRotation(player: EntityPlayerSP, packet: S08PacketPlayerPosLook): Rotation {
        val flags = packet.func_179834_f()
        var yaw = packet.yaw
        var pitch = packet.pitch

        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) pitch += player.rotationPitch
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) yaw += player.rotationYaw

        return Rotation(yaw, pitch)
    }
}