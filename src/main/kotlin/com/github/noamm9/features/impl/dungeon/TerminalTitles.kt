package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketRecivedEvent
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.componnents.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.ui.hud.getValue
import com.github.noamm9.ui.hud.provideDelegate
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket

object TerminalTitles : Feature("Replaces the Terminal title with a better one") {
    private val duration by SliderSetting("Duration", 2.5, 0.5, 6, 0.5).withDescription("Duration of the title in seconds")
    private val mode by DropdownSetting(
        "Mode",
        0,
        listOf("Name + Term + Progress", "Term + Progress", "Progress")
    )
    private val bracket by DropdownSetting("Bracket Type", 0, listOf("()", "[]", "<>", "{}"))
    private val phaseDone by ToggleSetting("Phase Done").withDescription("Renders Phase Done instead of 7/8 or 8/8")

    private val hud by hudElement("Terminal Title") { context, example ->
        val str = if(example) "terminal title" else drawable
        Render2D.drawString(context, str, 0, 0)
        return@hudElement str.width().toFloat() to 9f
    }

    private val mainRegex = Regex("(.+) (?:activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")  //ign, type, td, tm

    private var timer = 0
    private var drawable = ""

    private val titleListener = register<MainThreadPacketRecivedEvent.Pre> {
        //if(!LocationUtils.inDungeon || LocationUtils.F7Phase !== 3) return@register
        if (event.packet !is ClientboundSetSubtitleTextPacket) return@register
        val title = event.packet.text().unformattedText
        val match = mainRegex.find(title)?.destructured ?: return@register
        val (ign, type, td, tm) = match
        drawable = handleTitle(ign, type, td.toInt(), tm.toInt())
        timer = duration.value.toInt() * 1000
        tickListener.register()
        event.isCanceled = true
    }

    private val tickListener = register<TickEvent.Start> {
        //if(!LocationUtils.inDungeon || LocationUtils.F7Phase !== 3) return@register
        if(timer <= 0) {drawable = ""; this.listener.unregister()}
        timer -= 50
    }.unregister()

    private fun handleTitle(ign: String, type: String, td: Int, tm: Int): String {
        val color = ColorUtils.colorCodeByPresent(td, tm)
        if(phaseDone.value && td == tm) return "&a&lPhase Done!"
        val brackets = when (bracket.value) {
            0 -> listOf("(", ")")
            1 -> listOf("[", "]")
            2 -> listOf("<", ">")
            3 -> listOf("{", "}")
            else -> listOf("", "")
        }

        val typE = when(type) {
            "terminal" -> "&5Terminal"
            "device" -> "&bDevice"
            "lever" -> "&cLever"
            else -> ""
        }

        return when(mode.value) {
            0 -> "$ign $typE &7${brackets[0]}$color$td&8/&a$tm&7${brackets[1]}"
            1 -> "$typE &7${brackets[0]}$color$td&8/&a$tm&7${brackets[1]}"
            2 -> "&7${brackets[0]}$color$td&8/&a$tm&7${brackets[1]}"
            else -> ""
        }
    }
}