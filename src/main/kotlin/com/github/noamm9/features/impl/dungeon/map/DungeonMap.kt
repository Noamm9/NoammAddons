package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.handlers.*
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import net.minecraft.world.level.block.Blocks

object DungeonMap: Feature() {
    override fun init() {
        configSettings.addAll(MapConfig.configSettings)
        hudElements.add(MapRenderer)

        //#if CHEAT
        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register

            val mimicRoom = DungeonScanner.mimicRoom
            if (MapConfig.mimicEsp.value && ! ScoreCalculation.mimicKilled && mimicRoom != null) {
                for (chestPos in mimicRoom.trappedChestPositions) {
                    if (! WorldUtils.getStateAt(chestPos).`is`(Blocks.TRAPPED_CHEST)) continue
                    if (mimicRoom.secretCoords[SecretType.CHEST]?.any { it == chestPos } != true) continue
                    event.ctx.renderBlock(chestPos, MapConfig.mimicEspColor.value, phase = true)
                }
            }
        }
        //#endif
    }
}