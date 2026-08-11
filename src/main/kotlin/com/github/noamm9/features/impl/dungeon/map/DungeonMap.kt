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
import com.github.noamm9.utils.render.Render3D.renderBoxBounds
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

            for (tile in DungeonScanner.dungeonList) {
                if (tile !is DoorTile || tile.opened) continue
                if (! tile.type.equalsOneOf(DoorType.BLOOD, DoorType.WITHER)) continue
                if (shouldHideUndiscovered && tile.state == RoomState.UNDISCOVERED && ! DungeonTree.isFairy(tile)) continue
                val color = (if (tile.type.keys > 0) MapConfig.witherDoorKeyColor else MapConfig.witherDoorNoKeyColor).value
                event.ctx.renderBoxBounds(
                    tile.aabb,
                    color.withAlpha((MapConfig.witherDoorFill.value * 2.55).toInt()),
                    outline = MapConfig.boxWitherDoorsMode.value.equalsOneOf(0, 2),
                    fill = MapConfig.boxWitherDoorsMode.value.equalsOneOf(1, 2),
                    phase = true
                )
            }
        }
    }
}