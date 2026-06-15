package com.github.noamm9.features.impl.dev

import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.render.NoammRenderLayers
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import net.minecraft.world.entity.Entity
import java.awt.Color
import java.util.*

object Box3D: Feature("Replaces the Glow Esp with 3D boxes") {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val outlineOpacity by SliderSetting("Outline Opacity", 35, 0, 100, 1).hideIf { mode.value == 1 }
    private val fillOpacity by SliderSetting("Fill Opacity", 35, 0, 100, 1).hideIf { mode.value == 0 }

    //#if CHEAT
    private val phase by ToggleSetting("Phase", true)
    //#endif

    private val entities = WeakHashMap<Entity, Color>()

    override fun init() {
        register<CheckEntityGlowEvent>(EventPriority.LOWEST) {
            if (! event.shouldGlow) return@register
            event.shouldGlow = false
            if (event.entity == mc.player) return@register
            entities[event.entity] = event.color
        }

        register<RenderWorldEvent> {
            if (entities.isEmpty()) return@register

            val outline = mode.value.equalsOneOf(0, 2)
            val fill = mode.value.equalsOneOf(1, 2)
            val phase = run {
                //#if CHEAT
                phase.value
                //#else
                //$false
                //#endif
            }

            if (! outline && ! fill) return@register
            val stack = event.ctx.matrixStack
            val consumers = event.ctx.consumers

            stack.pushPose()
            stack.translate(event.ctx.camera.position().reverse())

            val fillBuffer = if (fill) consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED) else null
            val lineBuffer = if (outline) consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES) else null

            entities.forEach { (entity, color) ->
                val aabb = entity.renderBoundingBox.inflate(0.1)

                fillBuffer?.let {
                    Render3D.addFilledBoxVertices(
                        stack.last(), it, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
                        color.red / 255f, color.green / 255f, color.blue / 255f, fillOpacity.value / 100f
                    )
                }

                lineBuffer?.let {
                    Render3D.renderLineBox(
                        stack.last(), it, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
                        color.red / 255f, color.green / 255f, color.blue / 255f, outlineOpacity.value / 100f,
                        lineWidth.value.toFloat()
                    )
                }
            }

            stack.popPose()

            entities.clear()
        }
    }
}