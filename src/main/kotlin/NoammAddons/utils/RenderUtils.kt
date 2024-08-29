package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.mixins.AccessorMinecraft
import NoammAddons.utils.ChatUtils.addColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.round


object RenderUtils {
    private val renderManager: RenderManager = mc.renderManager
    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer

    private fun getPartialTicks() = (mc as AccessorMinecraft).timer.renderPartialTicks


    private fun preDraw() {
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    }

    private fun postDraw() {
        GlStateManager.disableBlend()
        GlStateManager.enableTexture2D()
    }

    fun EntityPlayer.getRenderX(): Double = this.lastTickPosX + (this.posX - this.lastTickPosX) * getPartialTicks()
    fun EntityPlayer.getRenderY(): Double = this.lastTickPosY + (this.posY - this.lastTickPosY) * getPartialTicks()
    fun EntityPlayer.getRenderZ(): Double = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * getPartialTicks()

    fun Entity.getRenderX(): Double = this.lastTickPosX + (this.posX - this.lastTickPosX) * getPartialTicks()
    fun Entity.getRenderY(): Double = this.lastTickPosY + (this.posY - this.lastTickPosY) * getPartialTicks()
    fun Entity.getRenderZ(): Double = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * getPartialTicks()

    private fun drawFilledAABB(aabb: AxisAlignedBB, c: Color, alphaMultiplier: Float = 1f) {
        GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f * alphaMultiplier)

        // vertical
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()
        GlStateManager.color(
            c.red / 255f * 0.8f,
            c.green / 255f * 0.8f,
            c.blue / 255f * 0.8f,
            c.alpha / 255f * alphaMultiplier,
        )

