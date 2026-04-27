package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.minus
import com.github.noamm9.utils.NumbersUtils.plus
import com.github.noamm9.utils.NumbersUtils.times
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.sqrt


object Render2D {
    fun Slot.highlight(ctx: GuiGraphics, color: Color) = drawRect(ctx, x, y, 16, 16, color)

    fun drawTexture(ctx: GuiGraphics, texture: Identifier, x: Number, y: Number, width: Number, height: Number) {
        ctx.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }

    fun drawRect(ctx: GuiGraphics, x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE) {
        val pose = ctx.pose()
        pose.translate(x.toFloat(), y.toFloat())
        ctx.fill(0, 0, width.toInt(), height.toInt(), color.rgb)
        pose.translate(- x.toFloat(), - y.toFloat())
    }

    fun drawBorder(ctx: GuiGraphics, x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE, thickness: Number = 1) {
        drawRect(ctx, x, y, width, thickness, color)
        drawRect(ctx, x, y + height - thickness, width, thickness, color)
        drawRect(ctx, x, y + thickness, thickness, height - (thickness * 2), color)
        drawRect(ctx, x + width - thickness, y + thickness, thickness, height - (thickness * 2), color)
    }

    fun drawLine(ctx: GuiGraphics, x1: Number, y1: Number, x2: Number, y2: Number, color: Color, thickness: Number = 1) {
        val pose = ctx.pose()
        val fx1 = x1.toFloat()
        val fy1 = y1.toFloat()
        val fx2 = x2.toFloat()
        val fy2 = y2.toFloat()
        val iThick = thickness.toInt()

        val dx = fx2 - fx1
        val dy = fy2 - fy1
        val distance = sqrt(dx * dx + dy * dy).toInt()
        val angle = atan2(dy, dx)

        pose.translate(fx1, fy1)
        pose.rotate(angle)

        ctx.fill(0, 0, distance, iThick, color.rgb)

        pose.rotate(- angle)
        pose.translate(- fx1, - fy1)
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphics, str: String, x: Number, y: Number, color: Color = Color.WHITE, scale: Number = 1, shadow: Boolean = true) {
        val pose = ctx.pose()
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fScale = scale.toFloat()

        pose.translate(fx, fy)
        if (fScale != 1f) pose.scale(fScale, fScale)
        ctx.drawString(mc.font, str.addColor(), 0, 0, color.rgb, shadow)
        if (fScale != 1f) pose.scale(1f / fScale, 1f / fScale)
        pose.translate(- fx, - fy)
    }

    fun drawCenteredString(ctx: GuiGraphics, str: String, x: Number, y: Number, color: Color = Color.WHITE, scale: Number = 1, shadow: Boolean = true) {
        val fScale = scale.toFloat()
        val totalScaledWidth = with(Render2D) { str.width() } * fScale
        val centerX = x.toFloat() - (totalScaledWidth / 2f)
        drawString(ctx, str, centerX, y, color, scale, shadow)
    }

    fun renderItem(context: GuiGraphics, item: ItemStack, x: Number, y: Number, scale: Number = 1) {
        context.pose().pushMatrix()
        context.pose().translate(x.toFloat(), y.toFloat())
        context.pose().scale(scale.toFloat())
        context.renderItem(item, 0, 0)
        context.pose().popMatrix()
    }

    fun renderItem(context: GuiGraphics, itemPath: String, x: Number, y: Number, size: Number) {
        val location = Identifier.parse(itemPath)
        val atlas = mc.textureManager.getTexture(Identifier.withDefaultNamespace("textures/atlas/blocks.png")) as TextureAtlas
        val sprite = atlas.getSprite(location)
        context.pose().pushMatrix()
        context.pose().translate(x.toFloat(), y.toFloat())
        context.blit(sprite.atlasLocation(), 0, 0, size.toInt(), size.toInt(), sprite.u0, sprite.u1, sprite.v0, sprite.v1)
        context.pose().popMatrix()
    }

    fun drawFloatingRect(graphics: GuiGraphics, x: Number, y: Number, width: Number, height: Number, color: Color) {
        val base = color.rgb
        val light = color.brighter().rgb
        val dark = color.darker().rgb
        val ix = x.toInt()
        val iy = y.toInt()
        val iw = width.toInt()
        val ih = height.toInt()

        graphics.fill(ix, iy, ix + 1, iy + ih, light)
        graphics.fill(ix + 1, iy, ix + iw, iy + 1, light)
        graphics.fill(ix + iw - 1, iy + 1, ix + iw, iy + ih, dark)
        graphics.fill(ix + 1, iy + ih - 1, ix + iw - 1, iy + ih, dark)
        graphics.fill(ix + 1, iy + 1, ix + iw - 1, iy + ih - 1, base)
    }

    fun drawPlayerHead(context: GuiGraphics, x: Int, y: Int, size: Int, skin: Identifier) {
        context.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, size, size, 8, 8, 64, 64, - 1)
        context.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, size, size, 8, 8, 64, 64, - 1)
    }

    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { mc.font.width(it.addColor()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return mc.font.lineHeight * lineCount
    }

    /**
     * Draws a gradient from Color1 (Left) to Color2 (Right)
     */
    fun drawHorizontalGradient(ctx: GuiGraphics, x: Number, y: Number, width: Number, height: Number, color1: Color, color2: Color) {
        val pose = ctx.pose()
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()
        val angle = (- Math.PI / 2).toFloat() // -90 degrees

        pose.translate(fx, fy + fh)
        pose.rotate(angle)

        ctx.fillGradient(0, 0, fh.toInt(), fw.toInt(), color1.rgb, color2.rgb)

        pose.rotate(- angle)
        pose.translate(- fx, - (fy + fh))
    }

    /**
     * Draws a gradient from Color1 (Top) to Color2 (Bottom)
     */
    fun drawVerticalGradient(ctx: GuiGraphics, x: Number, y: Number, width: Number, height: Number, color1: Color, color2: Color) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val iw = width.toInt()
        val ih = height.toInt()

        ctx.pose().translate(fx, fy)
        ctx.fillGradient(0, 0, iw, ih, color1.rgb, color2.rgb)
        ctx.pose().translate(- fx, - fy)
    }
}