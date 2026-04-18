package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.ui.hud.getValue
import com.github.noamm9.ui.hud.provideDelegate
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import java.awt.Color

object SpringBoots : Feature("Shows the spring boots charge progress on screen.") {
    private val show2DHud by ToggleSetting("Draw Height", true)
    private val textColor by ColorSetting("Text Color", Color.WHITE, false).showIf { show2DHud.value }
    private val drawMode by DropdownSetting("Draw Mode", 0, listOf("Blocks", "Percentage")).showIf { show2DHud.value }

    private val show3DBox by ToggleSetting("Draw Box", true)
    private val renderMode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline")).showIf { show3DBox.value }
    private val boxColor by ColorSetting("Box Color", Color.GREEN, false).showIf { show3DBox.value }
    private val boxPhase by ToggleSetting("See Through Walls", true).showIf { show3DBox.value }

    private val highPitches = setOf(0.82539684f, 0.8888889f, 0.93650794f, 1.0476191f, 1.1746032f, 1.3174603f, 1.7777778f)
    private const val LOW_PITCH = 0.6984127f
    private val resetPitches = floatArrayOf(0.0952381f, 1.6984127f)

    private val heights = listOf(
        0.0f, 3.0f, 6.5f, 9.0f, 11.5f, 13.5f, 16.0f, 18.0f, 19.0f,
        20.5f, 22.5f, 25.0f, 26.5f, 28.0f, 29.0f, 30.0f, 31.0f, 33.0f,
        34.0f, 35.5f, 37.0f, 38.0f, 39.5f, 40.0f, 41.0f, 42.5f, 43.5f,
        44.0f, 45.0f, 46.0f, 47.0f, 48.0f, 49.0f, 50.0f, 51.0f, 52.0f,
        53.0f, 54.0f, 55.0f, 56.0f, 57.0f, 58.0f, 59.0f, 60.0f, 61.0f
    )
    private val MAX_HEIGHT = heights.last()

    private var currentHeight = 0f
    private var highs = 0
    private var lows = 0

    private val isWearing get() = mc.player?.getItemBySlot(EquipmentSlot.FEET)?.skyblockId == "SPRING_BOOTS"

    private fun reset() {
        highs = 0
        lows = 0
        currentHeight = 0f
    }

    private fun getDynamicColor(currentIndex: Int): Color {
        val percent = (currentIndex / (heights.size - 1).toDouble()).coerceIn(0.0, 1.0)
        return Color(Color.HSBtoRGB((percent * 0.33).toFloat(), 1f, 1f))
    }

    private val hud by hudElement(name = "Spring Boots Height", enabled = { LocationUtils.inSkyblock }, shouldDraw = { show2DHud.value }) { ctx, example ->
        val h = if (example) 33.0f else currentHeight
        if (h <= 0f && !example) return@hudElement 0f to 0f

        val isPercent = drawMode.value == 1
        val displayValue = if (isPercent) (h / MAX_HEIGHT) * 100f else h
        val suffix = if (isPercent) "%" else ""
        val labelText = "Height: "
        val valueText = "${"%.1f".format(displayValue)}$suffix"
        val staticColor = textColor.value
        val dynamicColor = if (example) getDynamicColor(17) else getDynamicColor(lows + highs)

        Render2D.drawString(ctx, labelText, 0, 0, staticColor)

        val labelWidth = labelText.width()
        Render2D.drawString(ctx, valueText, labelWidth, 0, dynamicColor)

        return@hudElement (labelWidth + valueText.width()).toFloat() to 9f
    }

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (!LocationUtils.inSkyblock) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            val player = mc.player ?: return@register
            val id = packet.sound.value().location
            val pitch = packet.pitch

            when {
                SoundEvents.NOTE_BLOCK_PLING.`is`(id) && player.isCrouching && isWearing -> {
                    when {
                        pitch == LOW_PITCH -> lows = (lows + 1).coerceAtMost(2)
                        pitch in highPitches -> highs++
                    }
                    currentHeight = heights[(lows + highs).coerceIn(heights.indices)]
                }
                SoundEvents.FIREWORK_ROCKET_LAUNCH.location == id && pitch.equalsOneOf(resetPitches[0], resetPitches[1]) -> reset()
            }
        }

        register<TickEvent.End> {
            if (!LocationUtils.inSkyblock) return@register
            val player = mc.player ?: return@register
            if (!player.isCrouching || !isWearing) reset()
        }

        register<RenderWorldEvent> {
            if (!LocationUtils.inSkyblock || !show3DBox.value || currentHeight == 0f) return@register
            val pos = mc.player?.renderVec ?: return@register

            val fill = Color(boxColor.value.red, boxColor.value.green, boxColor.value.blue, 50)

            Render3D.renderBox(
                ctx = event.ctx,
                x = pos.x,
                y = pos.y + currentHeight.toDouble(),
                z = pos.z,
                width = 1.0,
                height = 1.0,
                outlineColor = boxColor.value,
                fillColor = fill,
                outline = renderMode.value.equalsOneOf(1, 2),
                fill = renderMode.value.equalsOneOf(0, 2),
                phase = boxPhase.value
            )
        }
    }
}