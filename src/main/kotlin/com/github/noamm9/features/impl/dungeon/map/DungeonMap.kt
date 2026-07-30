package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.SeparatorSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
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
import com.github.noamm9.utils.render.Render3D.renderBox
import net.minecraft.world.level.block.Blocks

object DungeonMap: Feature() {
    override fun init() {
        MapConfig.setup().forEach {
            it.headerName?.let { name ->
                if (configSettings.isNotEmpty()) {
                    configSettings.add(SeparatorSetting())
                }
                configSettings.add(CategorySetting(name))
            }

            configSettings.add(it)
        }

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

            if (! MapConfig.boxWitherDoors.value) return@register

            val shouldHideUndiscovered = ! MapConfig.dungeonMapCheater.value || DungeonListener.dungeonStarted
            val color = (if (DungeonListener.doorKeys > 0) MapConfig.witherDoorKeyColor.value else MapConfig.witherDoorNoKeyColor.value).withAlpha((MapConfig.witherDoorFill.value * 2.55).toInt())

            for (tile in DungeonScanner.dungeonList) {
                if (tile !is DoorTile) continue
                if (tile.opened) continue
                if (! tile.type.equalsOneOf(DoorType.BLOOD, DoorType.WITHER)) continue

                val isFairy = DungeonTree.isFairy(tile)

                if (shouldHideUndiscovered && tile.state == RoomState.UNDISCOVERED && ! isFairy) continue

                event.ctx.renderBox(tile.x + 0.5, 69, tile.z + 0.5, 3, 4, color, phase = true)
            }
        }
    }
}
