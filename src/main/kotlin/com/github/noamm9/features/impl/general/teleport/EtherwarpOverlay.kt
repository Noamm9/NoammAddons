package com.github.noamm9.features.impl.general.teleport

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.hideIf
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.EtherwarpHelper
import com.github.noamm9.utils.render.Render3D
import java.awt.Color

object EtherwarpOverlay: Feature() {
    private val mode by DropdownSetting("Mode", 0, listOf("Outline", "Fill", "Filled Outline")).section("Settings")
    private val phase by ToggleSetting("Phase")
    private val lineWidth by SliderSetting("Line Width", 1.0, 1.0, 10.0, 0.1).hideIf { mode.value == 1 }

    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }.section("Colors")
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }

    private val invalidFillColor by ColorSetting("Invalid Fill Color ", Color.RED.withAlpha(50)).hideIf { mode.value == 0 }
    private val invalidOutlineColor by ColorSetting("Invalid Outline Color ", Color.RED, false).hideIf { mode.value == 1 }

    override fun init() {
        register<RenderWorldEvent> {
            val player = mc.player ?: return@register
            if (! player.isSteppingCarefully) return@register
            val heldItem = player.mainHandItem.takeUnless { it.isEmpty } ?: return@register
            val distance = EtherwarpHelper.getEtherwarpDistance(heldItem) ?: return@register
            val (valid, pos) = EtherwarpHelper.getEtherPos(player.position(), player.lookAngle, distance)

            Render3D.renderBlock(
                event.ctx, pos ?: return@register,
                if (valid) outlineColor.value else invalidOutlineColor.value,
                if (valid) fillColor.value else invalidFillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = phase.value,
                lineWidth.value.toFloat()
            )
        }
    }
}