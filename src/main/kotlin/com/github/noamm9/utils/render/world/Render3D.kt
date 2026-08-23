package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.times
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Matrix4f
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

        val minX = pos.x + shape.min(Direction.Axis.X)
        val minY = pos.y + shape.min(Direction.Axis.Y)
        val minZ = pos.z + shape.min(Direction.Axis.Z)
        val maxX = pos.x + shape.max(Direction.Axis.X)
        val maxY = pos.y + shape.max(Direction.Axis.Y)
        val maxZ = pos.z + shape.max(Direction.Axis.Z)

        renderBoxBounds(minX, minY, minZ, maxX, maxY, maxZ, outlineColor, fillColor, outline, fill, phase, lineWidth)
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
        matrixStack.translate(camera.pos.reverse())
        val pose = uMatrixStack()
        val buffer = RenderBatcher.circleBatch(phase)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
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

            buffer.vertex(pose, x1Inner, topY, z1Inner, r, g, b, a)
            buffer.vertex(pose, x1Outer, topY, z1Outer, r, g, b, a)
            buffer.vertex(pose, x2Outer, topY, z2Outer, r, g, b, a)
            buffer.vertex(pose, x2Inner, topY, z2Inner, r, g, b, a)

            buffer.vertex(pose, x1Outer, bottomY, z1Outer, r, g, b, a)
            buffer.vertex(pose, x1Outer, topY, z1Outer, r, g, b, a)
            buffer.vertex(pose, x2Outer, topY, z2Outer, r, g, b, a)
            buffer.vertex(pose, x2Outer, bottomY, z2Outer, r, g, b, a)

            buffer.vertex(pose, x1Inner, bottomY, z1Inner, r, g, b, a)
            buffer.vertex(pose, x1Inner, topY, z1Inner, r, g, b, a)
            buffer.vertex(pose, x2Inner, topY, z2Inner, r, g, b, a)
            buffer.vertex(pose, x2Inner, bottomY, z2Inner, r, g, b, a)

            buffer.vertex(pose, x1Inner, bottomY, z1Inner, r, g, b, a)
            buffer.vertex(pose, x1Outer, bottomY, z1Outer, r, g, b, a)
            buffer.vertex(pose, x2Outer, bottomY, z2Outer, r, g, b, a)
            buffer.vertex(pose, x2Inner, bottomY, z2Inner, r, g, b, a)
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
        val cameraPos = camera.pos
        val segments = (radius.toDouble() * 100).toInt().coerceAtLeast(64)

        matrixStack.pushPose()
        matrixStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z)
        matrixStack.mulPose(camera.orientation)
        val pose = uMatrixStack()
        val buffer = RenderBatcher.filledBatch(phase)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

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

            buffer.vertex(pose, i1x, i1y, 0f, r, g, b, a)
            buffer.vertex(pose, o1x, o1y, 0f, r, g, b, a)
            buffer.vertex(pose, o2x, o2y, 0f, r, g, b, a)

            buffer.vertex(pose, i1x, i1y, 0f, r, g, b, a)
            buffer.vertex(pose, o2x, o2y, 0f, r, g, b, a)
            buffer.vertex(pose, i2x, i2y, 0f, r, g, b, a)
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
        val hw = width.toDouble() / 2.0

        renderBoxBounds(
            x.toDouble() - hw, y.toDouble(), z.toDouble() - hw,
            x.toDouble() + hw, y.toDouble() + height.toDouble(), z.toDouble() + hw,
            outlineColor, fillColor, outline, fill, phase, lineWidth
        )
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

        matrixStack.pushPose()
        matrixStack.translate(camera.pos.reverse())
        val pose = uMatrixStack()

        if (fill) RenderBatcher.filledBatch(phase).addFilledBoxVertices(
            pose,
            minX,
            minY, minZ, maxX, maxY, maxZ, fillColor.red / 255f,
            fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f
        )

        if (outline) RenderBatcher.lineBatch(phase).addLineBoxVertices(
            pose,
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
        val dx = (x.toDouble() - camera.pos.x).toFloat()
        val dy = (y.toDouble() - camera.pos.y).toFloat()
        val dz = (z.toDouble() - camera.pos.z).toFloat()
        val toScale = (scale.toFloat() * 0.025f)

        matrixStack.pushPose()
        matrixStack.translate(dx, dy, dz)
        matrixStack.mulPose(camera.orientation)
        matrixStack.scale(toScale, - toScale, toScale)

        val matrix = Matrix4f(matrixStack.last().pose())
        for ((i, line) in text.addColor().lineSequence().withIndex())
            RenderBatcher.addText(matrix, line, - line.width() / 2f, i * 9f, color.rgb, phase)

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
        matrixStack.translate(camera.pos.reverse())
        val pose = uMatrixStack()

        val buffer = RenderBatcher.lineBatch(false)
        val direction = finish.subtract(start).normalize()
        val timeOffset = (System.currentTimeMillis() % 100000L) / 1000f
        val segments = 10
        val nx = direction.x.toFloat()
        val ny = direction.y.toFloat()
        val nz = direction.z.toFloat()
        val w = thickness.toFloat()

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

            buffer.vertex(pose, p0.x.toFloat(), p0.y.toFloat(), p0.z.toFloat(), r0, g0, b0, alpha, nx, ny, nz, w)
            buffer.vertex(pose, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat(), r1, g1, b1, alpha, nx, ny, nz, w)
        }

        matrixStack.popPose()
    }

    fun RenderContext.renderLine(start: Vec3, finish: Vec3, color: Color, thickness: Number = 2, phase: Boolean = false) {
        matrixStack.pushPose()
        matrixStack.translate(camera.pos.reverse())

        RenderBatcher.lineBatch(phase).line(
            uMatrixStack(),
            start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
            finish.x.toFloat(), finish.y.toFloat(), finish.z.toFloat(),
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f,
            thickness.toFloat()
        )

        matrixStack.popPose()
    }

    fun RenderContext.renderTracer(point: Vec3, color: Color, thickness: Number = 2.5) {
        matrixStack.pushPose()
        matrixStack.translate(camera.pos.reverse())

        val cameraPoint = camera.pos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot))
        RenderBatcher.lineBatch(phase = true).line(
            uMatrixStack(),
            cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat(),
            point.x.toFloat(), point.y.toFloat(), point.z.toFloat(),
            color.red / 255f, color.green / 255f, color.blue / 255f, 1f,
            thickness.toFloat()
        )

        matrixStack.popPose()
    }
}
