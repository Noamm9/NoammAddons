package noammaddons.utils

import gg.essential.elementa.utils.withAlpha
import net.minecraft.block.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.*
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.esp.EspSettings
import noammaddons.features.impl.esp.EspSettings.lineWidth
import noammaddons.utils.BlockUtils.getStateAt
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderHelper.renderX
import noammaddons.utils.RenderHelper.renderY
import noammaddons.utils.RenderHelper.renderZ
import noammaddons.utils.Utils.isOneOf
import org.lwjgl.BufferUtils
import org.lwjgl.util.glu.Project
import java.awt.Color
import java.nio.FloatBuffer
import java.nio.IntBuffer


object RenderUtils2D {
    data class Box2D(val x: Double, val y: Double, val w: Double, val h: Double)

    val modelViewMatrix: FloatBuffer = BufferUtils.createFloatBuffer(16)
    val projectionMatrix: FloatBuffer = BufferUtils.createFloatBuffer(16)
    val viewportDims: IntBuffer = BufferUtils.createIntBuffer(16)

    fun worldToScreenPosition(vec3: Vec3): Vec3? {
        val coords = BufferUtils.createFloatBuffer(3)
        val success = Project.gluProject(
            vec3.xCoord.toFloat(), vec3.yCoord.toFloat(), vec3.zCoord.toFloat(),
            modelViewMatrix, projectionMatrix, viewportDims, coords
        )

        return success.takeIf { it && coords[2] in 0.0 .. 1.0 }?.run {
            val sr = ScaledResolution(mc)
            Vec3(coords[0] / sr.scaleFactor.toDouble(), (sr.scaledHeight - (coords[1] / sr.scaleFactor)).toDouble(), coords[2].toDouble())
        }
    }

    private fun calculateBoundingBox(aabb: AxisAlignedBB): Box2D? {
        var x1 = Double.MAX_VALUE
        var x2 = Double.MIN_VALUE
        var y1 = Double.MAX_VALUE
        var y2 = Double.MIN_VALUE

        aabb.corners.forEach { vertex ->
            worldToScreenPosition(vertex)?.let { vec ->
                x1 = x1.coerceAtMost(vec.xCoord)
                x2 = x2.coerceAtLeast(vec.xCoord)
                y1 = y1.coerceAtMost(vec.yCoord)
                y2 = y2.coerceAtLeast(vec.yCoord)
            }
        }
        return if (x1 != Double.MAX_VALUE) Box2D(x1, y1, x2, y2) else null
    }

    inline val AxisAlignedBB.corners: List<Vec3>
        get() =
            listOf(
                Vec3(minX, minY, minZ), Vec3(minX, maxY, minZ), Vec3(maxX, maxY, minZ), Vec3(maxX, minY, minZ),
                Vec3(minX, minY, maxZ), Vec3(minX, maxY, maxZ), Vec3(maxX, maxY, maxZ), Vec3(maxX, minY, maxZ)
            )

    fun draw2dBackgroundNameTag(
        text: String,
        entity: Entity,
        padding: Number,
        backgroundColor: Color = Color.GRAY.withAlpha(0.5f),
        accentColor: Color = Color.BLUE,
        textColor: Color = Color.WHITE,
        scale: Float = 1f,
    ) {
        worldToScreenPosition(entity.renderVec.add(y = 0.5 + entity.height))?.let {
            val width = RenderHelper.getStringWidth(text) + padding.toDouble()
            val height = RenderHelper.getStringHeight(text) + padding.toDouble()
            GlStateManager.pushMatrix()
            GlStateManager.translate(it.xCoord, it.yCoord, .0)
            GlStateManager.scale(scale, scale, scale)
            RenderUtils.drawRoundedRect(backgroundColor, - width / 2, - height / 2, width, height * 0.9, 2)
            RenderUtils.drawRoundedRect(accentColor, - width / 2, - height / 2 + height * 0.9, width, height * 0.1, 2)
            RenderUtils.drawCenteredText(text, 0, - RenderHelper.getStringHeight(text) / 2, 1f, textColor)
            GlStateManager.popMatrix()
        }
    }


    fun drawNameTag(vec3: Vec3, name: String) {
        worldToScreenPosition(vec3)?.let { pos ->
            mc.fontRendererObj.drawString(
                name, pos.xCoord.toFloat(), pos.yCoord.toFloat(), - 1, true
            )
        }
    }

    fun canSeeThroughSoftBlocks(entity: Entity): Boolean {
        val from = mc.thePlayer.getPositionEyes(RenderHelper.partialTicks)
        val to = entity.getPositionEyes(RenderHelper.partialTicks)
        val step = 0.2
        val distance = from.distanceTo(to)
        val steps = (distance / step).toInt()
        val dir = to.subtract(from).normalize()

        repeat(steps) { i ->
            val point = from.addVector(dir.xCoord * i * step, dir.yCoord * i * step, dir.zCoord * i * step)
            val state = getStateAt(BlockPos(point))
            val block = state.block

            if (block != Blocks.air && block.canCollideCheck(state, false)) {
                return block.isOneOf(BlockGlass::class, BlockStainedGlass::class, BlockFence::class, BlockPane::class, BlockLeaves::class)
            }
        }

        return true
    }

    fun draw2dEsp(entity: Entity, color: Color, thickness: Number = lineWidth) {
        if (! EspSettings.phase && ! canSeeThroughSoftBlocks(entity)) return

        val entityAABB = entity.entityBoundingBox
            .offset(- entity.posX, - entity.posY, - entity.posZ)
            .offset(entity.renderX, entity.renderY, entity.renderZ)

        calculateBoundingBox(entityAABB)?.let { box ->
            val outline = EspSettings.outlineOpacity != .0
            val fill = EspSettings.fillOpacity != .0
            val outlineAlpha = EspSettings.outlineOpacity.toFloat() / 100f
            val fillAlpha = EspSettings.fillOpacity.toFloat() / 100f

            if (fill) RenderUtils.drawRect(color.withAlpha(fillAlpha), box.x, box.y, box.w - box.x, box.h - box.y)
            if (outline) RenderUtils.drawRectBorder(color.withAlpha(outlineAlpha), box.x, box.y, box.w - box.x, box.h - box.y, thickness)
        }
    }
}