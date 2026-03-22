package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.Render3D
import net.minecraft.tags.BlockTags

object GyroHelper: Feature("Renders a circle where your gyro will be located.", "Gyro Helper") {
    private val drawBox by ToggleSetting("Draw Box", true).withDescription("Draws a box in the middle of the Gyrokinetic Wand sucking range").section("Render")
    private val drawRing by ToggleSetting("Draw Sucking Range", true).withDescription("Draws the sucking range of the Gyrokinetic Wand")
    private val lineWidth by SliderSetting("Ring Width", 2, 1, 10, 1).withDescription("Controls the thickness of the ring")

    private val boxColor by ColorSetting("Box Color", Utils.favoriteColor.withAlpha(0.3f)).section("Color")
    private val ringColor by ColorSetting("Ring Color", Utils.favoriteColor)

    override fun init() {
        register<RenderWorldEvent> {
            if (! drawRing.value && ! drawBox.value) return@register
            if (boxColor.value.alpha + ringColor.value.alpha == 0) return@register
            if (mc.player?.mainHandItem?.skyblockId != "GYROKINETIC_WAND") return@register
            val gyroPos = MathUtils.raytrace(mc.player !!, 25) ?: return@register
            val stateAtPos = WorldUtils.getStateAt(gyroPos)
            val stateAbove = WorldUtils.getStateAt(gyroPos.above())

            if (stateAtPos.isAir || (! stateAbove.isAir && ! stateAbove.`is`(BlockTags.WOOL_CARPETS))) return@register

            if (drawBox.value) Render3D.renderBox(event.ctx, gyroPos.x + 0.5, gyroPos.y, gyroPos.z + 0.5, 1, 1, boxColor.value)
            if (drawRing.value) {
                val center = gyroPos.add(0.5, 2.05, 0.5).toVec()
                Render3D.renderCircle(event.ctx, center, 10.0, ringColor.value, lineWidth.value)
            }
        }
    }
}