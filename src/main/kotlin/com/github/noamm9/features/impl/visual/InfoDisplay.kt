package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.IFeatureWindow
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.network.chat.Component
import java.awt.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object InfoDisplay: Feature("Displays the system time, clicks per second, FPS, and TPS on screen."), IFeatureWindow {
    private val fpsDisplay by ToggleSetting("FPS Display").section("FPS")
    private val fpsColor by ColorSetting("FPS Color", Color(230, 114, 230), false).showIf { fpsDisplay.value }

    private val clockDisplay by ToggleSetting("Clock Display").section("Clock")
    private val seconds by ToggleSetting("Show Seconds").showIf { clockDisplay.value }
    private val twelveHour by ToggleSetting("12 Hour Clock").showIf { clockDisplay.value }
    private val clockColor by ColorSetting("Clock Color", Color(255, 134, 0), false).showIf { clockDisplay.value }

    private val tpsDisplay by ToggleSetting("TPS Display").section("TPS")
    private val tpsColor by ColorSetting("TPS Color", Color(0, 114, 255), false).showIf { tpsDisplay.value }

    private val pingDisplay by ToggleSetting("Ping Display").section("Ping")
    private val pingSuffix by ToggleSetting("Show ms suffix").showIf { pingDisplay.value }
    private val colorizePing by ToggleSetting("Colorize Ping").showIf { pingDisplay.value }
    private val pingColor by ColorSetting("Ping Color", Color(0, 114, 255), false).showIf { pingDisplay.value }

    private val cpsDisplay by ToggleSetting("CPS Display").section("CPS")
    private val cpsLabelPosition by DropdownSetting("CPS Label Position", 0, listOf("Right", "Left", "Hidden")).showIf { cpsDisplay.value }
    private val cpsNumberColor by ColorSetting("CPS Number Color", Color.WHITE, false).showIf { cpsDisplay.value }
    private val cpsSeparatorColor by ColorSetting("CPS Separator Color", Color(170, 170, 170), false).showIf { cpsDisplay.value }
    private val cpsLabelColor by ColorSetting("CPS Label Color", Color(85, 255, 255), false).showIf { cpsDisplay.value && cpsLabelPosition.value != 2 }

    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val clockFormatSeconds = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val clockFormat12 = DateTimeFormatter.ofPattern("h:mm a")
    private val clockFormatSeconds12 = DateTimeFormatter.ofPattern("h:mm:ss a")

    private val leftClicks = mutableListOf<Long>()
    private val rightClicks = mutableListOf<Long>()

    override fun init() {
        hudElement("ClockDisplay", { clockDisplay.value }) { ctx, _ ->
            val text = LocalTime.now().format(if (twelveHour.value) (if (seconds.value) clockFormatSeconds12 else clockFormat12) else if (seconds.value) clockFormatSeconds else clockFormat)
            ctx.drawString(text, 0, 0, clockColor.value)
            return@hudElement text.width() to 9
        }

        hudElement("CPS Display", { cpsDisplay.value }) { ctx, _ ->
            val comp = Component.empty().apply {
                if (cpsLabelPosition.value == 1) append(Component.literal("CPS ").withColor(cpsLabelColor.value.rgb))
                append(Component.literal(getCps(leftClicks).toString()).withColor(cpsNumberColor.value.rgb))
                append(Component.literal(" | ").withColor(cpsSeparatorColor.value.rgb))
                append(Component.literal(getCps(rightClicks).toString()).withColor(cpsNumberColor.value.rgb))
                if (cpsLabelPosition.value == 0) append(Component.literal(" CPS").withColor(cpsLabelColor.value.rgb))
            }

            ctx.text(mc.font, comp, 2, 2, - 1)

            return@hudElement mc.font.width(comp) + 4 to 11
        }

        hudElement("FpsDisplay", { fpsDisplay.value }) { ctx, _ ->
            val text = "FPS: &f${mc.fps}"
            ctx.drawString(text, 0, 0, fpsColor.value)
            return@hudElement text.width() to 9
        }

        hudElement("TpsDisplay", { tpsDisplay.value }) { ctx, example ->
            val text = "TPS: &f${if (example) 20 else ServerUtils.tps.toFixed(1)}"
            ctx.drawString(text, 0, 0, tpsColor.value)
            return@hudElement text.width() to 9
        }

        hudElement("PingDisplay", { pingDisplay.value }) { ctx, _ ->
            val ping = ServerUtils.currentPing
            val color = when {
                ! colorizePing.value -> "&f"
                ping < 50 -> "&a"
                ping < 100 -> "&2"
                ping < 150 -> "&e"
                ping < 250 -> "&6"
                else -> "&c"
            }
            val text = "Ping: $color$ping${if (pingSuffix.value) "ms" else ""}"
            ctx.drawString(text, 0, 0, pingColor.value)
            return@hudElement text.width() to 9
        }

        register<WorldChangeEvent> {
            leftClicks.clear()
            rightClicks.clear()
        }
    }

    private fun getCps(list: MutableList<Long>): Int {
        val now = System.currentTimeMillis()
        list.removeIf { now - it > 1000 }
        return list.size
    }

    @JvmStatic
    fun addLeftClick() {
        if (! enabled || ! cpsDisplay.value) return
        leftClicks.add(System.currentTimeMillis())
    }

    @JvmStatic
    fun addRightClick() {
        if (! enabled || ! cpsDisplay.value) return
        rightClicks.add(System.currentTimeMillis())
    }
}