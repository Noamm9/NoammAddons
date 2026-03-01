package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import java.awt.Color

object TerminalTitles: Feature("Reformats the Terminal titles on P3.") {
    private val duration by SliderSetting("Duration", 2.5, 0.5, 6, 0.5).withDescription("Duration of the title in seconds")
    private val mode by DropdownSetting("Mode", 0, listOf("Name + Term + Progress", "Term + Progress", "Progress"))
    private val bracket by DropdownSetting("Bracket Type", 0, listOf("()", "[]", "<>", "{}"))
    private val phaseDone by ToggleSetting("Phase Done").withDescription("Renders Phase Done instead of 7/7 or 8/8")
    private val gateTitles by ToggleSetting("Gate Titles").withDescription("Reformats Gate related Titles.")

    private val hud = object: HudElement() {
        override val name = "Terminal Titles"
        override val toggle get() = TerminalTitles.enabled
        override val shouldDraw get() = titleStr.isNotBlank()

        override fun draw(ctx: GuiGraphics, example: Boolean): Pair<Float, Float> {
            val str = if (example) handleTitle(mc.user.name, "terminal", 6, 7) else titleStr
            Render2D.drawCenteredString(ctx, str, 0, 0)
            return str.width().toFloat() to 9f
        }

        override fun isHovered(mx: Int, my: Int): Boolean {
            val halfWidth = width * scale / 2
            return mx >= x - halfWidth && mx <= x + halfWidth && my >= y && my <= y + (height * scale)
        }

        override fun drawBackground(ctx: GuiGraphics, mx: Int, my: Int) {
            val scaledW = width * scale
            val scaledH = height * scale
            val drawX = x - scaledW / 2
            val drawY = y

            val hovered = mx >= drawX && mx <= drawX + scaledW && my >= drawY && my <= drawY + scaledH
            val borderColor = if (isDragging || hovered) Style.accentColor else Color(255, 255, 255, 40)

            Render2D.drawRect(ctx, drawX, drawY, scaledW.toDouble(), scaledH.toDouble(), Color(10, 10, 10, 150))
            Render2D.drawRect(ctx, drawX, drawY, scaledW.toDouble(), 1.0, borderColor)
            Render2D.drawRect(ctx, drawX, drawY + scaledH - 1, scaledW.toDouble(), 1.0, borderColor)
        }
    }

    override fun init() {
        hudElements.add(hud)

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inDungeon || LocationUtils.F7Phase != 3) return@register
            if (event.packet !is ClientboundSetSubtitleTextPacket) return@register
            val title = event.packet.text.unformattedText

            if (gateTitles.value) when (title) {
                "The gate has been destroyed!" -> {
                    titleStr = "&cGate Destroyed!"
                    timer = duration.value.toInt() * 1000
                    tickListener.register()
                    event.isCanceled = true
                    return@register
                }

                "The gate will open in 5 seconds!" -> {
                    titleStr = "&c&lGATE!"
                    timer = duration.value.toInt() * 1000
                    tickListener.register()
                    event.isCanceled = true
                    return@register
                }
            }

            val (name, type, min, max) = mainRegex.find(event.packet.text().unformattedText)?.destructured ?: return@register
            titleStr = handleTitle(name, type, min.toInt(), max.toInt())
            timer = duration.value.toInt() * 1000
            tickListener.register()
            event.isCanceled = true
        }
    }

    private val mainRegex = Regex("(.+) (?:activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")

    private var titleStr = ""
    private var timer = 0

    private val tickListener = register<TickEvent.Start> {
        if (timer <= 0) {
            this.listener.unregister()
            titleStr = ""
        }

        timer -= 50
    }

    private fun handleTitle(name: String, type: String, min: Int, max: Int): String {
        val color = ColorUtils.colorCodeByPercent(min, max)
        if (phaseDone.value && min == max) return "&a&lPhase Done!"
        val brackets = when (bracket.value) {
            0 -> listOf("(", ")")
            1 -> listOf("[", "]")
            2 -> listOf("<", ">")
            3 -> listOf("{", "}")
            else -> listOf("", "")
        }

        val formattedType = when (type) {
            "terminal" -> "&5Terminal"
            "device" -> "&bDevice"
            "lever" -> "&cLever"
            else -> ""
        }

        val formattedName = (DungeonPlayer.get(name)?.clazz?.code ?: "&7") + name

        return when (mode.value) {
            0 -> "$formattedName $formattedType &f${brackets[0]}$color$min&8/&a$max&f${brackets[1]}"
            1 -> "$formattedType &f${brackets[0]}$color$min&f/&a$max&f${brackets[1]}"
            2 -> "&f${brackets[0]}$color$min&f/&a$max&f${brackets[1]}"
            else -> ""
        }
    }
}