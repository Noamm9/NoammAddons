package com.github.noamm9.features.impl.dev

import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.EntityUnloadEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import java.awt.Color
import java.util.*

object Box3D: Feature("Replaces the Glow ESP with 3D boxes") {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val outlineOpacity by SliderSetting("Outline Opacity", 35, 0, 100, 1).hideIf { mode.value == 1 }
    private val fillOpacity by SliderSetting("Fill Opacity", 35, 0, 100, 1).hideIf { mode.value == 0 }

    //#if CHEAT
    private val phase by ToggleSetting("Phase", true)
    //#endif

    private val entities = HashMap<Int, Color>()

    override fun init() {
        register<EntityUnloadEvent> { entities.remove(event.entity.id) }
        register<WorldChangeEvent> { entities.clear() }

        register<CheckEntityGlowEvent>(EventPriority.LOWEST) {
            if (! event.shouldGlow) return@register
            event.cancel()
            if (event.entity == mc.player) return@register
            entities[event.entity.id] = event.color
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

            val iterator = entities.entries.iterator()
            while (iterator.hasNext()) {
                val (id, color) = iterator.next()
                val entity = mc.level?.getEntity(id) ?: run {
                    iterator.remove()
                    continue
                }

                Render3D.renderBoxBounds(
                    event.ctx, entity.renderBoundingBox.inflate(0.1),
                    color.withAlpha(outlineOpacity.value.toFloat() / 100),
                    color.withAlpha(fillOpacity.value.toFloat() / 100),
                    outline = outline,
                    fill = fill,
                    phase = phase,
                    lineWidth = lineWidth.value
                )
            }
        }
    }
}