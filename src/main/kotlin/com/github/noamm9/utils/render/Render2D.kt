package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.minus
import com.github.noamm9.utils.NumbersUtils.plus
import com.github.noamm9.utils.NumbersUtils.times
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.Slot
import org.joml.Matrix3x2f
import java.awt.Color
import kotlin.math.*


object Render2D: ISelfInit {
    private val slotHighlights = mutableMapOf<Long, Int>()

    override fun init() {
        register<RenderOverlayEvent> { slotHighlights.clear() }
    }

    fun Slot.highlight(ctx: GuiGraphicsExtractor, color: Color, priority: Int = 0) {
        if (checkSlot(x, y, priority)) ctx.fill(x, y, x + 16, y + 16, color.rgb)
    }

    fun checkSlot(x: Int, y: Int, priority: Int): Boolean {
        val key = (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL) // hash
        val claimed = slotHighlights[key]
        if (claimed != null && claimed >= priority) return false
        slotHighlights[key] = priority
        return true
    }

    fun GuiGraphicsExtractor.drawTexture(texture: Identifier, x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE) {
        blit(RenderPipelines.GUI_TEXTURED, texture, x.toInt(), y.toInt(), 0f, 0f, width.toInt(), height.toInt(), width.toInt(), height.toInt(), color.rgb)
    }

    fun GuiGraphicsExtractor.scissor(x: Number, y: Number, width: Number, height: Number) {
        enableScissor(x.toInt(), y.toInt(), x.toInt() + width.toInt(), y.toInt() + height.toInt())
    }

    fun GuiGraphicsExtractor.drawRect(x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        pose().scale(fw, fh)
        fill(0, 0, 1, 1, color.rgb)
        pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawBorder(x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE, thickness: Number = 1) {
        drawRect(x, y, width, thickness, color)
        drawRect(x, y + height - thickness, width, thickness, color)
        drawRect(x, y + thickness, thickness, height - thickness * 2, color)
        drawRect(x + width - thickness, y + thickness, thickness, height - thickness * 2, color)
    }

    fun GuiGraphicsExtractor.drawLine(x1: Number, y1: Number, x2: Number, y2: Number, color: Color, thickness: Number = 1) {
        val fx1 = x1.toFloat()
        val fy1 = y1.toFloat()
        val fx2 = x2.toFloat()
        val fy2 = y2.toFloat()
        val ft = thickness.toFloat()

        val dx = fx2 - fx1
        val dy = fy2 - fy1
        val distance = sqrt(dx * dx + dy * dy)
        val angle = atan2(dy, dx)

        pose().pushMatrix()
        pose().translate(fx1, fy1)
        pose().rotate(angle)
        pose().translate(0f, - ft / 2f)
        pose().scale(distance, ft)

        fill(0, 0, 1, 1, color.rgb)

        pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawString(str: String, x: Number, y: Number, color: Color = Color.WHITE, scale: Number = 1, shadow: Boolean = true) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fScale = scale.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        if (fScale != 1f) pose().scale(fScale, fScale)
        text(mc.font, str.addColor(), 0, 0, color.rgb, shadow)
        pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawCenteredString(str: String, x: Number, y: Number, color: Color = Color.WHITE, scale: Number = 1, shadow: Boolean = true) {
        val fScale = scale.toFloat()
        val totalScaledWidth = str.width() * fScale
        val centerX = x.toFloat() - (totalScaledWidth / 2f)
        drawString(str, centerX, y, color, scale, shadow)
    }

    fun GuiGraphicsExtractor.drawFloatingRect(x: Number, y: Number, width: Number, height: Number, color: Color) {
        val base = color.rgb
        val light = color.brighter().rgb
        val dark = color.darker().rgb
        val ix = x.toInt()
        val iy = y.toInt()
        val iw = width.toInt()
        val ih = height.toInt()

        fill(ix, iy, ix + 1, iy + ih, light)
        fill(ix + 1, iy, ix + iw, iy + 1, light)
        fill(ix + iw - 1, iy + 1, ix + iw, iy + ih, dark)
        fill(ix + 1, iy + ih - 1, ix + iw - 1, iy + ih, dark)
        fill(ix + 1, iy + 1, ix + iw - 1, iy + ih - 1, base)
    }

    fun GuiGraphicsExtractor.drawPlayerHead(x: Int, y: Int, size: Int, skin: Identifier) {
        blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, size, size, 8, 8, 64, 64, - 1)
        blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, size, size, 8, 8, 64, 64, - 1)
    }

