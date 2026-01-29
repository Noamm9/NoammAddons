package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.section
import com.github.noamm9.ui.clickgui.componnents.showIf
import com.github.noamm9.ui.hud.getValue
import com.github.noamm9.ui.hud.provideDelegate
import com.github.noamm9.utils.dungeons.enums.Blessing
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.ChatFormatting
import java.awt.Color

object BlessingDisplay: Feature("Displays the current active blessings of the dungeon") {
    private val power by ToggleSetting("Power Blessing", true).section("Blessings")
    private val time by ToggleSetting("Time Blessing", true)
    private val wisdom by ToggleSetting("Wisdom Blessing", false)
    private val life by ToggleSetting("Life Blessing", false)
    private val stone by ToggleSetting("Stone Blessing", false)

    private val powerColor by ColorSetting("Power Color", Color(ChatFormatting.DARK_RED.color !!)).showIf { power.value }.section("Colors")
    private val timeColor by ColorSetting("Time Color", Color(ChatFormatting.DARK_PURPLE.color !!)).showIf { time.value }
    private val wisdomColor by ColorSetting("Wisdom Color", Color(ChatFormatting.AQUA.color !!)).showIf { wisdom.value }
    private val lifeColor by ColorSetting("Red Color", Color(ChatFormatting.RED.color !!)).showIf { life.value }
    private val stoneColor by ColorSetting("Stone Color", Color(ChatFormatting.GRAY.color !!)).showIf { stone.value }

    private fun getBlessingConfig(type: Blessing) = when (type) {
        Blessing.POWER -> power.value to powerColor.value
        Blessing.TIME -> time.value to timeColor.value
        Blessing.STONE -> stone.value to stoneColor.value
        Blessing.LIFE -> life.value to lifeColor.value
        Blessing.WISDOM -> wisdom.value to wisdomColor.value
    }

    val blessingHud by hudElement("BlessingDisplay") { context, example ->
        var maxWidth = 0f
        var currentY = 0f

        Blessing.entries.forEach { blessing ->
            val (enabled, color) = getBlessingConfig(blessing)

            val value = if (example) 5 else blessing.current
            if (! enabled || value <= 0) return@forEach

            val text = "${blessing.displayString} §f$value"

            Render2D.drawString(context, text, 0, currentY.toInt(), color)

            maxWidth = maxOf(maxWidth, text.width().toFloat())
            currentY += 9f
        }

        return@hudElement maxWidth to currentY
    }
}