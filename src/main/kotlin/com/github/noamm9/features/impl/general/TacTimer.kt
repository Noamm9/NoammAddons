package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents

object TacTimer: Feature("Shows a 3 seconds timer when you use the Tactical Insertion ability") {
    private val reverseTimer by ToggleSetting("Reverse Timer").withDescription("Flips the Timer to count up instead of down").section("Timer")
    private val prefix by ToggleSetting("Tac Prefix", true).withDescription("Adds \"Tac:\" prefix to the timer")
    private val suffix by ToggleSetting("Timer Suffix").withDescription("Adds \"s\" suffix to the timer")

    private val markBlock by ToggleSetting("Start Waypoint").withDescription("Shows a waypoint at the start block of the Tactical Insertion ability").section("Waypoint")
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline")).showIf { markBlock.value }
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { ! markBlock.value || mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { ! markBlock.value || mode.value == 1 }

    private var lastClick = System.currentTimeMillis()
    private var pos: BlockPos? = null
    private var ticks = 0

    override fun init() {
        hudElement("TacTimer", shouldDraw = { ticks > 0 }, centered = true) { ctx, example ->
            val text = getTimer(if (example) 60 else ticks)
            Render2D.drawCenteredString(ctx, text, 0, 0)
            text.width().toFloat() to 9f
        }

        register<PacketEvent.Sent> {
            if (! LocationUtils.inSkyblock) return@register
            val packet = event.packet as? ServerboundUseItemPacket ?: return@register
            val item = mc.player?.getItemInHand(packet.hand)?.takeUnless { it.isEmpty } ?: return@register
            if (item.skyblockId != "TACTICAL_INSERTION") return@register
            lastClick = System.currentTimeMillis()
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            if (System.currentTimeMillis() - lastClick > 500) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            if (packet.sound.value() != SoundEvents.FLINTANDSTEEL_USE) return@register
            if (packet.pitch != 0.74603176f) return@register

            ticks = 60
            if (markBlock.value) pos = mc.player?.blockPosition()
        }

        register<RenderWorldEvent> {
            if (! markBlock.value) return@register
            val position = pos ?: return@register
            Render3D.renderBlock(
                event.ctx, position,
                outlineColor.value, fillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = true,
            )
        }

        register<TickEvent.Server> { if (ticks > 0) ticks -- else pos = null }

        register<WorldChangeEvent> {
            lastClick = System.currentTimeMillis()
            pos = null
            ticks = 0
        }
    }

    fun getTimer(ticks: Int): String {
        val timeLeft = (if (reverseTimer.value) 60 - ticks else ticks) / 20.0
        val color = ColorUtils.colorCodeByPercent(ticks, 60)
        val prefix = if (prefix.value) "&5Tac: " else ""
        val suffix = if (suffix.value) "s" else ""
        return "$prefix$color${timeLeft.toFixed(1)}$suffix"
    }
}