    /**
     * Draws a gradient from Color1 (Left) to Color2 (Right)
     */
    fun GuiGraphicsExtractor.drawHorizontalGradient(x: Number, y: Number, width: Number, height: Number, color1: Color, color2: Color) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()
        val angle = (- Math.PI / 2).toFloat() // -90 degrees

        pose().pushMatrix()
        pose().translate(fx, fy + fh)
        pose().rotate(angle)
        pose().scale(fh, fw)
        fillGradient(0, 0, 1, 1, color1.rgb, color2.rgb)
        pose().popMatrix()
    }

    /**
     * Draws a gradient from Color1 (Top) to Color2 (Bottom)
     */
    fun GuiGraphicsExtractor.drawVerticalGradient(x: Number, y: Number, width: Number, height: Number, color1: Color, color2: Color) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        pose().scale(fw, fh)
        fillGradient(0, 0, 1, 1, color1.rgb, color2.rgb)
        pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawAnnularSegment(centerX: Number, centerY: Number, innerRadius: Number, outerRadius: Number, startAngle: Number, endAngle: Number, color: Color) {
        val fcenterX = centerX.toFloat()
        val fcenterY = centerY.toFloat()
        val finnerRadius = innerRadius.toFloat()
        val fouterRadius = outerRadius.toFloat()
        val dstartAngle = startAngle.toDouble()
        val dendAngle = endAngle.toDouble()
        val diameter = ceil(fouterRadius * 2f).toInt() + 2
        val pose = Matrix3x2f(pose())

        guiRenderState.addGuiElement(object: GuiElementRenderState {
            override fun buildVertices(buffer: VertexConsumer) {
                val steps = ceil((dendAngle - dstartAngle) * fouterRadius / 2.0).toInt().coerceAtLeast(1)
                val step = (dendAngle - dstartAngle) / steps

                repeat(steps) { index ->
                    val angle1 = startAngle + index * step
                    val angle2 = angle1 + step
                    val cos1 = cos(angle1).toFloat()
                    val sin1 = sin(angle1).toFloat()
                    val cos2 = cos(angle2).toFloat()
                    val sin2 = sin(angle2).toFloat()

                    buffer.addVertexWith2DPose(pose, fcenterX + cos1 * fouterRadius, fcenterY + sin1 * fouterRadius).setColor(color.rgb)
                    buffer.addVertexWith2DPose(pose, fcenterX + cos1 * finnerRadius, fcenterY + sin1 * finnerRadius).setColor(color.rgb)
                    buffer.addVertexWith2DPose(pose, fcenterX + cos2 * finnerRadius, fcenterY + sin2 * finnerRadius).setColor(color.rgb)
                    buffer.addVertexWith2DPose(pose, fcenterX + cos2 * fouterRadius, fcenterY + sin2 * fouterRadius).setColor(color.rgb)
                }
            }

            override fun pipeline() = RenderPipelines.GUI
            override fun textureSetup() = TextureSetup.noTexture()
            override fun scissorArea() = null
            override fun bounds() = ScreenRectangle(
                floor(centerX - outerRadius).toInt() - 1,
                floor(centerY - outerRadius).toInt() - 1,
                diameter, diameter
            ).transformMaxBounds(pose)
        })
    }
}