package com.github.noamm9.ui.hud

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName

abstract class HudElement {
    open val name = this::class.simpleName ?: this::class.jvmName
    abstract val toggle: Boolean
    open val shouldDraw = true
    open val centered = false
    var width = 0f
    var height = 0f

    init {
        randomizePosition()
    }

    private fun randomizePosition() {
        if (mc.window == null) return
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        val estW = 60
        val estH = 20

        val padding = 10

        var safeX = 0f
        var safeY = 0f
        var foundSpot = false

        for (i in 0 .. 50) {
            val randX = Random.nextInt(padding, (screenW - estW - padding).coerceAtLeast(padding + 1)).toFloat()
            val randY = Random.nextInt(padding, (screenH - estH - padding).coerceAtLeast(padding + 1)).toFloat()

            var collides = false
            for (element in FeatureManager.hudElements) {
                val otherW = if (element.width > 0) element.width else estW.toFloat()
                val otherH = if (element.height > 0) element.height else estH.toFloat()

                if (randX < element.x + otherW &&
                    randX + estW > element.x &&
                    randY < element.y + otherH &&
                    randY + estH > element.y
                ) {
                    collides = true
                    break
                }
            }

            if (! collides) {
                safeX = randX
                safeY = randY
                foundSpot = true
                break
            }
        }

        if (foundSpot) {
            x = safeX
            y = safeY
        }
        else {
            x = (FeatureManager.hudElements.size * 10).toFloat() % (screenW - 50)
            y = (FeatureManager.hudElements.size * 10).toFloat() % (screenH - 50)
        }
    }

    open var x = 0f
    open var y = 0f

    var scale = 1f

    var isDragging = false
    private var dragX = 0f
    private var dragY = 0f

    fun renderElement(ctx: GuiGraphicsExtractor, example: Boolean) {
        if (! toggle) return

        ctx.pose().pushMatrix()
        ctx.pose().translate(x, y)
        ctx.pose().scale(scale, scale)

        draw(ctx, example).run {
            width = first
            height = second
        }

        ctx.pose().popMatrix()
    }

    abstract fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float>

    fun drawEditor(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
        if (! toggle) return

        if (isDragging) {
            x = mx - dragX
            y = my - dragY
        }

        drawBackground(ctx, mx, my)
        renderElement(ctx, true)
    }

    open fun drawBackground(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
        val scaledW = width * scale
        val scaledH = height * scale
        val centeredOffset = if (centered) scaledW / 2f else 0f
        val hovered = mx >= x - centeredOffset && mx <= x - centeredOffset + scaledW && my >= y && my <= y + scaledH

        val borderColor = if (isDragging || hovered) Style.accentColor else Color(255, 255, 255, 40)

        Render2D.drawRect(ctx, x - centeredOffset, y, scaledW, scaledH, Color(10, 10, 10, 150))
        Render2D.drawRect(ctx, x - centeredOffset, y, scaledW, 1f, borderColor)
        Render2D.drawRect(ctx, x - centeredOffset, y + scaledH - 1f, scaledW, 1f, borderColor)
    }

    open fun isHovered(mx: Int, my: Int): Boolean {
        val scaledW = width * scale
        val scaledH = height * scale
        val centeredOffset = (if (centered) scaledW / 2 else 0).toInt()
        return mx >= x - centeredOffset && mx <= x - centeredOffset + scaledW && my >= y && my <= y + scaledH
    }

    fun startDragging(mx: Int, my: Int) {
        if (isHovered(mx, my)) {
            isDragging = true
            dragX = mx - x
            dragY = my - y
        }
    }
}


operator fun <T: HudElement> T.provideDelegate(thisRef: Feature, prop: KProperty<*>): T {
    thisRef.hudElements.add(this)
    return this
}

operator fun <T: HudElement> T.getValue(thisRef: Feature, prop: KProperty<*>): T = this