package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.DoorTile
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.handlers.*
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D.renderBlock
import com.github.noamm9.utils.render.Render3D.renderBoxBounds
import net.minecraft.world.level.block.Blocks

object DungeonMap: Feature() {
    override fun init() {
        configSettings.addAll(MapConfig.configSettings)
        hudElements.add(MapRenderer)

        register<RenderWorldEvent> {
            if (! enabled || ! LocationUtils.inDungeon || LocationUtils.inBoss) return@register

            val mimicRoom = DungeonScanner.mimicRoom
            if (MapConfig.mimicEsp.value && ! ScoreCalculation.mimicKilled && mimicRoom != null) {
                for (chestPos in mimicRoom.trappedChestPositions) {
                    if (! WorldUtils.getStateAt(chestPos).`is`(Blocks.TRAPPED_CHEST)) continue
                    val rotation = mimicRoom.rotation ?: continue
                    val corner = mimicRoom.clayPos ?: continue
                    val reletive = ScanUtils.getRelativeCoord(chestPos, corner, rotation)
                    if (mimicRoom.data.secretCoords.chest.none { it == reletive }) continue

                    event.ctx.renderBlock(chestPos, MapConfig.mimicEspColor.value, phase = true)
                }
            }

            if (! MapConfig.boxDoors.value) return@register
            val shouldHideUndiscovered = ! MapConfig.dungeonMapCheater.value ||
                (DungeonListener.dungeonStarted && ! MapConfig.highlightAllDoors.value)

            for (tile in DungeonScanner.dungeonList) {
                if (tile !is DoorTile || tile.opened) continue
                if (! tile.type.equalsOneOf(DoorType.BLOOD, DoorType.WITHER)) continue
                if (shouldHideUndiscovered && tile.state == RoomState.UNDISCOVERED && ! DungeonTree.isFairy(tile)) continue
                event.ctx.renderBoxBounds(
                    tile.aabb,
                    (if (tile.type.keys > 0) MapConfig.doorKeyColor else MapConfig.doorNoKeyColor).value,
                    outline = MapConfig.boxDoorsMode.value.equalsOneOf(0, 2),
                    fill = MapConfig.boxDoorsMode.value.equalsOneOf(1, 2),
                    phase = true
                )
            }
        }
    }
}