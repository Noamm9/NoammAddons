package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.event.impl.TerminalEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.floor7.terminals.impl.*
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.isVisable
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawFloatingRect
import com.github.noamm9.utils.render.Render2D.drawRect
import gg.essential.universal.UGraphics
import gg.essential.universal.UKeyboard
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object TerminalSolver: Feature("Renders solutions for Floor 7 terminals."), ICustomMenu {
    private val redGreenTerm by ToggleSetting("Red-Green", true).section("Toggles")
    private val colorsTerm by ToggleSetting("Colors", true)
    private val startsWithTerm by ToggleSetting("Start-With", true)
    private val rubixTerm by ToggleSetting("Rubix", true)
    private val numbersTerm by ToggleSetting("Numbers", true)
    private val melodyTerm by ToggleSetting("Melody", true)

    private val soundsEnabled by ToggleSetting("Terminal Sounds", true).section("Sounds")
    private val clickSound = createSoundSettings("Click Sound", SoundEvents.NOTE_BLOCK_PLING.value()) { soundsEnabled.value }

    private val scale by SliderSetting("Custom Menu's Scale", 1f, 0.1f, 2f, 0.01f).section("Settings - UI")
    private val slotGap by SliderSetting("Slot Gap", 0f, 0f, 8f, 0.5f, "px")
    private val padding by SliderSetting("Padding", 2f, 0f, 12f, 0.5f, "px")
    private val slotStyle by DropdownSetting("Slot Style", 0, listOf("Rect", "Bordered-Rect", "Button"))
    val breakTimeout by SliderSetting("Break Timeout", 500, 300, 800, 50, "ms").withDescription("How long should the solver wait for a click to be confirmed by hypixel")
    private val backgroundColor by ColorSetting("Background Color", Color(0, 0, 0, 100))
    private val borderColor by ColorSetting("Border Color", Color(255, 255, 255))
    private val titleColor by ColorSetting("Title Text Color", Color.WHITE)
    private val overlayTextColor by ColorSetting("Overlay Text Color", Color.WHITE)
    private val solutionColor by ColorSetting("Solution Color", Color(0, 255, 0, 130))

    val positiveColor by ColorSetting("Rubix: Positive (+)", Color(0, 114, 255, 130)).section("Rubix").showIf { rubixTerm.value }
    val negativeColor by ColorSetting("Rubix: Negative (-)", Color(205, 0, 0, 130)).showIf { rubixTerm.value }

    val showNumbers by ToggleSetting("Show Numbers").section("Numbers").showIf { numbersTerm.value }
    val firstColor by ColorSetting("1st Click Color", Color(0, 255, 0, 130)).showIf { numbersTerm.value }
    val secondColor by ColorSetting("2nd Click Color", Color(255, 255, 120, 130)).showIf { numbersTerm.value }
    val thirdColor by ColorSetting("3rd Click Color", Color(200, 0, 0, 130)).showIf { numbersTerm.value }

    val columnColor by ColorSetting("Melody: Column", Color(255, 0, 255, 130)).section("Melody").showIf { melodyTerm.value }
    val indicatorColor by ColorSetting("Melody: Indicator", Color(255, 116, 0, 130)).showIf { melodyTerm.value }
    val wrongColor by ColorSetting("Melody: Wrong", Color(255, 0, 0, 130)).showIf { melodyTerm.value }

    private var cachedMinCol: Int? = null
    private var cachedMinRow: Int? = null

    private var hoveredSlot: Int? = null

    override fun init() {
        register<TerminalEvent.Open> {
            cachedMinCol = null
            cachedMinRow = null
            hoveredSlot = null
        }

        register<ScreenEvent.PreRender> {
            if (! TerminalListener.inTerm) return@register
            val handler = TerminalListener.currentHandler ?: return@register
            if (! handler.enabled()) return@register
            event.isCanceled = true

            Resolution.push(event.context)

            val uiScale = 3f * scale.value
            val screenWidth = Resolution.width / uiScale
            val screenHeight = Resolution.height / uiScale
            val (gridWidth, gridHeight) = handler.gridSize
            val gap = slotGap.value
            val pad = padding.value
            val slotSize = 16f

            val width = gridWidth * slotSize + (gridWidth - 1) * gap
            val height = gridHeight * slotSize + (gridHeight - 1) * gap
            val offsetX = screenWidth / 2f - width / 2f
            val offsetY = screenHeight / 2f - height / 2f
            val mx = (Resolution.getMouseX() / uiScale) - offsetX
            val my = (Resolution.getMouseY() / uiScale) - offsetY

            event.context.pose().pushMatrix()
            event.context.pose().scale(uiScale, uiScale)
            event.context.pose().translate(offsetX, offsetY)

            if (titleColor.value.isVisable()) event.context.drawCenteredString(handler.displayName, width / 2f, - 15f - pad, color = titleColor.value, scale = 1.2f)
            if (backgroundColor.value.isVisable()) event.context.drawRect(- pad, - pad, width + pad * 2, height + pad * 2, backgroundColor.value)
            if (borderColor.value.isVisable()) event.context.drawBorder(- pad, - pad, width + pad * 2, height + pad * 2, borderColor.value)

            val solutionSlots = handler.solution.map { it.slotId }
            val minCol = cachedMinCol ?: if (solutionSlots.isEmpty()) 0 else solutionSlots.minOf { it % 9 }.also { cachedMinCol = it }
            val minRow = cachedMinRow ?: if (solutionSlots.isEmpty()) 0 else solutionSlots.minOf { it / 9 }.also { cachedMinRow = it }

            fun slotPos(slot: Int): Pair<Float, Float> {
                val col = slot % 9 - minCol
                val row = slot / 9 - minRow
                val x = col * (slotSize + gap)
                val y = row * (slotSize + gap)
                return x to y
            }

            hoveredSlot = run {
                if (mx < 0 || my < 0 || mx > width && my > height) return@run null

                val cell = slotSize + gap
                val col = (mx / cell).toInt() + minCol
                val row = (my / cell).toInt() + minRow

                return@run row * 9 + col
            }

            handler.solution.forEachIndexed { index, click ->
                val slot = click.slotId
                val (slotX, slotY) = slotPos(slot)

                handler.renderSlot(event.context, slotX, slotY, index, click, solutionColor.value)

                val item = TerminalListener.currentItems[slot]
                if (item != null && "terminal" in NoammAddons.debugFlags) {
                    event.context.item(item, slotX.toInt(), slotY.toInt())
                    event.context.itemDecorations(mc.font, item, slotX.toInt(), slotY.toInt())
                }
            }

            handler.render(event.context, solutionColor.value, ::slotPos, mx, my, ::hoveredSlot)

            event.context.pose().popMatrix()
            Resolution.pop(event.context)
        }

        register<ContainerEvent.MouseClick> {
            val handler = TerminalListener.currentHandler ?: return@register
            if (! handler.enabled()) return@register
            event.isCanceled = true
            handler.click()
        }

        register<ContainerEvent.Keyboard> {
            if (event.key.equalsOneOf(KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value, UKeyboard.KEY_ESCAPE)) return@register
            val handler = TerminalListener.currentHandler ?: return@register
            if (! handler.enabled()) return@register
            event.isCanceled = true
            if (event.key != KeyMappingHelper.getBoundKeyOf(mc.options.keyDrop).value) return@register
            handler.click()
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! TerminalListener.inTerm) return@register
            if (! soundsEnabled.value) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            if (packet.sound.value() != SoundEvents.NOTE_BLOCK_PLING.value()) return@register
            if (packet.volume != 8f || packet.pitch != 4.047619f) return@register
            clickSound.action.invoke()
            event.isCanceled = true
        }
    }

    fun spanFor(count: Int) = count * 16f + (count - 1) * slotGap.value

    fun drawSlot(ctx: GuiGraphicsExtractor, x: Number, y: Number, color: Color, w: Number = 16, h: Number = 16) {
        when (slotStyle.value) {
            0 -> ctx.drawRect(x, y, w, h, color)
            1 -> {
                ctx.drawRect(x, y, w, h, color.withAlpha(40))
                ctx.drawBorder(x, y, w, h, color)
            }

            2 -> ctx.drawFloatingRect(x, y, w, h, color.darker())
        }
    }

    fun drawCenteredText(ctx: GuiGraphicsExtractor, text: String, slotX: Number, slotY: Number) {
        val centerX = slotX.toFloat() + 8f
        val centerY = slotY.toFloat() + 8f - UGraphics.getFontHeight() / 2
        ctx.drawCenteredString(text, centerX, centerY, color = overlayTextColor.value)
    }

    private fun Terminal.click() {
        if (TerminalListener.checkFcDelay()) return
        val slot = hoveredSlot ?: return
        val click = getClickForSlot(slot) ?: return

        predict(click)
        click.send()
    }

    private fun Terminal.enabled() = when (this) {
        is NumberTerminal -> numbersTerm.value
        is ColorsTerminal -> colorsTerm.value
        is MelodyTerminal -> melodyTerm.value
        is RubixTerminal -> rubixTerm.value
        is RedGreenTerminal -> redGreenTerm.value
        is StartWithTerminal -> startsWithTerm.value
    }

    override fun isActive() = TerminalListener.inTerm
}