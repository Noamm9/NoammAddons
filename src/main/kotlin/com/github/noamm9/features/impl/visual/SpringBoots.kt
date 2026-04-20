package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.NumbersUtils.toFixed
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

object SpringBoots: Feature("Shows the spring boots charge progress on screen.") {
    private val show2DHud by ToggleSetting("Show in HUD", true).section("HUD")
    private val drawMode by DropdownSetting("Draw Mode", 0, listOf("Percentage", "Blocks")).showIf { show2DHud.value }

    private val show3DBox by ToggleSetting("Draw Box", true).section("Box")
    private val renderMode by DropdownSetting("Render Mode", 1, listOf("Fill", "Outline", "Filled Outline")).showIf { show3DBox.value }
    private val boxColor by ColorSetting("Box Color", Color.GREEN, false).showIf { show3DBox.value }
    private val boxPhase by ToggleSetting("See Through Walls", true).showIf { show3DBox.value }

    private val highPitches = floatArrayOf(0.82539684f, 0.8888889f, 0.93650794f, 1.0476191f, 1.1746032f, 1.3174603f, 1.7777778f)
    private val resetPitches = floatArrayOf(0.0952381f, 1.6984127f)
    private const val LOW_PITCH = 0.6984127f
    private val heights = floatArrayOf(
        0.0f, 3.0f, 6.5f, 9.0f, 11.5f, 13.5f, 16.0f, 18.0f, 19.0f,
        20.5f, 22.5f, 25.0f, 26.5f, 28.0f, 29.0f, 30.0f, 31.0f, 33.0f,
        34.0f, 35.5f, 37.0f, 38.0f, 39.5f, 40.0f, 41.0f, 42.5f, 43.5f,
        44.0f, 45.0f, 46.0f, 47.0f, 48.0f, 49.0f, 50.0f, 51.0f, 52.0f,
        53.0f, 54.0f, 55.0f, 56.0f, 57.0f, 58.0f, 59.0f, 60.0f, 61.0f
    )

    private var currentHeight = 0f
    private var highs = 0
    private var lows = 0

    private fun reset() {
        currentHeight = 0f
        highs = 0
        lows = 0
    }

    override fun init() {
        hudElement(
            name = "Spring Boots HUD",
            enabled = { show2DHud.value },
            shouldDraw = { currentHeight > 0 },
            centered = true
        ) { ctx, example ->
            val isPercent = drawMode.value == 0
            val h = if (example) 33.0f else currentHeight
            val displayValue = if (isPercent) (h / heights.last()) * 100f else h
            val suffix = if (isPercent) "%" else ""
            val color = ColorUtils.colorCodeByPercent(lows + highs, heights.lastIndex, reversed = true)
            val prefix = if (isPercent) "Charge: " else "Blocks: "
            val text = prefix + color + displayValue.toFixed(1) + suffix

            Render2D.drawCenteredString(ctx, text, 0, 0)
            return@hudElement text.width().toFloat() to 9f
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            val player = mc.player?.takeIf { it.onGround() } ?: return@register

            val pitch = packet.pitch
            val id = packet.sound.value().location
            val isNote = SoundEvents.NOTE_BLOCK_PLING.`is`(id)
            val isFirework = SoundEvents.FIREWORK_ROCKET_LAUNCH.location == id

            if (! isNote && ! isFirework) return@register

            when {
                isNote && player.isCrouching && player.getItemBySlot(EquipmentSlot.FEET)?.skyblockId == "SPRING_BOOTS" -> {
                    when {
                        pitch == LOW_PITCH -> lows = (lows + 1).coerceAtMost(2)
                        highPitches.any { it == pitch } -> highs ++
                    }
                    currentHeight = heights[(lows + highs).coerceIn(heights.indices)]
                }

                isFirework && resetPitches.any { it == pitch } -> reset()
            }
        }

        register<TickEvent.End> {
            if (! LocationUtils.inSkyblock) return@register
            if (currentHeight <= 0) return@register
            val player = mc.player as? ILocalPlayer ?: return@register
            if (! player.isSneakingServer || ! player.onGroundServer()) reset()
        }

        register<RenderWorldEvent> {
            if (! show3DBox.value) return@register
            if (currentHeight <= 0f) return@register
            val pos = mc.player?.renderVec ?: return@register

            Render3D.renderBox(
                ctx = event.ctx,
                x = pos.x,
                y = pos.y + currentHeight.toDouble(),
                z = pos.z,
                width = 1.0,
                height = 1.0,
                outlineColor = boxColor.value,
                fillColor = boxColor.value.withAlpha(50),
                outline = renderMode.value.equalsOneOf(1, 2),
                fill = renderMode.value.equalsOneOf(0, 2),
                phase = boxPhase.value
            )
        }
    }
}