package noammaddons.utils

import gg.essential.elementa.components.UIRoundedRectangle.Companion.drawRoundedRectangle
import gg.essential.elementa.utils.LineUtils
import gg.essential.universal.UGraphics
import gg.essential.universal.UGraphics.getStringWidth
import gg.essential.universal.UMatrixStack
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.toVec3
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.distanceIn3DWorld
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.getEyePos
import noammaddons.utils.RenderHelper.applyAlpha
import noammaddons.utils.RenderHelper.bindColor
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderHelper.getRenderX
import noammaddons.utils.RenderHelper.getRenderY
import noammaddons.utils.RenderHelper.getRenderZ
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderHelper.glBindColor
import noammaddons.utils.Utils.isNull
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin


object RenderUtils {
    private val renderManager: RenderManager = mc.renderManager
    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer

    private fun drawOutlinedAABB(aabb: AxisAlignedBB, c: Color) {

        bindColor(c)
        worldRenderer.begin(3, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()

        bindColor(c)
        worldRenderer.begin(3, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()

        bindColor(c)
        worldRenderer.begin(1, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()
    }

    private fun drawFilledAABB(aabb: AxisAlignedBB, color: Color) {

        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()

        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()

        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()

        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()

        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()

        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()
    }


    fun drawBlockBox(
        blockPos: BlockPos,
        overlayColor: Color,
        outlineColor: Color = overlayColor,
        outline: Boolean,
        fill: Boolean,
        phase: Boolean = true,
        LineThickness: Float = 3f
    ) {
        if (! outline && ! fill) throw IllegalArgumentException("outline and fill cannot both be false")
        val distance = distanceIn3DWorld(blockPos.toVec3(), Player?.getRenderVec() ?: return)
        val adjustedLineWidth = (LineThickness.toDouble() / (distance / 8f)).coerceIn(0.5, LineThickness.toDouble()).toFloat()

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableCull()
        GlStateManager.disableTexture2D()
        if (phase) GlStateManager.disableDepth()

        val x = blockPos.x.toDouble()
        val y = blockPos.y.toDouble()
        val z = blockPos.z.toDouble()

        var axisAlignedBB = AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1)
        val block = mc.theWorld.getBlockState(blockPos).block

        if (block != null) {
            block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
                .offset(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        }

        if (fill) drawFilledAABB(axisAlignedBB, overlayColor)

        if (outline) {
            glLineWidth(adjustedLineWidth)
            drawOutlinedAABB(axisAlignedBB, outlineColor.applyAlpha(255))
            glLineWidth(1f)
        }

        if (phase) GlStateManager.enableDepth()

        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.disableBlend()
        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
    }

    fun drawEntityBox(
        entity: Entity, color: Color,
        outline: Boolean = config.espOutlineOpacity != 0f,
        fill: Boolean = config.espFilledOpacity != 0f
    ) {
        if (! outline && ! fill) return
        val x = entity.getRenderX() - renderManager.viewerPosX
        val y = entity.getRenderY() - renderManager.viewerPosY
        val z = entity.getRenderZ() - renderManager.viewerPosZ
        val distance = distanceIn3DWorld(entity.getRenderVec(), Player?.getRenderVec() ?: return)
        val adjustedLineWidth = (config.espOutlineWidth / (distance / 8f)).coerceIn(0.5, config.espOutlineWidth.toDouble()).toFloat()

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

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.disableCull()

        if (outline) {
            glLineWidth(adjustedLineWidth)
            drawOutlinedAABB(axisAlignedBB, color)
            glLineWidth(1f)
        }

        if (fill) drawFilledAABB(axisAlignedBB, color.applyAlpha(config.espFilledOpacity * 255))

        GlStateManager.disableBlend()
        GlStateManager.enableDepth()
        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.popMatrix()
    }

    fun drawBox(x: Number, y: Number, z: Number, color: Color, outline: Boolean, fill: Boolean, width: Number = 1f, height: Number = 1f, phase: Boolean = true, LineThickness: Number = 3f) {
        if (! outline && ! fill) throw IllegalArgumentException("outline and fill cannot both be false")
        val distance = distanceIn3DWorld(Vec3(x.toDouble(), y.toDouble(), z.toDouble()), Player?.getRenderVec() ?: return)
        val adjustedLineWidth = (LineThickness.toDouble() / (distance / 8f)).coerceIn(0.5, LineThickness.toDouble()).toFloat()

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableCull()
        GlStateManager.disableTexture2D()
        if (phase) GlStateManager.disableDepth()

        val axisAlignedBB = AxisAlignedBB(
            x.toDouble(), y.toDouble(), z.toDouble(),
            x.toDouble() + width.toDouble(),
            y.toDouble() + height.toDouble(),
            z.toDouble() + width.toDouble()
        ).expand(.0020000000949949026, .0020000000949949026, .0020000000949949026).offset(
            - renderManager.viewerPosX,
            - renderManager.viewerPosY,
            - renderManager.viewerPosZ
        )

        if (fill) drawFilledAABB(axisAlignedBB, color)

        if (outline) {
            glLineWidth(adjustedLineWidth)
            drawOutlinedAABB(axisAlignedBB, color)
            glLineWidth(1f)
        }

        if (phase) GlStateManager.enableDepth()

        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.disableBlend()

        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
    }

    fun drawBox(from: Vec3, to: Vec3, color: Color, outline: Boolean, fill: Boolean, phase: Boolean = true, LineThickness: Number = 3f) {
        drawBox(
            from.xCoord.toFloat(), from.yCoord.toFloat(), from.zCoord.toFloat(),
            color, outline, fill,
            width = to.xCoord.toFloat() - from.xCoord.toFloat(), height = to.yCoord.toFloat() - from.yCoord.toFloat(),
            phase = phase, LineThickness = LineThickness
        )
    }

    fun drawString(text: String, pos: Vec3, color: Color = Color.WHITE, scale: Float = 1f, renderBlackBox: Boolean = true, shadow: Boolean = true, phase: Boolean = true) {
        val f1 = 0.0266666688
        val lines = text.addColor().split("\n")
        val maxWidth = lines.maxOf { getStringWidth(it) } / 2
        val fixedScale = scale * 1.5f

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        if (phase) {
            GlStateManager.disableDepth()
            GlStateManager.depthMask(false)
        }

        GlStateManager.translate(
            pos.xCoord - renderManager.viewerPosX,
            pos.yCoord - renderManager.viewerPosY,
            pos.zCoord - renderManager.viewerPosZ
        )

        if (mc.gameSettings.thirdPersonView != 2) {
            GlStateManager.rotate(- renderManager.playerViewY, 0f, 1f, 0f)
            GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
        }
        else {
            GlStateManager.rotate(- renderManager.playerViewY, 0f, 1f, 0f)
            GlStateManager.rotate(renderManager.playerViewX, - 1f, 0f, 0f)
        }

        GlStateManager.scale(- f1, - f1, - f1)
        GlStateManager.scale(fixedScale, fixedScale, fixedScale)

        if (renderBlackBox) {
            GlStateManager.disableTexture2D()
            worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
            worldRenderer.pos(- maxWidth - 1.0, - 1.0, 0.0).color(0, 0, 0, 64).endVertex()
            worldRenderer.pos(- maxWidth - 1.0, 9.0 * lines.size, 0.0).color(0, 0, 0, 64).endVertex()
            worldRenderer.pos(maxWidth + 1.0, 9.0 * lines.size, 0.0).color(0, 0, 0, 64).endVertex()
            worldRenderer.pos(maxWidth + 1.0, - 1.0, 0.0).color(0, 0, 0, 64).endVertex()
            tessellator.draw()
            GlStateManager.enableTexture2D()
        }

        lines.forEachIndexed { i, line ->
            UGraphics.drawString(
                UMatrixStack(),
                line,
                - UGraphics.getStringWidth(line) / 2f,
                i * 9f,
                color.rgb, shadow
            )
        }

        if (phase) {
            GlStateManager.enableDepth()
            GlStateManager.depthMask(true)
        }
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
    }

    fun drawString(text: String, x: Number, y: Number, z: Number, color: Color = Color.WHITE, scale: Number = 1f, renderBlackBox: Boolean = false, shadow: Boolean = true, phase: Boolean = false) {
        drawString(
            text = text,
            pos = Vec3(x.toDouble(), y.toDouble(), z.toDouble()),
            color = color,
            renderBlackBox = renderBlackBox,
            scale = scale.toFloat(),
            shadow = shadow,
            phase = phase
        )
    }

    fun draw3DLine(from: Vec3, to: Vec3, color: Color, lineWidth: Float = 4f) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.translate(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        glLineWidth(lineWidth)
        bindColor(color, 255)
        glEnable(GL_LINE_SMOOTH)

        worldRenderer.begin(GL_LINES, DefaultVertexFormats.POSITION)
        worldRenderer.pos(from.xCoord, from.yCoord, from.zCoord).endVertex()
        worldRenderer.pos(to.xCoord, to.yCoord, to.zCoord).endVertex()
        tessellator.draw()

        glLineWidth(1f)
        glDisable(GL_LINE_SMOOTH)
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun drawTracer(pos: Vec3, color: Color, lineWidth: Float = 3f) {
        draw3DLine(getEyePos(), pos, color, lineWidth)
    }

    fun drawText(text: String, x: Float, y: Float, scale: Float = 1f, color: Color = Color.WHITE) {
        val stack = UMatrixStack()

        stack.push()
        stack.scale(scale, scale, scale)
        UGraphics.enableBlend()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)

        var yOffset = y
        val formattedText = text.addColor()
        if (formattedText.contains("\n")) {
            formattedText.split("\n").forEach {
                yOffset += 9 * scale
                UGraphics.drawString(
                    stack,
                    it,
                    x / scale,
                    yOffset / scale,
                    color.rgb, true
                )
            }
        }
        else {
            UGraphics.drawString(
                stack,
                formattedText,
                x / scale,
                yOffset / scale,
                color.rgb, true
            )
        }
        bindColor(Color.WHITE)

        UGraphics.disableBlend()
        stack.pop()
    }

    fun drawCenteredText(text: String, x: Number, y: Number, scale: Number = 1f, color: Color = Color.WHITE) {
        drawText(
            text,
            x.toFloat() - (getStringWidth(text.addColor()) * scale.toFloat() / 2),
            y.toFloat(),
            scale.toFloat(),
            color
        )
    }

    fun drawChromaWaveText(text: String, x: Float, y: Float, scale: Float = 1f, waveSpeed: Float = 4f) {
        val string = text.removeFormatting()
        val time = System.currentTimeMillis()
        val chromaWidth = getStringWidth(string)
        val speed = waveSpeed * 1000.0
        var xPos = x

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        for (i in string.indices) {
            val charWidth = UGraphics.getCharWidth(text[i])

            val waveOffset = i.toFloat() / chromaWidth
            val hue = ((time % speed) / speed + waveOffset) % 1f
            val waveHue = (hue + (sin(waveOffset * Math.PI) * 0.1f)).toFloat() % 1f

            val startColor = Color.getHSBColor(waveHue, 1.0f, 1.0f).rgb
            // val endColor = Color.getHSBColor((waveHue + 0.1f) % 1f, 1.0f, 1.0f).rgb

            UGraphics.drawString(
                UMatrixStack(),
                text[i].toString(),
                xPos / scale,
                y / scale,
                startColor,
                true
            )

            xPos += charWidth * scale
        }

        GlStateManager.popMatrix()
    }

    fun drawCenteredChromaWaveText(text: String, x: Float, y: Float, scale: Float = 1f, waveSpeed: Float = 4f) {
        drawChromaWaveText(
            text,
            x - (getStringWidth(text.removeFormatting()) * scale / 2),
            y, scale, waveSpeed
        )
    }

    /**
     * @author SkytilsMod
     * Modified
     */
    fun renderItem(itemStack: ItemStack?, x: Float, y: Float, zLevel: Float = 100f) {
        val matrixStack = UMatrixStack()
        val itemRenderer = mc.renderItem
        if (itemStack.isNull()) return

        matrixStack.push()
        matrixStack.translate(x, y, zLevel)
        UGraphics.color4f(1f, 1f, 1f, 1f)

        matrixStack.runWithGlobalState {
            GlStateManager.pushMatrix()
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting()
            GlStateManager.enableDepth()
            GlStateManager.depthMask(true)
            GlStateManager.enableBlend()
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
            GlStateManager.enableAlpha()

            itemRenderer.renderItemAndEffectIntoGUI(itemStack, 0, 0)
            itemRenderer.renderItemOverlayIntoGUI(
                itemStack !!.item.getFontRenderer(itemStack) ?: mc.fontRendererObj,
                itemStack, 0, 0, null
            )

            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting()
            GlStateManager.disableBlend()
            GlStateManager.disableAlpha()
            GlStateManager.depthMask(false)
            GlStateManager.disableDepth()
            GlStateManager.popMatrix()
        }
        matrixStack.pop()
        UGraphics.disableLighting()
    }

    fun renderTexture(texture: ResourceLocation?, x: Int, y: Int, w: Int, h: Int) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glBindColor(Color.WHITE)

        mc.textureManager.bindTexture(texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        glBegin(GL_QUADS)
        glTexCoord2f(0f, 1f)
        glVertex2f(x.toFloat(), (y + h).toFloat())
        glTexCoord2f(1f, 1f)
        glVertex2f((x + w).toFloat(), (y + h).toFloat())
        glTexCoord2f(1f, 0f)
        glVertex2f((x + w).toFloat(), y.toFloat())
        glTexCoord2f(0f, 0f)
        glVertex2f(x.toFloat(), y.toFloat())
        glEnd()

        glDisable(GL_BLEND)
        glPopMatrix()
    }

    fun drawCylinder(baseCenter: Vec3, radius: Double, color: Color, phase: Boolean = true) {
        val segments = (36 * radius).toInt()
        val innerRadius = radius - 0.1
        val height = 0.1

        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        bindColor(color)
        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableCull()

        GlStateManager.translate(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        GlStateManager.translate(baseCenter.xCoord, baseCenter.yCoord, baseCenter.zCoord)

        worldRenderer.begin(GL_QUAD_STRIP, DefaultVertexFormats.POSITION)
        for (i in 0 .. segments) {
            val angle = 2.0 * Math.PI * i / segments
            val x = cos(angle)
            val z = sin(angle)
            worldRenderer.pos(x * radius, 0.0, z * radius).endVertex()
            worldRenderer.pos(x * radius, height, z * radius).endVertex()
        }
        tessellator.draw()

        worldRenderer.begin(GL_QUAD_STRIP, DefaultVertexFormats.POSITION)
        for (i in 0 .. segments) {
            val angle = 2.0 * Math.PI * i / segments
            val x = cos(angle)
            val z = sin(angle)
            worldRenderer.pos(x * innerRadius, height, z * innerRadius).endVertex()
            worldRenderer.pos(x * innerRadius, 0.0, z * innerRadius).endVertex()
        }
        tessellator.draw()

        worldRenderer.begin(GL_QUAD_STRIP, DefaultVertexFormats.POSITION)
        for (i in 0 .. segments) {
            val angle = 2.0 * Math.PI * i / segments
            val x = cos(angle)
            val z = sin(angle)
            worldRenderer.pos(x * innerRadius, height, z * innerRadius).endVertex()
            worldRenderer.pos(x * radius, height, z * radius).endVertex()
        }
        tessellator.draw()

        worldRenderer.begin(GL_QUAD_STRIP, DefaultVertexFormats.POSITION)
        for (i in 0 .. segments) {
            val angle = 2.0 * Math.PI * i / segments
            val x = cos(angle)
            val z = sin(angle)
            worldRenderer.pos(x * radius, 0.0, z * radius).endVertex()
            worldRenderer.pos(x * innerRadius, 0.0, z * innerRadius).endVertex()
        }
        tessellator.draw()


        GlStateManager.enableCull()
        if (phase) GlStateManager.enableDepth()
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    fun drawRoundedRect(color: Color, x: Number, y: Number, width: Number, height: Number, radius: Number = 5f) {
        val stack = UMatrixStack()

        stack.push()
        UGraphics.GL.pushMatrix()
        UGraphics.GL.scale(0.25f, 0.25f, 0.25f)
        // downscaling for better resolution

        drawRoundedRectangle(
            stack,
            x.toFloat() * 4,
            y.toFloat() * 4,
            (x.toFloat() + width.toFloat()) * 4,
            (y.toFloat() + height.toFloat()) * 4,
            radius.toFloat() * 4,
            color
        )

        UGraphics.GL.popMatrix()
        stack.pop()
    }

    fun drawPlayerHead(resourceLocation: ResourceLocation, x: Float, y: Float, width: Float, height: Float, radius: Float = 10f) {
        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.translate(x + width / 2, y + height / 2, 0f)

        glEnable(GL_STENCIL_TEST)
        glClear(GL_STENCIL_BUFFER_BIT)

        glStencilFunc(GL_ALWAYS, 1, 0xFF)
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        glStencilMask(0xFF)

        drawRoundedRect(Color.WHITE, - width / 2, - height / 2, width, height, radius)

        glStencilFunc(GL_EQUAL, 1, 0xFF)
        glStencilMask(0x00)

        mc.textureManager.bindTexture(resourceLocation)

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos((- width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(8.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(16.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(16.0 / 64.0, 8.0 / 64.0).endVertex()
        worldRenderer.pos((- width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(8.0 / 64.0, 8.0 / 64.0).endVertex()
        tessellator.draw()

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos((- width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(40.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(48.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(48.0 / 64.0, 8.0 / 64.0).endVertex()
        worldRenderer.pos((- width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(40.0 / 64.0, 8.0 / 64.0).endVertex()
        tessellator.draw()

        glStencilMask(0xFF)
        glDisable(GL_STENCIL_TEST)
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
    }

    fun drawLine(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float) {
        glEnable(GL_LINE_SMOOTH)

        LineUtils.drawLine(
            UMatrixStack(),
            x1, y1, x2, y2,
            color, thickness
        )
        glDisable(GL_LINE_SMOOTH)
    }

    fun drawRoundedBorder(color: Color, x: Number, y: Number, width: Number, height: Number, radius: Number = 5f, thickness: Number = 2f) {
        val radius1 = radius.toFloat() * 0.5142857142857143

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glBindColor(color)
        glScaled(0.25, 0.25, 0.25)
        glLineWidth(thickness.toFloat())
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glBegin(GL_LINE_LOOP)

        for (i in 0 .. 16) {
            val angle = Math.toRadians(180.0) + i * Math.toRadians(90.0) / 16
            val x2 = x.toFloat() + radius1 + cos(angle) * radius1
            val y2 = y.toFloat() + radius1 + sin(angle) * radius1
            glVertex2d(x2 * 4, y2 * 4)
        }

        for (i in 0 .. 16) {
            val angle = Math.toRadians(270.0) + i * Math.toRadians(90.0) / 16
            val x2 = x.toFloat() + width.toFloat() - radius1 + cos(angle) * radius1
            val y2 = y.toFloat() + radius1 + sin(angle) * radius1
            glVertex2d(x2 * 4, y2 * 4)
        }

        for (i in 0 .. 16) {
            val angle = Math.toRadians(.0) + i * Math.toRadians((90).toDouble()) / 16
            val x2 = x.toFloat() + width.toFloat() - radius1 + cos(angle) * radius1
            val y2 = y.toFloat() + height.toFloat() - radius1 + sin(angle) * radius1
            glVertex2d(x2 * 4, y2 * 4)
        }

        for (i in 0 .. 16) {
            val angle = Math.toRadians(90.0) + i * Math.toRadians(90.0) / 16
            val x2 = x.toFloat() + radius1 + cos(angle) * radius1
            val y2 = y.toFloat() + height.toFloat() - radius1 + sin(angle) * radius1
            glVertex2d(x2 * 4, y2 * 4)
        }

        glEnd()

        glDisable(GL_LINE_SMOOTH)
        glShadeModel(GL_FLAT)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glBindColor(Color.WHITE)
        glPopAttrib()
        glPopMatrix()
    }

    fun drawGradientRoundedBorder(
        x: Number,
        y: Number,
        width: Number,
        height: Number,
        radius: Number = 5f,
        lineWidth: Number = 2f,
        color1: Color,
        color2: Color = color1,
        color3: Color = color1,
        color4: Color = color1
    ) {
        val xStart = x.toDouble() * 2
        val yStart = y.toDouble() * 2
        val xEnd = (x.toDouble() + width.toDouble()) * 2
        val yEnd = (y.toDouble() + height.toDouble()) * 2
        val radiusScaled = radius.toDouble() * 2

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableTexture2D()
        GlStateManager.scale(0.5, 0.5, 0.5)

        glLineWidth(lineWidth.toFloat())
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)

        glBegin(GL_LINE_LOOP)

        // Top-left corner (color1 to color3)
        glBindColor(color1)
        for (i in 0 .. 90 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xStart + radiusScaled + sin(angle) * (- radiusScaled),
                yStart + radiusScaled + cos(angle) * (- radiusScaled)
            )
        }

        // Bottom-left corner (color3 to color4)
        glBindColor(color3)
        for (i in 90 .. 180 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xStart + radiusScaled + sin(angle) * (- radiusScaled),
                yEnd - radiusScaled + cos(angle) * (- radiusScaled)
            )
        }

        // Bottom-right corner (color4 to color2)
        glBindColor(color4)
        for (i in 0 .. 90 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xEnd - radiusScaled + sin(angle) * radiusScaled,
                yEnd - radiusScaled + cos(angle) * radiusScaled
            )
        }

        // Top-right corner (color2 to color1)
        glBindColor(color2)
        for (i in 90 .. 180 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xEnd - radiusScaled + sin(angle) * radiusScaled,
                yStart + radiusScaled + cos(angle) * radiusScaled
            )
        }

        glEnd()

        glShadeModel(GL_FLAT)
        glDisable(GL_LINE_SMOOTH)
        glLineWidth(1f)
        GlStateManager.disableBlend()
        GlStateManager.popAttrib()
        glBindColor(Color.WHITE)
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    fun drawRainbowRoundedBorder(x: Number, y: Number, width: Number, height: Number, radius: Number = 5f, thickness: Number = 2f, speed: Number = 1) {
        if (speed.toDouble() < 0) throw IllegalArgumentException("Speed must be positive")

        drawGradientRoundedBorder(
            x, y,
            width, height,
            radius, thickness,
            getRainbowColor(0 * speed.toFloat()),
            getRainbowColor(0.33f * speed.toFloat()),
            getRainbowColor(0.66f * speed.toFloat()),
            getRainbowColor(1 * speed.toFloat())
        )
    }

    fun drawTextWithoutColorLeak(text: String, x: Float, y: Float, scale: Float = 1f, color: Color = Color.WHITE) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.color(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )

        mc.fontRendererObj.drawStringWithShadow(
            text.addColor(),
            x / scale,
            y / scale,
            color.rgb
        )

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun drawGradientRoundedRect(x: Number, y: Number, width: Number, height: Number, radius: Number, color1: Color, color2: Color = color1, color3: Color = color1, color4: Color = color1) {
        val scaledX = x.toDouble() * 2
        val scaledY = y.toDouble() * 2
        val scaledX2 = (x.toDouble() + width.toDouble()) * 2
        val scaledY2 = (y.toDouble() + height.toDouble()) * 2
        val radius1 = radius.toDouble() * 2

        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glScaled(0.5, 0.5, 0.5)

        glShadeModel(GL_SMOOTH)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_LINE_SMOOTH)
        glBegin(GL_POLYGON)

        glColor4f(color1.red / 255f, color1.green / 255f, color1.blue / 255f, color1.alpha / 255f)
        for (i in 0 .. 90 step 3) {
            glVertex2d(
                scaledX + radius1 + sin(i * Math.PI / 180) * (radius1 * - 1.0),
                scaledY + radius1 + cos(i * Math.PI / 180) * (radius1 * - 1.0)
            )
        }

        glColor4f(color3.red / 255f, color3.green / 255f, color3.blue / 255f, color3.alpha / 255f)
        for (i in 90 .. 180 step 3) {
            glVertex2d(
                scaledX + radius1 + sin(i * Math.PI / 180) * (radius1 * - 1.0),
                scaledY2 - radius1 + cos(i * Math.PI / 180) * (radius1 * - 1.0)
            )
        }

        glColor4f(color4.red / 255f, color4.green / 255f, color4.blue / 255f, color4.alpha / 255f)
        for (i in 0 .. 90 step 3) {
            glVertex2d(
                scaledX2 - radius1 + sin(i * Math.PI / 180) * radius1,
                scaledY2 - radius1 + cos(i * Math.PI / 180) * radius1
            )
        }

        glColor4f(color2.red / 255f, color2.green / 255f, color2.blue / 255f, color2.alpha / 255f)
        for (i in 90 .. 180 step 3) {
            glVertex2d(
                scaledX2 - radius1 + sin(i * Math.PI / 180) * radius1,
                scaledY + radius1 + cos(i * Math.PI / 180) * radius1
            )
        }
        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glShadeModel(GL_FLAT)
        glScaled(2.0, 2.0, 2.0)
        glPopAttrib()

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    fun drawWithNoLeak(callback: () -> Unit) {
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        callback()

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }

    fun drawTitle(title: String, subtitle: String, rainbow: Boolean = false) {
        val x = mc.getWidth() / 2f
        val y = mc.getHeight() / 2f - 60

        when (rainbow) {
            true -> {
                drawCenteredChromaWaveText(title, x, y, 3f)
                drawCenteredChromaWaveText(subtitle, x, y + 35, 1.5f)
            }

            false -> {
                drawCenteredText(title, x, y, 3f)
                drawCenteredText(subtitle, x, y + 35, 1.5f)
            }
        }
    }
}