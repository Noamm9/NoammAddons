package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.RenderHelper.renderX
import com.github.noamm9.utils.render.RenderHelper.renderY
import com.github.noamm9.utils.render.RenderHelper.renderZ
import com.github.noamm9.utils.render.world.Render3D.renderCircle
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.tan

/**
 * @see com.github.noamm9.utils.location.LocationUtils.F7Phase
 */
object P3BounceHelper: Feature(
    description = "Draws a horizontal ring in the sky at a -40° pitch reference, to help aim consistently during Necron's third phase (F7/M7).",
    name = "P3 Bounce Helper"
) {
    private val radius by SliderSetting("Radius", 20, 5, 50, 1)
    private val thickness by SliderSetting("Thickness", 2.0, 0.5, 5.0, 0.1)
    private val color by ColorSetting("Color", Color.YELLOW)

    override fun init() {
        register<RenderWorldEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            val player = mc.player ?: return@register

            // Use the render-interpolated position (same pattern as RenderHelper.renderVec) rather
            // than the raw per-tick player position - otherwise the ring visibly jitters between
            // ticks instead of moving smoothly like e.g. WitherDragons' arrow stack indicator.
            val eyeY = player.renderY + player.eyeHeight
            val height = eyeY + radius.value * tan(Math.toRadians(40.0))
            val center = Vec3(player.renderX, height, player.renderZ)

            event.ctx.renderCircle(center, radius.value, color.value, thickness.value, phase = true)
        }
    }
}
