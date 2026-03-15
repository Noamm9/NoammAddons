package com.github.noamm9.features.impl.floor7.devices

import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import java.awt.Color

object ArrowAlign: Feature("Arrow Align Solver") {
    private val colorStyle by DropdownSetting("Color Style", 0, listOf("Dynamic", "Custom")).withDescription("Dynamic: Colors the text based on the clicks needed.")
    private val textColor by ColorSetting("Text Color", Color.WHITE, false).showIf { colorStyle.value == 1 }
    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks").withDescription("Sneak to disable.")
    private val invertSneak by ToggleSetting("Invert Sneak").showIf { blockWrongClicks.value }

    private val gridCorner = BlockPos(- 2, 120, 75)
    private val gridBox = AABB(
        gridCorner.x.toDouble(), gridCorner.y.toDouble(), gridCorner.z.toDouble(),
        gridCorner.x + 1.0, gridCorner.y + 5.0, gridCorner.z + 5.0
    )

    private val clickTimestamps = mutableMapOf<Int, Long>()
    private val clicksRemaining = mutableMapOf<Int, Int>()

    private var frameRotations = IntArray(25) { - 1 }
    private var solution: List<Int>? = null

    override fun init() {
        register<TickEvent.Start> {
            if (LocationUtils.F7Phase != 3) return@register reset()
            if (mc.player !!.distanceToSqr(gridCorner.toVec()) > 200) return@register reset()

            val frames = mc.level !!.getEntitiesOfClass(ItemFrame::class.java, gridBox) { it.item.item == Items.ARROW }

            frames.forEach { frame ->
                val pos = frame.blockPosition()
                val index = (pos.y - gridCorner.y) + (pos.z - gridCorner.z) * 5

                if (index in 0 .. 24) {
                    val lastClick = clickTimestamps[index] ?: 0L
                    if (System.currentTimeMillis() - lastClick > 1000) {
                        frameRotations[index] = frame.rotation
                    }
                }
            }

            solution = null
            clicksRemaining.clear()

            possibleSolutions.forEach { arr ->
                for (i in arr.indices) {
                    val curr = frameRotations[i]
                    val target = arr[i]

                    if ((target == - 1 || curr == - 1) && target != curr) return@forEach
                }

                solution = arr

                for (i in arr.indices) {
                    val curr = frameRotations[i]
                    if (curr == - 1) continue

                    val needed = getClicks(curr, arr[i])
                    if (needed != 0) clicksRemaining[i] = needed
                }

                return@register
            }
        }

        register<PlayerInteractEvent.RIGHT_CLICK.ENTITY> {
            if (LocationUtils.F7Phase != 3) return@register
            val entity = event.entity as? ItemFrame ?: return@register
            if (entity.item.item != Items.ARROW) return@register
            val pos = entity.blockPosition().takeIf { it.x == gridCorner.x } ?: return@register

            val frameY = pos.y - gridCorner.y
            val frameZ = (pos.z - gridCorner.z) * 5
            val index = frameY + frameZ

            if (index !in 0 .. 24) return@register
            if (frameRotations[index] == - 1) return@register

            val shouldBlock = blockWrongClicks.value && (mc.player?.isCrouching == invertSneak.value)
            if (! clicksRemaining.containsKey(index) && shouldBlock) {
                event.isCanceled = true
                return@register
            }

            clickTimestamps[index] = System.currentTimeMillis()
            frameRotations[index] = (frameRotations[index] + 1) % 8

            val target = solution?.get(index) ?: return@register
            if (getClicks(frameRotations[index], target) == 0) {
                clicksRemaining.remove(index)
            }
        }

        register<RenderWorldEvent> {
            if (clicksRemaining.isEmpty() || LocationUtils.F7Phase != 3) return@register

            clicksRemaining.toList().forEach { (index, count) ->
                val pos = gridCorner.add(0.5, (index % 5) + 0.5, (index / 5) + 0.5)
                val color = if (colorStyle.value == 0) when {
                    count < 3 -> Color.GREEN
                    count < 5 -> Color.ORANGE
                    else -> Color.RED
                }
                else textColor.value


                Render3D.renderString(
                    "$count",
                    pos.x, pos.y + 0.55, pos.z + 0.5,
                    color = color,
                    scale = 1f,
                    phase = true
                )
            }
        }
    }

    private fun reset() {
        frameRotations.fill(- 1)
        solution = null
        clicksRemaining.clear()
        clickTimestamps.clear()
    }

    private fun getClicks(currentRotation: Int, targetRotation: Int): Int {
        if (targetRotation == - 1) return 0
        return (8 - currentRotation + targetRotation) % 8
    }

    private val possibleSolutions = listOf(
        listOf(7, 7, - 1, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, 3, - 1, - 1, - 1, - 1, 1, - 1, - 1, - 1, 7, 1),
        listOf(- 1, - 1, 7, 7, 5, - 1, 7, 1, - 1, 5, - 1, - 1, - 1, - 1, - 1, - 1, 7, 5, - 1, 1, - 1, - 1, 7, 7, 1),
        listOf(7, 7, - 1, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, - 1, 7, 5, - 1, - 1, - 1, - 1, 5, - 1, - 1, - 1, 3, 3),
        listOf(5, 3, 3, 3, - 1, 5, - 1, - 1, - 1, - 1, 7, 7, - 1, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, - 1),
        listOf(5, 3, 3, 3, 3, 5, - 1, - 1, - 1, 1, 7, 7, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, - 1, 7, 7, 7, 1),
        listOf(7, 7, 7, 7, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, 3, - 1, - 1, - 1, - 1, 1, - 1, 7, 7, 7, 1),
        listOf(- 1, - 1, - 1, - 1, - 1, 1, - 1, 1, - 1, 1, 1, - 1, 1, - 1, 1, 1, - 1, 1, - 1, 1, - 1, - 1, - 1, - 1, - 1),
        listOf(- 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, 3, - 1, - 1, - 1, - 1, 1, 7, 7, 7, 7, 1, - 1, - 1, - 1, - 1, - 1),
        listOf(- 1, - 1, - 1, - 1, - 1, - 1, 1, - 1, 1, - 1, 7, 1, 7, 1, 3, 1, - 1, 1, - 1, 1, - 1, - 1, - 1, - 1, - 1)
    )
}