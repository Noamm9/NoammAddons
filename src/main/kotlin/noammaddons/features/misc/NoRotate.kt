package noammaddons.features.misc

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.WorldLoadPostEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.isCoordinateInsideBoss
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ScanUtils.ScanRoom.currentRoom
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.send

object NoRotate: Feature() {
    private var doneLoadingTerrain = false

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        doneLoadingTerrain = false
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldLoadPostEvent) {
        setTimeout(1000) { doneLoadingTerrain = true }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! config.NoRotate) return
        if (event.packet !is S08PacketPlayerPosLook) return
        if (mc.isIntegratedServerRunning) return
        if (Player == null) return
        if (! inSkyblock) return
        if (currentRoom?.name == "Teleport Maze") return // it can break some mods

        handlePlayerPosLook(event.packet, event)
    }

    private fun handlePlayerPosLook(packet: S08PacketPlayerPosLook, event: PacketEvent.Received) {
        if (! doneLoadingTerrain) return
        val updatedPosition = calculateNewPosition(Player, packet)
        val updatedRotation = calculateNewRotation(Player, packet)
        if (isCoordinateInsideBoss(updatedPosition)) return

        event.setCanceled(true)

        Player.apply {
            setPosition(
                updatedPosition.xCoord,
                updatedPosition.yCoord,
                updatedPosition.zCoord
            )
            prevPosX = posX
            prevPosY = posY
            prevPosZ = posZ
        }

        C03PacketPlayer.C06PacketPlayerPosLook(
            Player.posX,
            Player.entityBoundingBox.minY,
            Player.posZ,
            updatedRotation.yaw,
            updatedRotation.pitch,
            Player.onGround
        ).send()
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
}