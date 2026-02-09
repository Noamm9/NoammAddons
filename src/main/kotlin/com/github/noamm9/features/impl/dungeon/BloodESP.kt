package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.*
import com.github.noamm9.ui.clickgui.componnents.impl.CategorySetting
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.utils.MathUtils.Vec3
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object BloodESP: Feature("Highlight the bloods before the dungeon start to help you for 0s bloodrush") {
    private val box by ToggleSetting("Room Box", true).withDescription("Draws a box around the blood room").section("Options")
    private val tracer by ToggleSetting("Door Tracer", true).withDescription("Draws a tracer to the blood room's door")
    private val s by CategorySetting("Colors").showIf { tracer.value || box.value }
    private val roomColor by ColorSetting("Room Color", Color.RED, false).showIf { box.value }.withDescription("The color of the Blood room ESP")
    private val tracerColor by ColorSetting("Tracer Color", Color.RED, false).showIf { tracer.value }.withDescription("The color of the Blood room Door tracer")

    private var bloodData: Pair<BlockPos, Int>? = null

    override fun init() {
        register<WorldChangeEvent> { bloodData = null }

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon) return@register
            if (DungeonListener.dungeonStarted) return@register
            if (bloodData != null) return@register
            bloodData = findBlood()
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon) return@register
            if (DungeonListener.dungeonStarted) return@register
            val (center, rotation) = bloodData ?: return@register
            val halfRoom = DungeonScanner.roomSize / 2

            val (doorX, doorZ) = when (rotation) {
                0 -> center.x to (center.z - halfRoom)
                1 -> (center.x - halfRoom) to center.z
                2 -> (center.x + halfRoom) to center.z
                else -> center.x to (center.z + halfRoom)
            }

            if (tracer.value) {
                Render3D.renderTracer(event.ctx, Vec3(doorX + 0.5, center.y, doorZ + 0.5), tracerColor.value)
            }

            Render3D.renderBox(
                event.ctx,
                center.x + 0.5, 66, center.z + 0.5,
                31, 34,
                roomColor.value,
                outline = true,
                fill = false,
                phase = true
            )
            /*
                        Render3D.renderBox(
                            event.ctx,
                            doorX, 98.99, doorZ,
                            3, 1,
                            Color.CYAN.withAlpha(70),
                            outline = false,
                            fill = true,
                            phase = false
                        )*/
        }
    }

    private fun findBlood(): Pair<BlockPos, Int>? {
        val mainRoom = DungeonInfo.uniqueRooms["Blood"]?.mainRoom ?: return null
        val center = BlockPos(mainRoom.x, 99, mainRoom.z)
        val checkOffsets = arrayOf(
            Triple(- 15, - 6, 0),
            Triple(- 6, 15, 1),
            Triple(15, 6, 3),
            Triple(6, - 15, 2)
        )

        checkOffsets.forEach { (dx, dz, i) ->
            if (WorldUtils.getBlockAt(center.x + dx, center.y, center.z + dz) == Blocks.REDSTONE_BLOCK) {
                return center to i
            }
        }

        return null
    }
}