        // x
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()
        GlStateManager.color(
            c.red / 255f * 0.9f,
            c.green / 255f * 0.9f,
            c.blue / 255f * 0.9f,
            c.alpha / 255f * alphaMultiplier,
        )
        // z
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()
    }

    private fun drawOutlinedAABB(aabb: AxisAlignedBB, color: Color) {

        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
        worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()

        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
        worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()

        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
        worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()

        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
        worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()

    }

    fun drawBlockBox(blockPos: BlockPos, color: Color, outline: Boolean, fill: Boolean, phase: Boolean = true, LineThickness: Float = 3f) {
        if (!outline && !fill) return

        GlStateManager.pushMatrix()
        preDraw()

        if (phase) {
            glDisable(GL_DEPTH_TEST)
            glDepthMask(false)
        }

        val x = blockPos.x.toDouble()
        val y = blockPos.y.toDouble()
        val z = blockPos.z.toDouble()

        var axisAlignedBB = AxisAlignedBB(x,y,z,x+1,y+1,z+1)
        val block = mc.theWorld.getBlockState(blockPos).block

        if (block != null) {
            block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
                .offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        }

        if (fill) drawFilledAABB(axisAlignedBB, color)

        if (outline) {
            glLineWidth(LineThickness)
            drawOutlinedAABB(axisAlignedBB, color)
        }

        if (phase) {
            glEnable(GL_DEPTH_TEST)
            glDepthMask(true)
        }

        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawEntityBox(entity: Entity, color: Color, outline: Boolean, fill: Boolean, partialTicks: Float) {
        if (!outline && !fill) return
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ

        var axisAlignedBB: AxisAlignedBB
        entity.entityBoundingBox.run {
            axisAlignedBB = AxisAlignedBB(
                minX - entity.posX,
                minY - entity.posY,
                minZ - entity.posZ,
                maxX - entity.posX,
                maxY - entity.posY,
                maxZ - entity.posZ
            ).offset(x, y, z)
        }

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_LIGHTING)
        glDepthMask(false)

        if (outline) {
            glLineWidth(config.espBoxOutlineWidth)
            drawOutlinedAABB(axisAlignedBB, color)
        }
        if (fill) {
            drawFilledAABB(axisAlignedBB, color)
        }

        glDepthMask(true)
        glPopAttrib()
        glPopMatrix()
    }

    fun drawString(text: String, pos: Vec3, color: Color, scale: Float = 1f, shadow: Boolean = true, phase: Boolean = true) {
        val f1 = 0.0266666688
        val width = mc.fontRendererObj.getStringWidth(text) / 2
        GlStateManager.pushMatrix()
        GlStateManager.translate(
            pos.xCoord - renderManager.viewerPosX,
            pos.yCoord - renderManager.viewerPosY,
            pos.zCoord - renderManager.viewerPosZ
        )

        glNormal3f(0f, 1f, 0f)

        if (mc.gameSettings.thirdPersonView != 2) {
            GlStateManager.rotate(-renderManager.playerViewY, 0f, 1f, 0f)
            GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
        }

        else {
            GlStateManager.rotate(-renderManager.playerViewY, 0f, 1f, 0f)
            GlStateManager.rotate(renderManager.playerViewX, -1f, 0f, 0f)
        }

        GlStateManager.scale(-f1, -f1, -f1)
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        if (phase) {
            glDisable(GL_DEPTH_TEST)
            glDepthMask(false)
        }

        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.enableTexture2D()
        mc.fontRendererObj.drawString(text, (-width).toFloat(), 0f, color.rgb, shadow)
        GlStateManager.disableBlend()
        if (phase) {
            glEnable(GL_DEPTH_TEST)
            glDepthMask(true)
        }
        GlStateManager.popMatrix()
    }

    fun draw3DLine(from: Vec3, to: Vec3, color: Color, LineWidth: Int = 6) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableDepth()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.disableTexture2D()
        val renderManager = Minecraft.getMinecraft().renderManager
        val renderPosX = to.xCoord - renderManager.viewerPosX
        val renderPosY = to.yCoord - renderManager.viewerPosY
        val renderPosZ = to.zCoord - renderManager.viewerPosZ
        glLineWidth(LineWidth.toFloat())
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        glBegin(GL_LINES)
        glVertex3d(from.xCoord, from.yCoord, from.zCoord)
        glVertex3d(renderPosX, renderPosY, renderPosZ)
        glEnd()
        glLineWidth(1.0f)
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.resetColor()
        GlStateManager.popMatrix()
    }

    fun drawTracer(pos: Vec3, color: Color, lineWidth: Int = 3) {
        val player = mc.thePlayer
        val playerVec3 = Vec3(
            player.renderOffsetX + renderManager.viewerPosX,
            player.renderOffsetY + player.getEyeHeight() + renderManager.viewerPosY,
            player.renderOffsetZ + renderManager.viewerPosZ
        )

        val startX = (playerVec3.xCoord - renderManager.viewerPosX).toFloat()
        val startY = (playerVec3.yCoord - renderManager.viewerPosY).toFloat()
        val startZ = (playerVec3.zCoord - renderManager.viewerPosZ).toFloat()

        val endX = (pos.xCoord - renderManager.viewerPosX).toFloat()
        val endY = (pos.yCoord - renderManager.viewerPosY).toFloat()
        val endZ = (pos.zCoord - renderManager.viewerPosZ).toFloat()

        // Prepare to draw
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth.toFloat())

        // Convert color to OpenGL format (0.0 to 1.0)
        glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)

        // Draw the line
        glBegin(GL_LINES)
        glVertex3f(startX, startY, startZ)
        glVertex3f(endX, endY, endZ)
        glEnd()

        // Clean up
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_LINE_SMOOTH)
        glPopMatrix()
    }

    fun drawText(text: String, x: Double, y: Double, scale: Double = 1.0) {

        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()


        GlStateManager.scale(scale, scale, scale)
        var yOffset = y - (mc.fontRendererObj.FONT_HEIGHT) / 2

        if (text.contains("\n")) {
            text.split("\n").forEach {
                yOffset += (mc.fontRendererObj.FONT_HEIGHT * scale).toInt()

                mc.fontRendererObj.drawStringWithShadow(
                    it.addColor(),
                    round(x / scale).toFloat(),
                    round(yOffset / scale).toFloat(),
                    0xFFFFFF
                )
            }
        }
        else {
            mc.fontRendererObj.drawStringWithShadow(
                text.addColor(),
                round(x / scale).toFloat(),
                round(yOffset / scale).toFloat(),
                0xFFFFFF
            )
        }

        GlStateManager.popMatrix()
    }

    fun renderItem(itemStack: ItemStack?, x: Int, y: Int) {
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.enableDepth()
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y)
    }

    fun renderTexture(texture: ResourceLocation?, x: Int, y: Int, width: Int = 16, height: Int = 16, enableLighting: Boolean = true) {
        if (enableLighting) RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.enableDepth()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.pushMatrix()
        mc.textureManager.bindTexture(texture)
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableAlpha()
        GlStateManager.alphaFunc(516, 0.1f)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(770, 771)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        GlStateManager.disableAlpha()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableLighting()
        GlStateManager.popMatrix()
    }
}
