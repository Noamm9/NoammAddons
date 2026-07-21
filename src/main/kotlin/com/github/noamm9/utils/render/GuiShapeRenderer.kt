package com.github.noamm9.utils.render

import com.github.noamm9.mixin.IGuiGraphicsExtractor
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object GuiShapeRenderer {
    fun drawAnnularSegment(ctx: GuiGraphicsExtractor, centerX: Float, centerY: Float, innerRadius: Float, outerRadius: Float, startAngle: Double, endAngle: Double, color: Color) {
        if (color.alpha == 0 || innerRadius >= outerRadius || startAngle >= endAngle) return
        val pose = Matrix3x2f(ctx.pose())
        val diameter = ceil(outerRadius * 2f).toInt() + 2
        val bounds = ScreenRectangle(
            floor(centerX - outerRadius).toInt() - 1,
            floor(centerY - outerRadius).toInt() - 1,
            diameter,
            diameter
        ).transformMaxBounds(pose)

        (ctx as IGuiGraphicsExtractor).guiRenderState.addGuiElement(
            AnnularSegment(pose, centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, color.rgb, bounds)
        )
    }

    private class AnnularSegment(
        private val pose: Matrix3x2fc,
        private val centerX: Float,
        private val centerY: Float,
        private val innerRadius: Float,
        private val outerRadius: Float,
        private val startAngle: Double,
        private val endAngle: Double,
        private val color: Int,
        private val bounds: ScreenRectangle
    ): GuiElementRenderState {
        override fun buildVertices(buffer: VertexConsumer) {
            val steps = ceil((endAngle - startAngle) * outerRadius / 2.0).toInt().coerceAtLeast(1)
            val step = (endAngle - startAngle) / steps

            repeat(steps) { index ->
                val angle1 = startAngle + index * step
                val angle2 = angle1 + step
                val cos1 = cos(angle1).toFloat()
                val sin1 = sin(angle1).toFloat()
                val cos2 = cos(angle2).toFloat()
                val sin2 = sin(angle2).toFloat()

                buffer.addVertexWith2DPose(pose, centerX + cos1 * outerRadius, centerY + sin1 * outerRadius).setColor(color)
                buffer.addVertexWith2DPose(pose, centerX + cos1 * innerRadius, centerY + sin1 * innerRadius).setColor(color)
                buffer.addVertexWith2DPose(pose, centerX + cos2 * innerRadius, centerY + sin2 * innerRadius).setColor(color)
                buffer.addVertexWith2DPose(pose, centerX + cos2 * outerRadius, centerY + sin2 * outerRadius).setColor(color)
            }
        }

        override fun pipeline(): RenderPipeline = RenderPipelines.GUI
        override fun textureSetup(): TextureSetup = TextureSetup.noTexture()
        override fun scissorArea(): ScreenRectangle? = null
        override fun bounds(): ScreenRectangle = bounds
    }
}
