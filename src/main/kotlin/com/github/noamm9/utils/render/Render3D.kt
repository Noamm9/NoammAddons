package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.minus
import com.github.noamm9.utils.NumbersUtils.plus
import com.github.noamm9.utils.NumbersUtils.times
import com.github.noamm9.utils.render.RenderHelper.positionVec
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Matrix4f
import org.joml.Vector3f
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
        val camPos = ctx.camera.positionVec
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

        if (fill) addFilledBoxVertices(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED),
            x1, y1, z1, x2, y2, z2,
            fillR, fillG, fillB, fillA
        )

        if (outline) renderLineBox(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES),
            x1, y1, z1, x2, y2, z2,
            outlineR, outlineG, outlineB, 1f,
            adjustedLineWidth.toFloat()
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
        val cameraPos = ctx.camera.positionVec
        val segments = (36 * radius).toInt()

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(- cameraPos.x, - cameraPos.y, - cameraPos.z)

        val buffer = ctx.consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val pose = ctx.matrixStack.last()

        val size = thickness.toDouble() / 40.0
        val innerR = radius - size
        val outerR = radius + size
        val bottomY = (center.y - size).toFloat()
        val topY = (center.y + size).toFloat()

        for (i in 0 until segments) {
            val angle1 = i * (2.0 * Math.PI / segments)
            val angle2 = (i + 1) * (2.0 * Math.PI / segments)

            val c1 = cos(angle1).toFloat()
            val s1 = sin(angle1).toFloat()
            val c2 = cos(angle2).toFloat()
            val s2 = sin(angle2).toFloat()

            val x1Inner = (center.x + innerR * c1).toFloat()
            val z1Inner = (center.z + innerR * s1).toFloat()
            val x1Outer = (center.x + outerR * c1).toFloat()
            val z1Outer = (center.z + outerR * s1).toFloat()

            val x2Inner = (center.x + innerR * c2).toFloat()
            val z2Inner = (center.z + innerR * s2).toFloat()
            val x2Outer = (center.x + outerR * c2).toFloat()
            val z2Outer = (center.z + outerR * s2).toFloat()

            addQuad(
                buffer, pose,
                x1Inner, topY, z1Inner,
                x1Outer, topY, z1Outer,
                x2Outer, topY, z2Outer,
                x2Inner, topY, z2Inner,
                r, g, b, a
            )

            addQuad(
                buffer, pose,
                x1Outer, bottomY, z1Outer,
                x1Outer, topY, z1Outer,
                x2Outer, topY, z2Outer,
                x2Outer, bottomY, z2Outer,
                r, g, b, a
            )

            addQuad(
                buffer, pose,
                x1Inner, bottomY, z1Inner,
                x1Inner, topY, z1Inner,
                x2Inner, topY, z2Inner,
                x2Inner, bottomY, z2Inner,
                r, g, b, a
            )

            addQuad(
                buffer, pose,
                x1Inner, bottomY, z1Inner,
                x1Outer, bottomY, z1Outer,
                x2Outer, bottomY, z2Outer,
                x2Inner, bottomY, z2Inner,
                r, g, b, a
            )
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
        val cam = ctx.camera.positionVec.reverse()

        val xd = x.toDouble()
        val yd = y.toDouble()
        val zd = z.toDouble()
        val hw = width.toDouble() / 2.0
        val hd = height.toDouble()

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(cam.x, cam.y, cam.z)

        if (fill) addFilledBoxVertices(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED),
            xd - hw, yd, zd - hw,
            xd + hw, yd + hd, zd + hw,
            fillColor.red / 255f, fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f
        )


        if (outline) renderLineBox(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES),
            xd - hw, yd, zd - hw,
            xd + hw, yd + hd, zd + hw,
            outlineColor.red / 255f, outlineColor.green / 255f, outlineColor.blue / 255f, 1f,
            lineWidth.toFloat()
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

    fun renderBoxBounds(
        ctx: RenderContext,
        minX: Double,
        minY: Double,
        minZ: Double,
        maxX: Double,
        maxY: Double,
        maxZ: Double,
        outlineColor: Color,
        fillColor: Color = outlineColor,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) {
        if (! outline && ! fill) return
        val cam = ctx.camera.positionVec

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(- cam.x, - cam.y, - cam.z)

        if (fill) addFilledBoxVertices(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED),
            minX, minY, minZ, maxX, maxY, maxZ,
            fillColor.red / 255f, fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f
        )

        if (outline) renderLineBox(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES),
            minX, minY, minZ, maxX, maxY, maxZ,
            outlineColor.red / 255f, outlineColor.green / 255f, outlineColor.blue / 255f, 1f,
            lineWidth.toFloat()
        )

        ctx.matrixStack.popPose()
    }

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
        val camPos = camera.positionVec
        val dx = (x.toDouble() - camPos.x).toFloat()
        val dy = (y.toDouble() - camPos.y).toFloat()
        val dz = (z.toDouble() - camPos.z).toFloat()

        matrices.translate(dx, dy, dz).rotate(camera.rotation()).scale(toScale, - toScale, toScale)

        val consumer = mc.renderBuffers().bufferSource()
        val textLayer = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
        val lines = text.addColor().split("\n")

        for ((i, line) in lines.withIndex()) textRenderer.drawInBatch(
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

        consumer.endBatch()
    }

    fun renderString(
        text: String,
        pos: Vec3,
        color: Color = Color.WHITE,
        scale: Number = 1f,
        phase: Boolean = false
    ) = renderString(text, pos.x, pos.y, pos.z, color, scale, phase)


    fun renderLine(ctx: RenderContext, start: Vec3, finish: Vec3, color: Color, thickness: Number = 2, phase: Boolean = false) {
        val cameraPos = ctx.camera.positionVec
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

        buffer.addVertex(matrix, start.x.toFloat(), start.y.toFloat(), start.z.toFloat()).setColor(r, g, b, a).setNormal(matrix, direction).setLineWidth(thickness.toFloat())
        buffer.addVertex(matrix, finish.x.toFloat(), finish.y.toFloat(), finish.z.toFloat()).setColor(r, g, b, a).setNormal(matrix, direction).setLineWidth(thickness.toFloat())

        ctx.consumers.endBatch(lines)
        ctx.matrixStack.popPose()
    }

    fun renderLine(ctx: RenderContext, start: BlockPos, end: BlockPos, thickness: Number, color: Color) {
        renderLine(ctx, Vec3.atCenterOf(start), Vec3.atCenterOf(end), color, thickness)
    }

    fun renderTracer(ctx: RenderContext, point: Vec3, color: Color, thickness: Number = 2.5) {
        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(- ctx.camera.positionVec.x, - ctx.camera.positionVec.y, - ctx.camera.positionVec.z)

        val buffer = (ctx.consumers as MultiBufferSource.BufferSource).getBuffer(NoammRenderLayers.getLinesThroughWalls(thickness.toDouble()))
        val cameraPoint = ctx.camera.position.add(Vec3.directionFromRotation(ctx.camera.xRot, ctx.camera.yRot))
        val renderType = NoammRenderLayers.LINES_THROUGH_WALLS
        val buffer = (ctx.consumers as MultiBufferSource.BufferSource).getBuffer(renderType)
        val cameraPoint = ctx.camera.positionVec.add(Vec3.directionFromRotation(ctx.camera.xRot(), ctx.camera.yRot()))
        val normal = point.toVector3f().sub(cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).normalize()
        val entry = ctx.matrixStack.last()

        buffer.addVertex(entry, cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).setColor(color.red / 255f, color.green / 255f, color.blue / 255f, 1f).setNormal(entry, normal).setLineWidth(thickness.toFloat())
        buffer.addVertex(entry, point.x.toFloat(), point.y.toFloat(), point.z.toFloat()).setColor(color.red / 255f, color.green / 255f, color.blue / 255f, 1f).setNormal(entry, normal).setLineWidth(thickness.toFloat())

        ctx.consumers.endBatch(renderType)
        ctx.matrixStack.popPose()
    }

    fun renderTracer(ctx: RenderContext, point: BlockPos, color: Color, thickness: Number) {
        renderTracer(ctx, Vec3.atCenterOf(point), color, thickness)
    }

    private fun addFilledBoxVertices(pose: PoseStack.Pose, buffer: VertexConsumer, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float) {
        val minX = x1.toFloat()
        val minY = y1.toFloat()
        val minZ = z1.toFloat()
        val maxX = x2.toFloat()
        val maxY = y2.toFloat()
        val maxZ = z2.toFloat()

        addQuad(buffer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a)
        addQuad(buffer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a)
        addQuad(buffer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a)
        addQuad(buffer, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a)
        addQuad(buffer, pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a)
        addQuad(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a)
    }

    private fun addVertex(buffer: VertexConsumer, pose: PoseStack.Pose, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(pose, x, y, z).setColor(r, g, b, a)
    }

    private fun addQuad(buffer: VertexConsumer, pose: PoseStack.Pose, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        addVertex(buffer, pose, x1, y1, z1, r, g, b, a)
        addVertex(buffer, pose, x2, y2, z2, r, g, b, a)
        addVertex(buffer, pose, x3, y3, z3, r, g, b, a)
        addVertex(buffer, pose, x1, y1, z1, r, g, b, a)
        addVertex(buffer, pose, x3, y3, z3, r, g, b, a)
        addVertex(buffer, pose, x4, y4, z4, r, g, b, a)
    }

    private fun renderLineBox(pose: PoseStack.Pose, buffer: VertexConsumer, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
        val minX = x1.toFloat()
        val minY = y1.toFloat()
        val minZ = z1.toFloat()
        val maxX = x2.toFloat()
        val maxY = y2.toFloat()
        val maxZ = z2.toFloat()

        addLine(buffer, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, lineWidth)

        addLine(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, lineWidth)

        addLine(buffer, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, lineWidth)
        addLine(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth)
    }

    private fun addLine(buffer: VertexConsumer, pose: PoseStack.Pose, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
        val normal = Vector3f(x2 - x1, y2 - y1, z2 - z1)
        if (normal.lengthSquared() > 0f) normal.normalize()
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, normal).setLineWidth(lineWidth)
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, normal).setLineWidth(lineWidth)
    }
}