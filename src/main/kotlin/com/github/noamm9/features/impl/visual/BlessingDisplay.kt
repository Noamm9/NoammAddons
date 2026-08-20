package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.dungeons.enums.Blessing
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.ChatFormatting
import java.awt.Color

object BlessingDisplay: Feature("Displays the current active blessings in the dungeon.") {
    private val power by BooleanConfig("Power Blessing", true).section("Blessings")
    private val time by BooleanConfig("Time Blessing", true)
    private val wisdom by BooleanConfig("Wisdom Blessing", false)
    private val life by BooleanConfig("Life Blessing", false)
    private val stone by BooleanConfig("Stone Blessing", false)

    private val powerColor by ColorConfig("Power Color", Color(ChatFormatting.DARK_RED.color !!)).showIf { power.value }.section("Colors")
    private val timeColor by ColorConfig("Time Color", Color(ChatFormatting.DARK_PURPLE.color !!)).showIf { time.value }
    private val wisdomColor by ColorConfig("Wisdom Color", Color(ChatFormatting.AQUA.color !!)).showIf { wisdom.value }
    private val lifeColor by ColorConfig("Red Color", Color(ChatFormatting.RED.color !!)).showIf { life.value }
    private val stoneColor by ColorConfig("Stone Color", Color(ChatFormatting.GRAY.color !!)).showIf { stone.value }

    private fun getBlessingConfig(type: Blessing) = when (type) {
        Blessing.POWER -> power.value to powerColor.value
        Blessing.TIME -> time.value to timeColor.value
        Blessing.STONE -> stone.value to stoneColor.value
        Blessing.LIFE -> life.value to lifeColor.value
        Blessing.WISDOM -> wisdom.value to wisdomColor.value
    }

    override fun init() {
        hudElement("BlessingDisplay") { context, example ->
            var maxWidth = 0f
            var currentY = 0f

            Blessing.entries.forEach { blessing ->
                val (enabled, color) = getBlessingConfig(blessing)

                val value = if (example) 5 else blessing.current
                if (! enabled || value <= 0) return@forEach

                val text = "${blessing.displayString} §f$value"

                context.drawString(text, 0, currentY.toInt(), color)

                maxWidth = maxOf(maxWidth, text.width().toFloat())
                currentY += 9f
            }

            return@hudElement maxWidth to currentY
        }
    }
}