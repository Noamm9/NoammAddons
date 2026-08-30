package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.times
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
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

        val minX = pos.x + shape.min(Direction.Axis.X) - 0.002
        val minY = pos.y + shape.min(Direction.Axis.Y) - 0.002
        val minZ = pos.z + shape.min(Direction.Axis.Z) - 0.002
        val maxX = pos.x + shape.max(Direction.Axis.X) + 0.002
        val maxY = pos.y + shape.max(Direction.Axis.Y) + 0.002
        val maxZ = pos.z + shape.max(Direction.Axis.Z) + 0.002

        matrixStack.pushPose()
        matrixStack.translate(camera.position().reverse())

        if (fill) collector.submitCustomGeometry(matrixStack, if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED) { pose, buffer ->
            addFilledBoxVertices(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, fillColor.red / 255f, fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f)
        }

        if (outline) collector.submitCustomGeometry(matrixStack, if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES) { pose, buffer ->
            renderLineBox(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, outlineColor.red / 255f, outlineColor.green / 255f, outlineColor.blue / 255f, 1f, lineWidth.toFloat())
        }

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
        val layer = if (phase) NoammRenderLayers.CIRCLE_FILLED_THROUGH_WALLS else NoammRenderLayers.CIRCLE_FILLED

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

        collector.submitCustomGeometry(matrixStack, layer) { pose, buffer ->
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
        val camera = this.camera
        val cameraPos = camera.position()
        val segments = (radius.toDouble() * 100).toInt().coerceAtLeast(64)

        matrixStack.pushPose()
        matrixStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z)
        matrixStack.mulPose(camera.rotation())

        val layer = if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

        val thicknessVal = thickness.toDouble() / 40.0
        val radiusVal = radius.toDouble()
        val innerR = (radiusVal - thicknessVal).coerceAtLeast(0.0)
        val outerR = radiusVal + thicknessVal

        val step = 2.0 * Math.PI / segments

        collector.submitCustomGeometry(matrixStack, layer) { pose, buffer ->
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

                buffer.addVertex(pose, i1x, i1y, 0f).setColor(r, g, b, a)
                buffer.addVertex(pose, o1x, o1y, 0f).setColor(r, g, b, a)
                buffer.addVertex(pose, o2x, o2y, 0f).setColor(r, g, b, a)

                buffer.addVertex(pose, i1x, i1y, 0f).setColor(r, g, b, a)
                buffer.addVertex(pose, o2x, o2y, 0f).setColor(r, g, b, a)
                buffer.addVertex(pose, i2x, i2y, 0f).setColor(r, g, b, a)
            }
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

        if (fill) collector.submitCustomGeometry(matrixStack, if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED) { pose, buffer ->
            addFilledBoxVertices(pose, buffer, xd - hw, yd, zd - hw, xd + hw, yd + hd, zd + hw, fillColor.red / 255f, fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f)
        }

        if (outline) collector.submitCustomGeometry(matrixStack, if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES) { pose, buffer ->
            renderLineBox(pose, buffer, xd - hw, yd, zd - hw, xd + hw, yd + hd, zd + hw, outlineColor.red / 255f, outlineColor.green / 255f, outlineColor.blue / 255f, 1f, lineWidth.toFloat())
        }

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

        if (fill) collector.submitCustomGeometry(matrixStack, if (phase) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED) { pose, buffer ->
            addFilledBoxVertices(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, fillColor.red / 255f, fillColor.green / 255f, fillColor.blue / 255f, fillColor.alpha / 255f)
        }

        if (outline) collector.submitCustomGeometry(matrixStack, if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES) { pose, buffer ->
            renderLineBox(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, outlineColor.red / 255f, outlineColor.green / 255f, outlineColor.blue / 255f, 1f, lineWidth.toFloat())
        }

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
        val toScale = (scale.toFloat() * 0.025f)
        val textRenderer = mc.font
        val camera = this.camera
        val camPos = camera.position()
        val dx = (x.toDouble() - camPos.x).toFloat()
        val dy = (y.toDouble() - camPos.y).toFloat()
        val dz = (z.toDouble() - camPos.z).toFloat()

        val textLayer = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
        val lines = text.addColor().split("\n")

        matrixStack.pushPose()
        matrixStack.translate(dx, dy, dz)
        matrixStack.mulPose(camera.rotation())
        matrixStack.scale(toScale, - toScale, toScale)

        for ((i, line) in lines.withIndex()) collector.submitText(
            matrixStack,
            - textRenderer.width(line) / 2f,
            i * 9f,
            Component.literal(line).visualOrderText,
            true,
            textLayer,
            LightCoordsUtil.FULL_BRIGHT,
            color.rgb,
            0,
            0
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

        val direction = finish.subtract(start).normalize().toVector3f()
        val timeOffset = (System.currentTimeMillis() % 100000L) / 1000f
        val segments = 10
        val nx = direction.x
        val ny = direction.y
        val nz = direction.z
        val w = thickness.toFloat()

        collector.submitCustomGeometry(matrixStack, NoammRenderLayers.LINES) { pose, buffer ->
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

                buffer.addVertex(pose, p0.x.toFloat(), p0.y.toFloat(), p0.z.toFloat()).setColor(r0, g0, b0, alpha).setNormal(pose, direction).setLineWidth(w)
                buffer.addVertex(pose, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat()).setColor(r1, g1, b1, alpha).setNormal(pose, direction).setLineWidth(w)
            }
        }

        matrixStack.popPose()
    }

    fun RenderContext.renderLine(start: Vec3, finish: Vec3, color: Color, thickness: Number = 2, phase: Boolean = false) {
        val cameraPos = camera.position()
        matrixStack.pushPose()
        matrixStack.translate(- cameraPos.x, - cameraPos.y, - cameraPos.z)

        val lines = if (phase) NoammRenderLayers.LINES_THROUGH_WALLS else NoammRenderLayers.LINES

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val direction = finish.subtract(start).normalize().toVector3f()

        collector.submitCustomGeometry(matrixStack, lines) { pose, buffer ->
            buffer.addVertex(pose, start.x.toFloat(), start.y.toFloat(), start.z.toFloat()).setColor(r, g, b, a).setNormal(pose, direction).setLineWidth(thickness.toFloat())
            buffer.addVertex(pose, finish.x.toFloat(), finish.y.toFloat(), finish.z.toFloat()).setColor(r, g, b, a).setNormal(pose, direction).setLineWidth(thickness.toFloat())
        }

        matrixStack.popPose()
    }

    fun RenderContext.renderTracer(point: Vec3, color: Color, thickness: Number = 2.5) {
        matrixStack.pushPose()
        matrixStack.translate(- camera.position().x, - camera.position().y, - camera.position().z)

        val cameraPoint = camera.position().add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()))
        val normal = point.toVector3f().sub(cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).normalize()

        collector.submitCustomGeometry(matrixStack, NoammRenderLayers.LINES_THROUGH_WALLS) { pose, buffer ->
            buffer.addVertex(pose, cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).setColor(color.red / 255f, color.green / 255f, color.blue / 255f, 1f).setNormal(pose, normal).setLineWidth(thickness.toFloat())
            buffer.addVertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat()).setColor(color.red / 255f, color.green / 255f, color.blue / 255f, 1f).setNormal(pose, normal).setLineWidth(thickness.toFloat())
        }

        matrixStack.popPose()
    }

    fun addFilledBoxVertices(pose: PoseStack.Pose, buffer: VertexConsumer, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float) {
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

    private fun addQuad(buffer: VertexConsumer, pose: PoseStack.Pose, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a)
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a)
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a)
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a)
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a)
        buffer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a)
    }

    fun renderLineBox(pose: PoseStack.Pose, buffer: VertexConsumer, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
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
