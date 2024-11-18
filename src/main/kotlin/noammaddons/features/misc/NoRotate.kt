package noammaddons.features.misc

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S07PacketRespawn
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.PlayerUtils.Player

object NoRotate: Feature() {
    private var doneLoadingTerrain = false

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! config.NoRotate) return

        when (val packet = event.packet) {
            is S08PacketPlayerPosLook -> handlePlayerPosLook(packet, event)
            is S07PacketRespawn -> doneLoadingTerrain = false
            is S0CPacketSpawnPlayer -> doneLoadingTerrain = false
        }
    }

    private fun handlePlayerPosLook(packet: S08PacketPlayerPosLook, event: PacketEvent.Received) {
        if (mc.isIntegratedServerRunning) return
        if (Player == null) return
        if (! inSkyblock) return

        event.setCanceled(true)
        val updatedPosition = calculateNewPosition(Player, packet)
        val updatedRotation = calculateNewRotation(Player, packet)

        Player.setPositionAndRotation(
            updatedPosition.xCoord,
            updatedPosition.yCoord,
            updatedPosition.zCoord,
            Player.rotationYaw,
            Player.rotationPitch
        )

        mc.netHandler.networkManager.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(
                Player.posX,
                Player.entityBoundingBox.minY,
                Player.posZ,
                updatedRotation.yaw,
                updatedRotation.pitch,
                Player.onGround
            )
        )

        if (! doneLoadingTerrain) {
            syncPlayerPosition()
            doneLoadingTerrain = true
        }
    }

    private fun calculateNewPosition(player: EntityPlayerSP, packet: S08PacketPlayerPosLook): Vec3 {
        val flags = packet.func_179834_f()
        var x = packet.x
        var y = packet.y
        var z = packet.z

        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.X)) x += player.posX
        else if (! config.NoRotateKeepMotion) player.motionX = 0.0
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Y)) y += player.posY
        else player.motionY = 0.0
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Z)) z += player.posZ
        else if (! config.NoRotateKeepMotion) player.motionZ = 0.0

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

    private fun syncPlayerPosition() {
        mc.thePlayer.apply {
            prevPosX = posX
            prevPosY = posY
            prevPosZ = posZ
        }
    }
}