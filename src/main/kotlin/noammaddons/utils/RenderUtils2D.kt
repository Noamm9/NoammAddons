package noammaddons.utils

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import noammaddons.noammaddons
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderHelper.renderX
import noammaddons.utils.RenderHelper.renderY
import noammaddons.utils.RenderHelper.renderZ
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.Project
import java.awt.Color
import java.nio.FloatBuffer
import java.nio.IntBuffer


object RenderUtils2D {
    data class Box2D(val x: Double, val y: Double, val w: Double, val h: Double)

    val modelViewMatrix: FloatBuffer = BufferUtils.createFloatBuffer(16)
    val projectionMatrix: FloatBuffer = BufferUtils.createFloatBuffer(16)
    val viewportDims: IntBuffer = BufferUtils.createIntBuffer(16)


    /**
     * Projects a 3D point to 2D screen coordinates.
     *
     * @param vec3 The 3D point to be projected.
     * @return The 2D screen coordinates as a Vec3, or null if projection fails.
     */
    private fun worldToScreenPosition(vec3: Vec3): Vec3? {
        val coords = BufferUtils.createFloatBuffer(3)
        val success = Project.gluProject(
            vec3.xCoord.toFloat(), vec3.yCoord.toFloat(), vec3.zCoord.toFloat(),
            modelViewMatrix, projectionMatrix, viewportDims, coords
        )

        return success.takeIf { it && coords[2] in 0.0 .. 1.0 }?.run {
            val sr = ScaledResolution(noammaddons.mc)
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
            noammaddons.mc.fontRendererObj.drawString(
                name, pos.xCoord.toFloat(), pos.yCoord.toFloat(), - 1, true
            )
        }
    }

    fun draw2dEsp(entity: Entity, color: Color, thickness: Float = config.espOutlineWidth) {
        val entityAABB = entity.entityBoundingBox
            .offset(- entity.posX, - entity.posY, - entity.posZ)
            .offset(entity.renderX, entity.renderY, entity.renderZ)

        calculateBoundingBox(entityAABB)?.let { box ->
            drawBox(box, color, thickness)
        }
    }

    fun drawBox(box: Box2D, color: Color, lineWidth: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(lineWidth)
        RenderHelper.bindColor(color)

        RenderUtils.worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)

        // Draw the four lines of the box
        RenderUtils.worldRenderer.pos(box.x, box.y, 0.0).endVertex()  // Top-left to top-right
        RenderUtils.worldRenderer.pos(box.w, box.y, 0.0).endVertex()

        RenderUtils.worldRenderer.pos(box.w, box.y, 0.0).endVertex()  // Top-right to bottom-right
        RenderUtils.worldRenderer.pos(box.w, box.h, 0.0).endVertex()

        RenderUtils.worldRenderer.pos(box.w, box.h, 0.0).endVertex()  // Bottom-right to bottom-left
        RenderUtils.worldRenderer.pos(box.x, box.h, 0.0).endVertex()

        RenderUtils.worldRenderer.pos(box.x, box.h, 0.0).endVertex()  // Bottom-left to top-left
        RenderUtils.worldRenderer.pos(box.x, box.y, 0.0).endVertex()

        RenderUtils.tessellator.draw()

        RenderHelper.bindColor(Color.WHITE)
        GL11.glLineWidth(1f)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GlStateManager.popMatrix()
    }
}