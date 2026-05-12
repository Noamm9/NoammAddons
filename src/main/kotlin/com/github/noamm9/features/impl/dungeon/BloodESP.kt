package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.MathUtils.Vec3
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object BloodESP: Feature("Highlights the blood room before dungeon start to help you with 0s Bloodrush.") {
    private val box by ToggleSetting("Room Box", true).withDescription("Draws a box around the Blood room.").section("Options")
    private val tracer by ToggleSetting("Door Tracer", true).withDescription("Draws a tracer to the Blood room's door.")
    private val s by CategorySetting("Colors").showIf { tracer.value || box.value }
    private val roomColor by ColorSetting("Room Color", Color.RED, false).showIf { box.value }.withDescription("The color of the Blood room ESP.")
    private val tracerColor by ColorSetting("Tracer Color", Color.RED, false).showIf { tracer.value }.withDescription("The color of the Blood room door tracer.")

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
            if (! box.value && ! tracer.value) return@register
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

            if (tracer.value) Render3D.renderTracer(event.ctx, Vec3(doorX + 0.5, center.y, doorZ + 0.5), tracerColor.value)

            if (box.value) Render3D.renderBox(
                event.ctx,
                center.x + 0.5, 66, center.z + 0.5,
                31, 34,
                roomColor.value,
                outline = true,
                fill = false,
                phase = true
            )
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
//#endif