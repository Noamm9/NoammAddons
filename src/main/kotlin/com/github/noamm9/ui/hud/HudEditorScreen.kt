package com.github.noamm9.ui.hud

import com.mojang.blaze3d.platform.InputConstants

import com.github.noamm9.config.ConfigManager
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.components.UIButton
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawLine
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HudEditorScreen: Screen(Component.literal("HudEditor")) {
    private companion object {
        const val SNAP_DISTANCE = 5f
        const val MIN_SCALE = 0.3f
        const val MAX_SCALE = 5f
        const val FIELD_WIDTH = 34f
        const val FIELD_HEIGHT = 14f
        val guideColor = Color(255, 220, 60)
        val bandColor = Color(80, 140, 255)
    }

    private val huds = FeatureManager.hudElements.filter(HudElement::toggle)
    private val selected = linkedSetOf<HudElement>()

    private var dragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private val dragOrigins = mutableMapOf<HudElement, Pair<Float, Float>>()

    private var bandStart: Pair<Float, Float>? = null
    private val verticalGuides = mutableListOf<Float>()
    private val horizontalGuides = mutableListOf<Float>()

    private var resetConfirmed = false
    private var gridEnabled = false
    private var snapEnabled = false
    private var gridSize = 10

    private var mouseX = 0f
    private var mouseY = 0f

    private val fields = listOf(
        "X" to { hud: HudElement -> hud.x.roundToInt().toString() },
        "Y" to { hud: HudElement -> hud.y.roundToInt().toString() },
        "Scale" to { hud: HudElement -> hud.scale.toFixed(1) }
    )

    private lateinit var gridToggleButton: UIButton

    override fun init() {
        super.init()

        val btnHeight = 20
        val center = width / 2

        addRenderableWidget(UIButton(center - 160, height - 40, 80, btnHeight, "§aSnap: ON") { button ->
            snapEnabled = ! snapEnabled
            button.message = Component.literal(if (snapEnabled) "§aSnap: ON" else "§7Snap: OFF")
        })

        gridToggleButton = addRenderableWidget(UIButton(center - 75, height - 40, 80, btnHeight, "§7Grid: OFF") {
            gridEnabled = ! gridEnabled
            updateGridButton()
        })

        addRenderableWidget(UIButton(center + 10, height - 40, 35, btnHeight, "§7-") {
            gridSize = (gridSize - 5).coerceAtLeast(5)
            if (! gridEnabled) gridEnabled = true
            updateGridButton()
        })

        addRenderableWidget(UIButton(center + 50, height - 40, 35, btnHeight, "§7+") {
            gridSize = (gridSize + 5).coerceAtMost(50)
            if (! gridEnabled) gridEnabled = true
            updateGridButton()
        })

        addRenderableWidget(UIButton(center + 90, height - 40, 70, btnHeight, "§cReset") { button ->
            if (! resetConfirmed) {
                button.message = Component.literal("§c§lConfirm?")
                resetConfirmed = true
                return@UIButton
            }
            resetConfirmed = false
            button.message = Component.literal("§cReset")
            FeatureManager.hudElements.forEach {
                it.x = HudElement.defaultX
                it.y = HudElement.defaultY
                it.scale = HudElement.defaultScale
                it.defaults.invoke(it)
            }
        })
    }

    private fun updateGridButton() {
        gridToggleButton.message = Component.literal(
            if (gridEnabled) "§aGrid: ON §7(${gridSize}px)" else "§7Grid: OFF"
        )
    }

    private fun hudAt(x: Float, y: Float) = huds.lastOrNull { it.bounds().contains(x, y) }

    private fun drawGrid(ctx: GuiGraphicsExtractor) {
        val gridColor = Color(255, 255, 255, 30).rgb
        val w = Resolution.width.toInt()
        val h = Resolution.height.toInt()

        var x = 0
        while (x <= w) {
            ctx.fill(x, 0, x + 1, h, gridColor)
            x += gridSize
        }

        var y = 0
        while (y <= h) {
            ctx.fill(0, y, w, y + 1, gridColor)
            y += gridSize
        }

        ctx.drawLine(0, h / 2, w, h / 2, Color.WHITE)
        ctx.drawLine(w / 2, 0, w / 2, h, Color.WHITE)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)
        this.mouseX = mouseX / Resolution.scale
        this.mouseY = mouseY / Resolution.scale

        if (dragging) updateDrag()
        if (gridEnabled) drawGrid(graphics)

        for (hud in huds) {
            hud.isDragging = dragging && hud in selected
            hud.drawBackground(graphics, this.mouseX.toInt(), this.mouseY.toInt())
            hud.renderElement(graphics, true)
            if (hud in selected) hud.bounds().run { graphics.drawBorder(left - 1f, top - 1f, right - left + 2f, bottom - top + 2f, Style.accentColor) }
        }

        verticalGuides.forEach { graphics.drawLine(it, 0f, it, Resolution.height, guideColor) }
        horizontalGuides.forEach { graphics.drawLine(0f, it, Resolution.width, it, guideColor) }

        bandStart?.let { (startX, startY) ->
            val left = min(startX, this.mouseX)
            val top = min(startY, this.mouseY)
            val w = abs(this.mouseX - startX)
            val h = abs(this.mouseY - startY)
            graphics.drawRect(left, top, w, h, bandColor.withAlpha(40))
            graphics.drawBorder(left, top, w, h, bandColor)
        }

        selected.singleOrNull()?.let { drawInfo(graphics, it) }

        Resolution.pop(graphics)
        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    private fun drawInfo(ctx: GuiGraphicsExtractor, hud: HudElement) {
        val labelsWidth = fields.sumOf { it.first.width() + 4 }
        val totalWidth = hud.name.width() + 10f + labelsWidth + fields.size * (FIELD_WIDTH + 8f) + 8f
        var x = (Resolution.width - totalWidth) / 2f
        val y = 8f

        ctx.drawRect(x, y, totalWidth, FIELD_HEIGHT + 6f, Color(15, 15, 15, 200))
        ctx.drawRect(x, y + FIELD_HEIGHT + 4f, totalWidth, 2f, Style.accentColor)
        x += 4f
        ctx.drawString(hud.name, x, y + 6f, Style.accentColor)
        x += hud.name.width() + 10f

        for ((label, getter) in fields) {
            ctx.drawString("§7$label", x, y + 6f)
            x += label.width() + 4f

            val value = getter(hud)
            ctx.drawRect(x, y + 3f, FIELD_WIDTH, FIELD_HEIGHT, Color(30, 30, 30, 200))
            ctx.drawString(value, x + (FIELD_WIDTH - value.width()) / 2f, y + 6f)
            x += FIELD_WIDTH + 8f
        }
    }

    private fun updateDrag() {
        var deltaX = mouseX - dragStartX
        var deltaY = mouseY - dragStartY
        verticalGuides.clear()
        horizontalGuides.clear()

        val lead = selected.firstOrNull() ?: return
        val (leadX, leadY) = dragOrigins[lead] ?: return

        if (gridEnabled) {
            deltaX = ((leadX + deltaX) / gridSize).roundToInt() * gridSize - leadX
            deltaY = ((leadY + deltaY) / gridSize).roundToInt() * gridSize - leadY
        }
        else if (snapEnabled) {
            lead.x = leadX + deltaX
            lead.y = leadY + deltaY
            val moved = lead.bounds()
            val others = huds.filter { it !in selected }.map(HudElement::bounds)

            val xTargets = listOf(Resolution.width / 2f) + others.flatMap { listOf(it.left, it.centerX, it.right) }
            val yTargets = listOf(Resolution.height / 2f) + others.flatMap { listOf(it.top, it.centerY, it.bottom) }

            snap(listOf(moved.left, moved.centerX, moved.right), xTargets)?.let { (offset, guide) ->
                deltaX += offset
                verticalGuides.add(guide)
            }
            snap(listOf(moved.top, moved.centerY, moved.bottom), yTargets)?.let { (offset, guide) ->
                deltaY += offset
                horizontalGuides.add(guide)
            }
        }

        for ((hud, origin) in dragOrigins) {
            hud.x = origin.first + deltaX
            hud.y = origin.second + deltaY
        }
    }

    private fun snap(edges: List<Float>, targets: List<Float>): Pair<Float, Float>? {
        var best: Pair<Float, Float>? = null
        for (edge in edges) for (target in targets) {
            val offset = target - edge
            if (abs(offset) <= SNAP_DISTANCE && (best == null || abs(offset) < abs(best.first))) best = offset to target
        }
        return best
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = (mouseButtonEvent.x / Resolution.scale).toFloat()
        val my = (mouseButtonEvent.y / Resolution.scale).toFloat()

        if (super.mouseClicked(mouseButtonEvent, bl)) return true
        if (mouseButtonEvent.button() != InputConstants.MOUSE_BUTTON_LEFT) return false

        val hud = hudAt(mx, my)
        val additive = mouseButtonEvent.hasShiftDown()

        if (hud == null) {
            if (! additive) selected.clear()
            bandStart = mx to my
            return true
        }

        if (hud in selected && additive) {
            selected.remove(hud)
            return true
        }
        if (hud !in selected && ! additive) selected.clear()

        selected.remove(hud)
        val ordered = listOf(hud) + selected
        selected.clear()
        selected.addAll(ordered)

        dragging = true
        dragStartX = mx
        dragStartY = my
        dragOrigins.clear()
        for (element in selected) dragOrigins[element] = element.x to element.y
        return true
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        bandStart?.let { (startX, startY) ->
            val band = HudElement.Bounds(min(startX, mouseX), min(startY, mouseY), max(startX, mouseX), max(startY, mouseY))
            huds.filter { band.intersects(it.bounds()) }.forEach(selected::add)
        }
        bandStart = null

        if (dragging) {
            dragging = false
            dragOrigins.clear()
            verticalGuides.clear()
            horizontalGuides.clear()
            huds.forEach { it.isDragging = false }
        }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val mx = (mouseX / Resolution.scale).toFloat()
        val my = (mouseY / Resolution.scale).toFloat()

        val hovered = hudAt(mx, my)
        val targets = if (hovered == null || hovered in selected) selected.toList() else listOf(hovered)

        if (targets.isNotEmpty()) {
            targets.forEach { it.scale = (((it.scale + vertical * 0.1) * 10).roundToInt() / 10f).coerceIn(MIN_SCALE, MAX_SCALE) }
            return true
        }

        if (gridEnabled) {
            val step = (vertical * 5).toInt()
            if (step != 0) {
                gridSize = (gridSize + step).coerceIn(5, 50)
                updateGridButton()
                return true
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        val step = if (keyEvent.hasShiftDown()) 10f else 1f
        val (dx, dy) = when (keyEvent.key) {
            InputConstants.KEY_LEFT -> - step to 0f
            InputConstants.KEY_RIGHT -> step to 0f
            InputConstants.KEY_UP -> 0f to - step
            InputConstants.KEY_DOWN -> 0f to step
            else -> return super.keyPressed(keyEvent)
        }

        if (selected.isEmpty()) return super.keyPressed(keyEvent)
        selected.forEach {
            it.x += dx
            it.y += dy
        }
        return true
    }

    override fun onClose() {
        ConfigManager.save()
        super.onClose()
    }
}