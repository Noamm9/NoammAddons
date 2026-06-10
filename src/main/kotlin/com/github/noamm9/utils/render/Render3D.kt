package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object Render3D {
    fun renderBlock(
        ctx: RenderContext,
        pos: BlockPos,
        outlineColor: Color,
        fillColor: Color = outlineColor,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) {
        if (! outline && ! fill) return

        val state = mc.level?.getBlockState(pos) ?: return
        val camPos = ctx.camera.position
        val shape = if (state.block != Blocks.AIR) state.getShape(mc.level !!, pos) else Shapes.block()
        val adjustedLineWidth = lineWidth.toDouble()

        val outlineR = outlineColor.red / 255f
        val outlineG = outlineColor.green / 255f
        val outlineB = outlineColor.blue / 255f

        val fillR = fillColor.red / 255f
        val fillG = fillColor.green / 255f
        val fillB = fillColor.blue / 255f
        val fillA = fillColor.alpha / 255f

        val minX = pos.x + shape.min(Direction.Axis.X) - 0.002
        val minY = pos.y + shape.min(Direction.Axis.Y) - 0.002
        val minZ = pos.z + shape.min(Direction.Axis.Z) - 0.002
        val maxX = pos.x + shape.max(Direction.Axis.X) + 0.002
        val maxY = pos.y + shape.max(Direction.Axis.Y) + 0.002
        val maxZ = pos.z + shape.max(Direction.Axis.Z) + 0.002

        val x1 = minX - camPos.x
        val y1 = minY - camPos.y
        val z1 = minZ - camPos.z
        val x2 = maxX - camPos.x
        val y2 = maxY - camPos.y
        val z2 = maxZ - camPos.z

        if (fill) ShapeRenderer.addChainedFilledBoxVertices(
            ctx.matrixStack,
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED),
            x1, y1, z1, x2, y2, z2,
            fillR, fillG, fillB, fillA
        )

        if (outline) ShapeRenderer.renderLineBox(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.getLinesThroughWalls(adjustedLineWidth) else NoammRenderLayers.getLines(adjustedLineWidth)),
            x1, y1, z1, x2, y2, z2,
            outlineR, outlineG, outlineB, 1f
        )
    }

    fun renderBlock(
        ctx: RenderContext,
        pos: BlockPos,
        color: Color,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) = renderBlock(ctx, pos, color, color, outline, fill, phase, lineWidth)

    fun renderCircle(
        ctx: RenderContext,
        center: Vec3,
        radius: Number,
        color: Color,
        thickness: Number = 2,
        phase: Boolean = false
    ) {
        val cameraPos = ctx.camera.position
        val segments = (radius.toDouble() * 36).toInt().coerceAtLeast(32)

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z)

        val layer = if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED
        val buffer = ctx.consumers.getBuffer(layer)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val matrix = ctx.matrixStack.last().pose()

        val thicknessVal = thickness.toDouble() / 40.0
        val radiusVal = radius.toDouble()
        val innerR = (radiusVal - thicknessVal).coerceAtLeast(0.0)
        val outerR = radiusVal + thicknessVal

        for (i in 0 .. segments) {
            val angle = i * (2.0 * Math.PI / segments)
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()

            buffer.addVertex(matrix, (innerR * c).toFloat(), 0f, (innerR * s).toFloat()).setColor(r, g, b, a)
            buffer.addVertex(matrix, (outerR * c).toFloat(), 0f, (outerR * s).toFloat()).setColor(r, g, b, a)
        }

        ctx.matrixStack.popPose()
    }

    fun renderBillboardedCircle(
        ctx: RenderContext,
        center: Vec3,
        radius: Number,
        color: Color,
        thickness: Number = 2,
        phase: Boolean = false
    ) {
        val camera = ctx.camera
        val cameraPos = camera.position
        val segments = (radius.toDouble() * 100).toInt().coerceAtLeast(64)

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z)
        ctx.matrixStack.mulPose(camera.rotation())

        val layer = if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED
        val buffer = ctx.consumers.getBuffer(layer)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val matrix = ctx.matrixStack.last().pose()

        val thicknessVal = thickness.toDouble() / 40.0
        val radiusVal = radius.toDouble()
        val innerR = (radiusVal - thicknessVal).coerceAtLeast(0.0)
        val outerR = radiusVal + thicknessVal

        for (i in 0 .. segments) {
            val angle = i * (2.0 * Math.PI / segments)
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()

            buffer.addVertex(matrix, (innerR * c).toFloat(), (innerR * s).toFloat(), 0f).setColor(r, g, b, a)
            buffer.addVertex(matrix, (outerR * c).toFloat(), (outerR * s).toFloat(), 0f).setColor(r, g, b, a)
        }

        ctx.matrixStack.popPose()
    }

    fun renderBox(
        ctx: RenderContext,
        x: Number,
        y: Number,
        z: Number,
        width: Number,
        height: Number,
        outlineColor: Color,
        fillColor: Color = outlineColor,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) {
        if (! outline && ! fill) return
        val cam = ctx.camera.position.reverse()

        val xd = x.toDouble()
        val yd = y.toDouble()
        val zd = z.toDouble()
        val hw = width.toDouble() / 2.0
        val hd = height.toDouble()

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(cam.x, cam.y, cam.z)

        if (fill) ShapeRenderer.addChainedFilledBoxVertices(
            ctx.matrixStack,
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED),
            xd - hw, yd, zd - hw,
            xd + hw, yd + hd, zd + hw,
            fillColor.red / 255f, fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f
        )

        if (outline) ShapeRenderer.renderLineBox(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.getLinesThroughWalls(lineWidth.toDouble()) else NoammRenderLayers.getLines(lineWidth.toDouble())),
            xd - hw, yd, zd - hw,
            xd + hw, yd + hd, zd + hw,
            outlineColor.red / 255f, outlineColor.green / 255f, outlineColor.blue / 255f, 1f
        )

        ctx.matrixStack.popPose()
    }

    fun renderBox(
        ctx: RenderContext,
        x: Number,
        y: Number,
        z: Number,
        width: Number,
        height: Number,
        color: Color = Color.CYAN,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) = renderBox(ctx, x, y, z, width, height, color, color, outline, fill, phase, lineWidth)

    fun renderString(
        text: String,
        x: Number, y: Number, z: Number,
        color: Color = Color.WHITE,
        scale: Number = 1f,
        phase: Boolean = false
    ) {
        val toScale = (scale.toFloat() * 0.025f)
        val matrices = Matrix4f()
        val textRenderer = mc.font
        val camera = mc.gameRenderer.mainCamera
        val dx = (x.toDouble() - camera.position.x).toFloat()
        val dy = (y.toDouble() - camera.position.y).toFloat()
        val dz = (z.toDouble() - camera.position.z).toFloat()

        matrices.translate(dx, dy, dz).rotate(camera.rotation()).scale(toScale, - toScale, toScale)

        val consumer = mc.renderBuffers().bufferSource()
        val textLayer = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
        val lines = text.addColor().split("\n")

        for ((i, line) in lines.withIndex()) {
            textRenderer.drawInBatch(
                line,
                - textRenderer.width(line) / 2f,
                i * 9f,
                color.rgb,
                true,
                matrices,
                consumer,
                textLayer,
                0,
                LightTexture.FULL_BLOCK
            )
        }
    }

    fun renderString(
        text: String,
        pos: Vec3,
        color: Color = Color.WHITE,
        scale: Number = 1f,
        phase: Boolean = false
    ) = renderString(text, pos.x, pos.y, pos.z, color, scale, phase)


    fun renderLine(ctx: RenderContext, start: Vec3, finish: Vec3, color: Color, thickness: Number = 2, phase: Boolean = false) {
        val cameraPos = ctx.camera.position
        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(- cameraPos.x, - cameraPos.y, - cameraPos.z)

        val lines = if (phase) NoammRenderLayers.getLinesThroughWalls(thickness.toDouble()) else NoammRenderLayers.getLines(thickness.toDouble())
        val buffer = (ctx.consumers as MultiBufferSource.BufferSource).getBuffer(lines)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val direction = finish.subtract(start).normalize().toVector3f()
        val matrix = ctx.matrixStack.last()

        buffer.addVertex(matrix, start.x.toFloat(), start.y.toFloat(), start.z.toFloat()).setColor(r, g, b, a).setNormal(matrix, direction)
        buffer.addVertex(matrix, finish.x.toFloat(), finish.y.toFloat(), finish.z.toFloat()).setColor(r, g, b, a).setNormal(matrix, direction)

        ctx.consumers.endBatch(lines)
        ctx.matrixStack.popPose()
    }

    fun renderTracer(ctx: RenderContext, point: Vec3, color: Color, thickness: Number = 2.5) {
        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(- ctx.camera.position.x, - ctx.camera.position.y, - ctx.camera.position.z)

        val buffer = (ctx.consumers as MultiBufferSource.BufferSource).getBuffer(NoammRenderLayers.getLinesThroughWalls(thickness.toDouble()))
        val cameraPoint = ctx.camera.position.add(Vec3.directionFromRotation(ctx.camera.xRot, ctx.camera.yRot))
        val normal = point.toVector3f().sub(cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).normalize()
        val entry = ctx.matrixStack.last()

        RenderSystem.lineWidth(thickness.toFloat())

        buffer.addVertex(entry, cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).setColor(color.red / 255f, color.green / 255f, color.blue / 255f, 1f).setNormal(entry, normal)
        buffer.addVertex(entry, point.x.toFloat(), point.y.toFloat(), point.z.toFloat()).setColor(color.red / 255f, color.green / 255f, color.blue / 255f, 1f).setNormal(entry, normal)

        ctx.consumers.endBatch(RenderType.lines())
        ctx.matrixStack.popPose()
    }
}