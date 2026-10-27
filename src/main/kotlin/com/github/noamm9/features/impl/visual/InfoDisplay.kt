package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.client.KeyMapping
import java.awt.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object InfoDisplay: Feature("Displays the system time, clicks per second, FPS, TPS and Keystrokes on screen.") {
    private val fpsDisplay by ToggleSetting("FPS Display").section("FPS")
    private val fpsColor by ColorSetting("FPS Color", Color(230, 114, 230), false).showIf { fpsDisplay.value }

    private val clockDisplay by ToggleSetting("Clock Display").section("Clock")
    private val seconds by ToggleSetting("Show Seconds").showIf { clockDisplay.value }
    private val clockColor by ColorSetting("Clock Color", Color(255, 134, 0), false).showIf { clockDisplay.value }

    private val tpsDisplay by ToggleSetting("TPS Display").section("TPS")
    private val tpsColor by ColorSetting("TPS Color", Color(0, 114, 255), false).showIf { tpsDisplay.value }

    private val pingDisplay by ToggleSetting("Ping Display").section("Ping")
    private val pingSuffix by ToggleSetting("Show ms suffix").showIf { pingDisplay.value }
    private val colorizePing by ToggleSetting("Colorize Ping").showIf { pingDisplay.value }
    private val pingColor by ColorSetting("Ping Color", Color(0, 114, 255)).showIf { pingDisplay.value }

    private val cpsDisplay by ToggleSetting("CPS Display").section("CPS")

    private val leftClicks = mutableListOf<Long>()
    private val rightClicks = mutableListOf<Long>()

    private val keystrokes by ToggleSetting("Keystrokes").section("Keystrokes")
    private val toggleWASD by ToggleSetting("Toggle WASD").showIf { keystrokes.value }
    private val toggleClicks by ToggleSetting("Toggle Clicks").showIf { keystrokes.value }
    private val toggleSpace by ToggleSetting("Toggle Space").showIf { keystrokes.value }
    private val held = mutableSetOf<KeyMapping>()
    private val pressedBg = Color(255, 255, 255, 170)
    private val idleBg = Color(0, 0, 0, 110)

    override fun init() {
        hudElement("ClockDisplay", { clockDisplay.value }) { ctx, _ ->
            val text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm${if (seconds.value) ":ss" else ""}"))
            ctx.drawString(text, 0, 0, clockColor.value)
            return@hudElement text.width() to 12
        }

        hudElement("CPS Display", { cpsDisplay.value }) { ctx, _ ->
            val l = getCps(leftClicks)
            val r = getCps(rightClicks)
            val text = "§f$l §7| §f$r §bCPS"
            ctx.drawString(text, 2f, 2f)
            return@hudElement text.width() + 4f to 12
        }

        hudElement("FpsDisplay", { fpsDisplay.value }) { ctx, _ ->
            val text = "${mc.fps} fps"
            ctx.drawString(text, 0, 0, fpsColor.value)
            return@hudElement text.width() to 12
        }

        hudElement("TpsDisplay", { tpsDisplay.value }) { ctx, example ->
            val text = "TPS: &f${if (example) 20 else ServerUtils.tps.toFixed(1)}"
            ctx.drawString(text, 0, 0, tpsColor.value)
            return@hudElement text.width() to 12
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
            return@hudElement text.width() to 12
        }

        hudElement("Keystrokes", { keystrokes.value }) { ctx, example ->
            val size = 20
            val gap = 2
            val step = size + gap
            val totalW = step * 3 - gap
            val o = mc.options
            var y = 0

            if (toggleWASD.value) {
                val w = ! example && o.keyUp in held
                ctx.drawRect(step, y, size, size, if (w) pressedBg else idleBg)
                ctx.drawCenteredString("W", step + size / 2, y + 6, if (w) Color.BLACK else Color.WHITE, shadow = false)
                y += step

                val a = ! example && o.keyLeft in held
                ctx.drawRect(0, y, size, size, if (a) pressedBg else idleBg)
                ctx.drawCenteredString("A", size / 2, y + 6, if (a) Color.BLACK else Color.WHITE, shadow = false)

                val s = ! example && o.keyDown in held
                ctx.drawRect(step, y, size, size, if (s) pressedBg else idleBg)
                ctx.drawCenteredString("S", step + size / 2, y + 6, if (s) Color.BLACK else Color.WHITE, shadow = false)

                val d = ! example && o.keyRight in held
                ctx.drawRect(step * 2, y, size, size, if (d) pressedBg else idleBg)
                ctx.drawCenteredString("D", step * 2 + size / 2, y + 6, if (d) Color.BLACK else Color.WHITE, shadow = false)
                y += step
            }

            if (toggleClicks.value) {
                val w = (totalW - gap) / 2
                val h = 26
                val l = ! example && o.keyAttack.isDown
                val r = ! example && o.keyUse.isDown

                ctx.drawRect(0, y, w, h, if (l) pressedBg else idleBg)
                ctx.drawCenteredString("LMB", w / 2, y + 7, if (l) Color.BLACK else Color.WHITE, 0.7f, false)
                ctx.drawCenteredString("${getCps(leftClicks)} CPS", w / 2, y + 13.5f, if (l) Color.BLACK else Color.WHITE, 0.7f, false)

                ctx.drawRect(w + gap, y, w, h, if (r) pressedBg else idleBg)
                ctx.drawCenteredString("RMB", w + gap + w / 2, y + 7, if (r) Color.BLACK else Color.WHITE, 0.7f, false)
                ctx.drawCenteredString("${getCps(rightClicks)} CPS", w + gap + w / 2, y + 13.5f, if (r) Color.BLACK else Color.WHITE, 0.7f, false)
                y += h + gap
            }

            if (toggleSpace.value) {
                val sp = ! example && o.keyJump in held
                ctx.drawRect(0, y, totalW, 12, if (sp) pressedBg else idleBg)
                ctx.drawRect(totalW / 4, y + 6, totalW / 2, 1, if (sp) Color.BLACK else Color.WHITE)
                y += 12 + gap
            }

            return@hudElement totalW to maxOf(y - gap, 1)
        }
        register<KeyboardEvent.KeyPressed> {
            val o = mc.options
            listOf(o.keyUp, o.keyLeft, o.keyDown, o.keyRight, o.keyJump).forEach { key ->
                if (key.matches(event.keyEvent)) held.add(key)
            }
        }

        register<KeyboardEvent.KeyReleased> {
            val o = mc.options
            listOf(o.keyUp, o.keyLeft, o.keyDown, o.keyRight, o.keyJump).forEach { key ->
                if (key.matches(event.keyEvent)) held.remove(key)
            }
        }

        register<WorldChangeEvent> {
            leftClicks.clear()
            rightClicks.clear()
            held.clear()
        }
    }

    private fun getCps(list: MutableList<Long>): Int {
        val now = System.currentTimeMillis()
        list.removeIf { now - it > 1000 }
        return list.size
    }

    @JvmStatic
    fun addLeftClick() {
        if (! enabled || (! cpsDisplay.value && ! keystrokes.value)) return
        leftClicks.add(System.currentTimeMillis())
    }

    @JvmStatic
    fun addRightClick() {
        if (! enabled || (! cpsDisplay.value && ! keystrokes.value)) return
        rightClicks.add(System.currentTimeMillis())
    }
}