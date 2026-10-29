package com.github.noamm9.compat

import com.github.noamm9.utils.Utils.send
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.phys.Vec3

//~ if <26.3 'positionDelta.hasDeltaX() || positionDelta.hasDeltaZ()' -> 'xa != 0.toShort() || za != 0.toShort()'
fun ClientboundMoveEntityPacket.hasHorizontalMove() = positionDelta.hasDeltaX() || positionDelta.hasDeltaZ()

//~ if <26.3 'positionDelta.decode(entity.positionCodec).endPosition()' -> 'entity.positionCodec.decode(xa.toLong(), ya.toLong(), za.toLong())'
fun ClientboundMoveEntityPacket.decodePosition(entity: Entity): Vec3 = positionDelta.decode(entity.positionCodec).endPosition()

//~ if <26.3 'position.endPosition()' -> 'values.position()'
val ClientboundEntityPositionSyncPacket.syncedPosition: Vec3 get() = position.endPosition()

//~ if <26.3 'xMaxSpeed == 0f && yMaxSpeed == 0f && zMaxSpeed == 0f' -> 'maxSpeed == 0f'
val ClientboundLevelParticlesPacket.hasNoSpeed get() = xMaxSpeed == 0f && yMaxSpeed == 0f && zMaxSpeed == 0f

//~ if <26.3 'get() = overrideLimiter' -> 'get() = isOverrideLimiter'
val ClientboundLevelParticlesPacket.overridesLimiter get() = overrideLimiter

//~ if <26.3 'getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false)[index]' -> 'frontText.getMessage(index, false)'
fun SignBlockEntity.frontLine(index: Int): String = getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false)[index].string

//~ if <26.3 '(pos, lines, net.minecraft.world.level.block.entity.SignTextSlot.FRONT)' -> '(pos, true, lines[0], lines[1], lines[2], lines[3])'
fun signUpdatePacket(pos: BlockPos, lines: List<String>) = ServerboundSignUpdatePacket(pos, lines, net.minecraft.world.level.block.entity.SignTextSlot.FRONT)

fun acceptTeleport(id: Int, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
    //? if <26.3 {
    /*ServerboundAcceptTeleportationPacket(id).send()
    net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, false, false).send()
    *///? } else {
    ServerboundAcceptTeleportationPacket(id, x, y, z, yaw, pitch).send()
    //? }
}
