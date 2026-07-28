package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.times
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Vector3f
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object Render3D {
    fun RenderContext.renderBlock(
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
        val shape = if (state.block != Blocks.AIR) state.getShape(mc.level !!, pos) else Shapes.block()

        val outlineR = outlineColor.red / 255f
        val outlineG = outlineColor.green / 255f
        val outlineB = outlineColor.blue / 255f

        val fillR = fillColor.red / 255f
        val fillG = fillColor.green / 255f
        val fillB = fillColor.blue / 255f
        val fillA = fillColor.alpha / 255f

        val minX = pos.x + shape.min(Direction.Axis.X)
        val minY = pos.y + shape.min(Direction.Axis.Y)
        val minZ = pos.z + shape.min(Direction.Axis.Z)
        val maxX = pos.x + shape.max(Direction.Axis.X)
        val maxY = pos.y + shape.max(Direction.Axis.Y)
        val maxZ = pos.z + shape.max(Direction.Axis.Z)

        matrixStack.pushPose()
        matrixStack.translate(camera.position().reverse())

        if (fill) consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED).addFilledBoxVertices(
            matrixStack.last(),
            minX,
            minY, minZ, maxX, maxY, maxZ, fillR,
            fillG, fillB, fillA
        )

        if (outline) consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES).addLineBoxVertices(
            matrixStack.last(),
            minX,
            minY, minZ, maxX, maxY, maxZ, outlineR,
            outlineG, outlineB, 1f, lineWidth.toFloat()
        )

        matrixStack.popPose()
    }

    fun RenderContext.renderBlock(
        pos: BlockPos,
        color: Color,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) = renderBlock(pos, color, color, outline, fill, phase, lineWidth)

    fun RenderContext.renderCircle(
        center: Vec3,
        radius: Number,
        color: Color,
        thickness: Number = 2,
        phase: Boolean = false
    ) {
        matrixStack.pushPose()
        matrixStack.translate(camera.position().reverse())
        val buffer = consumers.getBuffer(if (phase) NoammRenderLayers.CIRCLE_FILLED_THROUGH_WALLS else NoammRenderLayers.CIRCLE_FILLED)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val pose = matrixStack.last()
        val segments = (36 * radius).toInt()
        val size = thickness.toDouble() / 40.0
        val innerR = radius.toDouble() - size
        val outerR = radius.toDouble() + size
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

            buffer.addVertex(pose, x1Inner, topY, z1Inner).setColor(r, g, b, a)
            buffer.addVertex(pose, x1Outer, topY, z1Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Outer, topY, z2Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Inner, topY, z2Inner).setColor(r, g, b, a)

            buffer.addVertex(pose, x1Outer, bottomY, z1Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x1Outer, topY, z1Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Outer, topY, z2Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Outer, bottomY, z2Outer).setColor(r, g, b, a)

            buffer.addVertex(pose, x1Inner, bottomY, z1Inner).setColor(r, g, b, a)
            buffer.addVertex(pose, x1Inner, topY, z1Inner).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Inner, topY, z2Inner).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Inner, bottomY, z2Inner).setColor(r, g, b, a)

            buffer.addVertex(pose, x1Inner, bottomY, z1Inner).setColor(r, g, b, a)
            buffer.addVertex(pose, x1Outer, bottomY, z1Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Outer, bottomY, z2Outer).setColor(r, g, b, a)
            buffer.addVertex(pose, x2Inner, bottomY, z2Inner).setColor(r, g, b, a)
        }

        matrixStack.popPose()
    }

    fun RenderContext.renderBillboardedCircle(
        center: Vec3,
        radius: Number,
        color: Color,
        thickness: Number = 2,
        phase: Boolean = false
    ) {
        val cameraPos = camera.position()
        val segments = (radius.toDouble() * 100).toInt().coerceAtLeast(64)

        matrixStack.pushPose()
        matrixStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z)
        matrixStack.mulPose(camera.rotation())

        val layer = if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED
        val buffer = consumers.getBuffer(layer)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val matrix = matrixStack.last().pose()

        val thicknessVal = thickness.toDouble() / 40.0
        val radiusVal = radius.toDouble()
        val innerR = (radiusVal - thicknessVal).coerceAtLeast(0.0)
        val outerR = radiusVal + thicknessVal

        val step = 2.0 * Math.PI / segments
        for (i in 0 until segments) {
            val c1 = cos(i * step).toFloat()
            val s1 = sin(i * step).toFloat()
            val c2 = cos((i + 1) * step).toFloat()
            val s2 = sin((i + 1) * step).toFloat()

            val i1x = (innerR * c1).toFloat()
            val i1y = (innerR * s1).toFloat()
            val o1x = (outerR * c1).toFloat()
            val o1y = (outerR * s1).toFloat()
            val i2x = (innerR * c2).toFloat()
            val i2y = (innerR * s2).toFloat()
            val o2x = (outerR * c2).toFloat()
            val o2y = (outerR * s2).toFloat()

            buffer.addVertex(matrix, i1x, i1y, 0f).setColor(r, g, b, a)
            buffer.addVertex(matrix, o1x, o1y, 0f).setColor(r, g, b, a)
            buffer.addVertex(matrix, o2x, o2y, 0f).setColor(r, g, b, a)

            buffer.addVertex(matrix, i1x, i1y, 0f).setColor(r, g, b, a)
            buffer.addVertex(matrix, o2x, o2y, 0f).setColor(r, g, b, a)
            buffer.addVertex(matrix, i2x, i2y, 0f).setColor(r, g, b, a)
        }

        matrixStack.popPose()
    }

    fun RenderContext.renderBox(
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
        val cam = camera.position().reverse()

        val xd = x.toDouble()
        val yd = y.toDouble()
        val zd = z.toDouble()
        val hw = width.toDouble() / 2.0
        val hd = height.toDouble()

        matrixStack.pushPose()
        matrixStack.translate(cam.x, cam.y, cam.z)

        if (fill) consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED).addFilledBoxVertices(
            matrixStack.last(),
            xd - hw,
            yd, zd - hw, xd + hw,
            yd + hd, zd + hw, fillColor.red / 255f,
            fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f
        )

        if (outline) consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES).addLineBoxVertices(
            matrixStack.last(),
            xd - hw,
            yd, zd - hw, xd + hw,
            yd + hd, zd + hw, outlineColor.red / 255f,
            outlineColor.green / 255f, outlineColor.blue / 255f, 1f, lineWidth.toFloat()
        )

        matrixStack.popPose()
    }

    fun RenderContext.renderBox(
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
    ) = renderBox(x, y, z, width, height, color, color, outline, fill, phase, lineWidth)

    fun RenderContext.renderBoxBounds(
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
        val cam = camera.position()

        matrixStack.pushPose()
        matrixStack.translate(- cam.x, - cam.y, - cam.z)

        if (fill) consumers.getBuffer(if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED).addFilledBoxVertices(
            matrixStack.last(),
            minX,
            minY, minZ, maxX, maxY, maxZ, fillColor.red / 255f,
            fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f
        )

        if (outline) consumers.getBuffer(if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES).addLineBoxVertices(
            matrixStack.last(),
            minX,
            minY, minZ, maxX, maxY, maxZ, outlineColor.red / 255f,
            outlineColor.green / 255f, outlineColor.blue / 255f, 1f, lineWidth.toFloat()
        )

        matrixStack.popPose()
    }

    fun RenderContext.renderBoxBounds(
        aabb: AABB,
        outlineColor: Color,
        fillColor: Color = outlineColor,
        outline: Boolean = true,
        fill: Boolean = true,
        phase: Boolean = false,
        lineWidth: Number = 2.5
    ) = renderBoxBounds(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, outlineColor, fillColor, outline, fill, phase, lineWidth)

    fun RenderContext.renderString(
        text: String,
        x: Number, y: Number, z: Number,
        color: Color = Color.WHITE,
        scale: Number = 1f,
        phase: Boolean = false
    ) {
        val camPos = camera.position()
        val dx = (x.toDouble() - camPos.x).toFloat()
        val dy = (y.toDouble() - camPos.y).toFloat()
        val dz = (z.toDouble() - camPos.z).toFloat()
        val toScale = (scale.toFloat() * 0.025f)

        matrixStack.pushPose()
        matrixStack.translate(dx, dy, dz)
        matrixStack.mulPose(camera.rotation())
        matrixStack.scale(toScale, - toScale, toScale)

        val textLayer = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
        val lines = text.addColor().lineSequence()

        for ((i, line) in lines.withIndex())
            mc.font.drawInBatch(
                line,
                - line.width() / 2f,
                i * 9f,
                color.rgb,
                true,
                matrixStack.last().pose(),
                consumers,
                textLayer,
                0,
                LightCoordsUtil.FULL_BRIGHT
            )

        matrixStack.popPose()
    }

    fun RenderContext.renderString(
        text: String,
        pos: Vec3,
        color: Color = Color.WHITE,
        scale: Number = 1f,
        phase: Boolean = false
    ) = renderString(text, pos.x, pos.y, pos.z, color, scale, phase)

    fun RenderContext.renderRainbowLine(start: Vec3, finish: Vec3, thickness: Number, alpha: Float) {
        matrixStack.pushPose()
        matrixStack.translate(camera.position().reverse())

        val buffer = consumers.getBuffer(NoammRenderLayers.LINES)
        val direction = finish.subtract(start).normalize().toVector3f()
        val timeOffset = (System.currentTimeMillis() % 100000L) / 1000f
        val matrix = matrixStack.last()
        val segments = 10

        for (i in 0 until segments) {
            val t0 = i / segments.toFloat()
            val t1 = (i + 1) / segments.toFloat()

            val p0 = start.lerp(finish, t0.toDouble())
            val p1 = start.lerp(finish, t1.toDouble())

            val hue0 = (t0 - timeOffset).mod(1f)
            val hue1 = (t1 - timeOffset).mod(1f)

            val rgb0 = Color.HSBtoRGB(hue0, 1f, 1f)
            val rgb1 = Color.HSBtoRGB(hue1, 1f, 1f)

            val r0 = ((rgb0 shr 16) and 0xFF) / 255f
            val g0 = ((rgb0 shr 8) and 0xFF) / 255f
            val b0 = (rgb0 and 0xFF) / 255f

            val r1 = ((rgb1 shr 16) and 0xFF) / 255f
            val g1 = ((rgb1 shr 8) and 0xFF) / 255f
            val b1 = (rgb1 and 0xFF) / 255f

            buffer.addVertex(matrix, p0.x.toFloat(), p0.y.toFloat(), p0.z.toFloat()).setColor(r0, g0, b0, alpha).setNormal(matrix, direction).setLineWidth(thickness.toFloat())
            buffer.addVertex(matrix, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat()).setColor(r1, g1, b1, alpha).setNormal(matrix, direction).setLineWidth(thickness.toFloat())
        }

        matrixStack.popPose()
    }

    fun RenderContext.renderLine(start: Vec3, finish: Vec3, color: Color, thickness: Number = 2, phase: Boolean = false) {
        val cameraPos = camera.position()
        matrixStack.pushPose()
        matrixStack.translate(- cameraPos.x, - cameraPos.y, - cameraPos.z)

        val lines = if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES
        val buffer = consumers.getBuffer(lines)

        buffer.addLine(
            matrixStack.last(),
            start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
            finish.x.toFloat(), finish.y.toFloat(), finish.z.toFloat(),
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f,
            thickness.toFloat()
        )

        matrixStack.popPose()
    }

    fun RenderContext.renderTracer(point: Vec3, color: Color, thickness: Number = 2.5) {
        matrixStack.pushPose()
        matrixStack.translate(camera.position().reverse())

        val buffer = consumers.getBuffer(NoammRenderLayers.LINES_THROUGH_WALLS)
        val cameraPoint = camera.position().add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()))

        buffer.addLine(
            matrixStack.last(),
            cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat(),
            point.x.toFloat(), point.y.toFloat(), point.z.toFloat(),
            color.red / 255f, color.green / 255f, color.blue / 255f, 1f,
            thickness.toFloat()
        )

        matrixStack.popPose()
    }

    private fun VertexConsumer.addFilledBoxVertices(pose: PoseStack.Pose, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float) {
        val minX = x1.toFloat() - 0.002f
        val minY = y1.toFloat() - 0.002f
        val minZ = z1.toFloat() - 0.002f
        val maxX = x2.toFloat() + 0.002f
        val maxY = y2.toFloat() + 0.002f
        val maxZ = z2.toFloat() + 0.002f

        addQuad(pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a)
        addQuad(pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a)
        addQuad(pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a)
        addQuad(pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a)
        addQuad(pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a)
        addQuad(pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a)
    }

    private fun VertexConsumer.addLineBoxVertices(pose: PoseStack.Pose, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
        val minX = x1.toFloat() - 0.002f
        val minY = y1.toFloat() - 0.002f
        val minZ = z1.toFloat() - 0.002f
        val maxX = x2.toFloat() + 0.002f
        val maxY = y2.toFloat() + 0.002f
        val maxZ = z2.toFloat() + 0.002f

        addLine(pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, lineWidth)
        addLine(pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, lineWidth)
        addLine(pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, lineWidth)
        addLine(pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, lineWidth)

        addLine(pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth)
        addLine(pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, lineWidth)
        addLine(pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth)
        addLine(pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, lineWidth)

        addLine(pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, lineWidth)
        addLine(pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth)
        addLine(pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, lineWidth)
        addLine(pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth)
    }

    private fun VertexConsumer.addQuad(pose: PoseStack.Pose, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        addVertex(pose, x1, y1, z1).setColor(r, g, b, a)
        addVertex(pose, x2, y2, z2).setColor(r, g, b, a)
        addVertex(pose, x3, y3, z3).setColor(r, g, b, a)
        addVertex(pose, x1, y1, z1).setColor(r, g, b, a)
        addVertex(pose, x3, y3, z3).setColor(r, g, b, a)
        addVertex(pose, x4, y4, z4).setColor(r, g, b, a)
    }

    private fun VertexConsumer.addLine(pose: PoseStack.Pose, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
        val normal = Vector3f(x2 - x1, y2 - y1, z2 - z1).apply { if (lengthSquared() > 0f) normalize() }
        addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, normal).setLineWidth(lineWidth)
        addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, normal).setLineWidth(lineWidth)
    }
}