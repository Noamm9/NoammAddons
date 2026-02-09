package com.github.noamm9.features.impl.dungeon.waypoints

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object SecretsWaypoints {
    private data class SecretWaypoint(val pos: BlockPos, val type: SecretType) {
        val color: Color = when (type) {
            SecretType.REDSTONE_KEY -> Color.RED
            SecretType.WITHER_ESSANCE -> Color.BLACK
            else -> Color.MAGENTA
        }
    }

    private val secretDefinitions by lazy { ScanUtils.roomList.associate { it.name to it.secretCoords } }

    private val currentSecrets = CopyOnWriteArrayList<SecretWaypoint>()

    fun onRoomEnter(room: UniqueRoom) {
        if (! DungeonWaypoints.secretWaypoints.value) return
        currentSecrets.clear()

        val rotation = room.rotation?.let { 360 - it } ?: return
        val corner = room.corner ?: return

        val coords = secretDefinitions[room.name] ?: return

        val activeSecrets = buildList {
            fun addSecrets(list: List<BlockPos>, type: SecretType) {
                list.forEach { add(SecretWaypoint(ScanUtils.getRealCoord(it, corner, rotation), type)) }
            }

            addSecrets(coords.redstoneKey, SecretType.REDSTONE_KEY)
            addSecrets(coords.wither, SecretType.WITHER_ESSANCE)
            addSecrets(coords.bat, SecretType.BAT)
            addSecrets(coords.item, SecretType.ITEM)
            addSecrets(coords.chest, SecretType.CHEST)
        }

        currentSecrets.addAll(activeSecrets)
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! DungeonWaypoints.secretWaypoints.value || currentSecrets.isEmpty()) return

        for (wp in currentSecrets) {
            if (wp.type == SecretType.REDSTONE_KEY && WorldUtils.getBlockAt(wp.pos) != Blocks.PLAYER_HEAD) continue
            Render3D.renderBlock(ctx, wp.pos, wp.color, fill = false, outline = true, phase = true)
        }
    }

    fun onSecret(event: DungeonEvent.SecretEvent) {
        if (! DungeonWaypoints.secretWaypoints.value || currentSecrets.isEmpty()) return
        if (event.type == SecretType.LEVER) return
        val playerPos = NoammAddons.mc.player?.blockPosition() ?: return
        if (event.pos.distSqr(playerPos) > 36) return

        val distinctTypes = setOf(SecretType.BAT, SecretType.ITEM)

        val target = if (event.type !in distinctTypes) currentSecrets.find { it.pos == event.pos }
        else currentSecrets.filter { it.type in distinctTypes }.minByOrNull { it.pos.distSqr(event.pos) }

        target?.let(currentSecrets::remove)
    }

    fun clear() = currentSecrets.clear()
}