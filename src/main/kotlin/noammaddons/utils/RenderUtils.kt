package noammaddons.utils

import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.esp.EspSettings
import noammaddons.features.impl.esp.EspSettings.fillOpacity
import noammaddons.features.impl.esp.EspSettings.outlineOpacity
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.NumbersUtils.minus
import noammaddons.utils.NumbersUtils.plus
import noammaddons.utils.PlayerUtils.getEyePos
import noammaddons.utils.RenderHelper.bindColor
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderHelper.glBindColor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderHelper.renderX
import noammaddons.utils.RenderHelper.renderY
import noammaddons.utils.RenderHelper.renderZ
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import kotlin.math.*


object RenderUtils {
    val renderManager: RenderManager = mc.renderManager
    val tessellator: Tessellator = Tessellator.getInstance()
    val worldRenderer: WorldRenderer = tessellator.worldRenderer

    fun preDraw() {
        GlStateManager.shadeModel(GL_SMOOTH)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableTexture2D()
        GlStateManager.disableCull()
        GlStateManager.disableLighting()
        GlStateManager.disableAlpha()
    }

    fun postDraw() {
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.enableCull()
        GlStateManager.enableAlpha()
        GlStateManager.resetColor()
        GlStateManager.shadeModel(GL_FLAT)
    }

    fun enableDepth() {
        GlStateManager.enableDepth()
        GlStateManager.depthMask(true)
    }

    fun disableDepth() {
        GlStateManager.disableDepth()
        GlStateManager.depthMask(false)
    }


    fun drawOutlinedAABB(aabb: AxisAlignedBB, c: Color) {
        bindColor(c)
        worldRenderer.begin(GL_LINES, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
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

    fun drawFilledAABB(aabb: AxisAlignedBB, color: Color) {
        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()
    }

    fun drawBlockBox(blockPos: BlockPos, overlayColor: Color, outlineColor: Color = overlayColor, outline: Boolean, fill: Boolean, phase: Boolean = true, lineWidth: Float = 3f) {
        if (! outline && ! fill) throw IllegalArgumentException("outline and fill cannot both be false")
        val distance = distance3D(blockPos.toVec(), mc.thePlayer?.renderVec ?: return)
        val adjustedLineWidth = (lineWidth.toDouble() / (distance / 8f)).coerceIn(0.5, lineWidth.toDouble()).toFloat()

        GlStateManager.pushMatrix()
        preDraw()
        if (phase) disableDepth()

        val x = blockPos.x.toDouble()
        val y = blockPos.y.toDouble()
        val z = blockPos.z.toDouble()

        var axisAlignedBB = AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1)
        val block = getBlockAt(blockPos)

        if (block != null) {
            block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                .expand(.0020000000949949056, .0020000000949949056, .0020000000949949056)
                .offset(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        }

        if (fill) drawFilledAABB(axisAlignedBB, overlayColor)

        if (outline) {
            glLineWidth(adjustedLineWidth)
            drawOutlinedAABB(axisAlignedBB, outlineColor.withAlpha(255))
            glLineWidth(1f)
        }

        if (phase) enableDepth()

        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawEntityBox(entity: Entity, color: Color, outline: Boolean = outlineOpacity != .0, fill: Boolean = fillOpacity != .0) {
        if (! outline && ! fill) return

        val axisAlignedBB = AxisAlignedBB(
            entity.entityBoundingBox.minX - entity.posX,
            entity.entityBoundingBox.minY - entity.posY,
            entity.entityBoundingBox.minZ - entity.posZ,
            entity.entityBoundingBox.maxX - entity.posX,
            entity.entityBoundingBox.maxY - entity.posY,
            entity.entityBoundingBox.maxZ - entity.posZ
        ).offset(
            entity.renderX - renderManager.viewerPosX,
            entity.renderY - renderManager.viewerPosY,
            entity.renderZ - renderManager.viewerPosZ
        )

        GlStateManager.pushMatrix()
        preDraw()
        if (EspSettings.phase) disableDepth()

        if (outline) {
            glLineWidth(2f)
            drawOutlinedAABB(axisAlignedBB, color.withAlpha(255))
            glLineWidth(1f)
        }

        if (fill) drawFilledAABB(axisAlignedBB, color.withAlpha(fillOpacity.toFloat() / 100f))

        if (EspSettings.phase) enableDepth()
        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawBox(x: Number, y: Number, z: Number, color: Color, outline: Boolean, fill: Boolean, width: Number = 1f, height: Number = 1f, phase: Boolean = true, lineWidth: Number = 3f) {
        if (! outline && ! fill) return

        GlStateManager.pushMatrix()
        preDraw()
        if (phase) disableDepth()

        val axisAlignedBB = AxisAlignedBB(
            x.toDouble(), y.toDouble(), z.toDouble(),
            x.toDouble() + width.toDouble(),
            y.toDouble() + height.toDouble(),
            z.toDouble() + width.toDouble()
        ).expand(.002, .002, .002).offset(
            - renderManager.viewerPosX,
            - renderManager.viewerPosY,
            - renderManager.viewerPosZ
        )

        if (fill) drawFilledAABB(axisAlignedBB, color)

        if (outline) {
            glLineWidth(2f)
            drawOutlinedAABB(axisAlignedBB, color.withAlpha(255))
            glLineWidth(1f)
        }

        if (phase) enableDepth()
        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawBox(from: Vec3, to: Vec3, color: Color, outline: Boolean, fill: Boolean, phase: Boolean = true, LineThickness: Number = 3f) {
        drawBox(
            from.xCoord.toFloat(), from.yCoord.toFloat(), from.zCoord.toFloat(),
            color, outline, fill,
            width = to.xCoord.toFloat() - from.xCoord.toFloat(), height = to.yCoord.toFloat() - from.yCoord.toFloat(),
            phase = phase, lineWidth = LineThickness
        )
    }

    fun drawString(text: String, pos: Vec3, color: Color = Color.WHITE, scale: Float = 1f, phase: Boolean = true) {
        val fText = text.addColor()
        GlStateManager.pushMatrix()
        if (phase) disableDepth()
        GlStateManager.translate(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        GlStateManager.translate(pos.xCoord, pos.yCoord, pos.zCoord)
        glNormal3f(0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(- renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(renderManager.playerViewX, if (mc.gameSettings.thirdPersonView != 2) 1f else - 1f, 0f, 0f)
        val f1 = 0.0266666688
        GlStateManager.scale(- f1, - f1, - f1)
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        mc.fontRendererObj.drawString(fText, - getStringWidth(fText) / 2f, 0f, color.rgb, true)
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        if (phase) enableDepth()
        GlStateManager.popMatrix()
    }

    fun drawString(text: String, x: Number, y: Number, z: Number, color: Color = Color.WHITE, scale: Number = 1f, phase: Boolean = false) {
        drawString(
            text = text,
            pos = Vec3(x.toDouble(), y.toDouble(), z.toDouble()),
            color = color,
            scale = scale.toFloat(),
            phase = phase
        )
    }

    fun drawBackgroundedString(
        text: String,
        pos: Vec3,
        scale: Number = 1,
        phase: Boolean = true,
        backgroundColor: Color = Color.GRAY.withAlpha(0.5f),
        accentColor: Color = Color(0, 114, 255),
        textColor: Color = Color.WHITE
    ) {
        val width = getStringWidth(text) + 2f
        val height = getStringHeight(text) + 2f
        val scaleF = scale.toFloat()
        val f1 = 0.0266666688

        GlStateManager.pushMatrix()
        if (phase) disableDepth()
        GlStateManager.translate(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        GlStateManager.translate(pos.xCoord, pos.yCoord, pos.zCoord)
        glNormal3f(0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(- renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(renderManager.playerViewX, if (mc.gameSettings.thirdPersonView != 2) 1f else - 1f, 0f, 0f)
        GlStateManager.scale(- f1, - f1, - f1)
        GlStateManager.scale(scaleF, scaleF, scaleF)
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        drawRoundedRect(backgroundColor, - width / 2, - height / 2, width, height * 0.9, 2)
        drawRoundedRect(accentColor, - width / 2, - height / 2 + height * 0.9, width, height * 0.1, 2)
        drawCenteredText(text, 0, - (height - 2) / 2, 1f, textColor)
        GlStateManager.disableBlend()
        GlStateManager.enableLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        if (phase) enableDepth()
        GlStateManager.popMatrix()
    }


    fun draw3DLine(from: Vec3, to: Vec3, color: Color, lineWidth: Float = 4f, phase: Boolean = true) {
        GlStateManager.pushMatrix()
        preDraw()
        if (phase) disableDepth()

        GlStateManager.translate(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
        glLineWidth(lineWidth)
        bindColor(color, 255)
        val hadLineSmooth = glIsEnabled(GL_LINE_SMOOTH)
        if (! hadLineSmooth) glEnable(GL_LINE_SMOOTH)

        worldRenderer.begin(GL_LINES, DefaultVertexFormats.POSITION)
        worldRenderer.pos(from.xCoord, from.yCoord, from.zCoord).endVertex()
        worldRenderer.pos(to.xCoord, to.yCoord, to.zCoord).endVertex()
        tessellator.draw()

        glLineWidth(1f)
        if (! hadLineSmooth) glDisable(GL_LINE_SMOOTH)
        if (phase) enableDepth()
        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawTracer(pos: Vec3, color: Color, lineWidth: Float = 3f) = draw3DLine(getEyePos(), pos, color, lineWidth)

    @Suppress("NAME_SHADOWING")
    fun drawText(text: String, x: Number, y: Number, scale: Number = 1f, color: Color = Color.WHITE) {
        val text = text.addColor()
        val (x, y) = (x.toFloat() to y.toFloat())
        val scale = scale.toFloat()

        GlStateManager.pushMatrix()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.translate(x, y, 0f)
        GlStateManager.scale(scale, scale, scale)
        if (text.contains("\n")) {
            text.split("\n").forEachIndexed { i, line ->
                mc.fontRendererObj.drawString(line, 0f, i * 9f, color.rgb, true)
            }
        }
        else mc.fontRendererObj.drawString("$text§r", 0f, 0f, color.rgb, true)
        GlStateManager.resetColor()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun drawText(text: List<String>, x: Number, y: Number, scale: Number = 1f, color: Color = Color.WHITE) {
        val totalHeight = getStringHeight(text, scale)
        val startY = y.toFloat() - (totalHeight / 2) // Center vertically

        text.forEachIndexed { i, line ->
            val lineY = startY + (i * 9f * scale.toFloat())
            drawText(line, x, lineY, scale, color)
        }
    }

    fun drawCenteredText(text: String, x: Number, y: Number, scale: Number = 1f, color: Color = Color.WHITE) {
        drawText(
            text,
            x.toFloat() - (getStringWidth(text, scale) / 2),
            y.toFloat(),
            scale.toFloat(),
            color
        )
    }

    fun drawCenteredText(text: List<String>, x: Number, y: Number, scale: Number = 1f, color: Color = Color.WHITE) {
        val totalHeight = getStringHeight(text, scale)
        val startY = y.toFloat() - (totalHeight / 2) // Center vertically

        text.forEachIndexed { i, line ->
            val lineY = startY + (i * 9f * scale.toFloat())
            drawText(
                line,
                x.toFloat() - (getStringWidth(line, scale) / 2),
                lineY,
                scale.toFloat(),
                color
            )
        }
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

    fun renderItem(itemStack: ItemStack?, x: Float, y: Float, scale: Float = 1f) {
        if (itemStack == null) return
        val itemRender = mc.renderItem

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.translate(x / scale, y / scale, 0f)

        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting()
        itemRender.zLevel = - 145f

        itemRender.renderItemAndEffectIntoGUI(itemStack, 0, 0)
        itemRender.renderItemOverlayIntoGUI(mc.fontRendererObj, itemStack, x.toInt(), y.toInt(), null)

        itemRender.zLevel = 0f
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
    }

    fun drawTexture(texture: ResourceLocation?, x: Number, y: Number, w: Number, h: Number) {
        if (texture == null) return

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        bindColor(Color.WHITE)

        mc.textureManager.bindTexture(texture)

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), y.toDouble() + h.toDouble(), 0.0).tex(.0, 1.0).endVertex()
        worldRenderer.pos(x.toDouble() + w.toDouble(), y.toDouble() + h.toDouble(), 0.0).tex(1.0, 1.0).endVertex()
        worldRenderer.pos(x.toDouble() + w.toDouble(), y.toDouble(), 0.0).tex(1.0, .0).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(.0, .0).endVertex()
        tessellator.draw()

        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
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

    fun drawRoundedRect(color: Color, x: Number, y: Number, width: Number, height: Number, radius: Number = 5) {
        val xd = x.toDouble() * 2.0
        val yd = y.toDouble() * 2.0
        val widthd = width.toDouble() * 2.0
        val heightd = height.toDouble() * 2.0
        val radiusd = radius.toDouble() * 2.0

        GlStateManager.pushMatrix()
        GlStateManager.scale(0.5, 0.5, 0.5)
        preDraw()
        bindColor(color)

        val x1 = xd + radiusd
        val y1 = yd + radiusd
        val x2 = xd + widthd - radiusd
        val y2 = yd + heightd - radiusd

        worldRenderer.begin(GL_POLYGON, DefaultVertexFormats.POSITION)

        for (i in 180 .. 270 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x1 + sin(angle) * radiusd, y1 + cos(angle) * radiusd, 0.0).endVertex()
        }

        for (i in 270 .. 360 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x1 + sin(angle) * radiusd, y2 + cos(angle) * radiusd, 0.0).endVertex()
        }

        for (i in 0 .. 90 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x2 + sin(angle) * radiusd, y2 + cos(angle) * radiusd, 0.0).endVertex()
        }

        for (i in 90 .. 180 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x2 + sin(angle) * radiusd, y1 + cos(angle) * radiusd, 0.0).endVertex()
        }

        tessellator.draw()

        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawRect(color: Color, x: Number, y: Number, width: Number, height: Number) {
        val pos = mutableListOf(x.toFloat(), y.toFloat(), x.toFloat() + width.toFloat(), y.toFloat() + height.toFloat())
        if (pos[0] > pos[2]) Collections.swap(pos, 0, 2)
        if (pos[1] > pos[3]) Collections.swap(pos, 1, 3)

        GlStateManager.pushMatrix()
        preDraw()
        bindColor(color)

        val worldRenderer = Tessellator.getInstance().worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.POSITION)
        worldRenderer.pos(pos[0].toDouble(), pos[3].toDouble(), 0.0).endVertex()
        worldRenderer.pos(pos[2].toDouble(), pos[3].toDouble(), 0.0).endVertex()
        worldRenderer.pos(pos[2].toDouble(), pos[1].toDouble(), 0.0).endVertex()
        worldRenderer.pos(pos[0].toDouble(), pos[1].toDouble(), 0.0).endVertex()
        Tessellator.getInstance().draw()

        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawRectBorder(color: Color, x: Number, y: Number, width: Number, height: Number, lineWidth: Number = 1f) {
        val pos = mutableListOf(x.toFloat(), y.toFloat(), x.toFloat() + width.toFloat(), y.toFloat() + height.toFloat())
        if (pos[0] > pos[2]) Collections.swap(pos, 0, 2)
        if (pos[1] > pos[3]) Collections.swap(pos, 1, 3)

        GlStateManager.pushMatrix()
        preDraw()
        bindColor(color)
        glLineWidth(lineWidth.toFloat())

        worldRenderer.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(pos[0].toDouble(), pos[3].toDouble(), 0.0).endVertex()
        worldRenderer.pos(pos[2].toDouble(), pos[3].toDouble(), 0.0).endVertex()
        worldRenderer.pos(pos[2].toDouble(), pos[1].toDouble(), 0.0).endVertex()
        worldRenderer.pos(pos[0].toDouble(), pos[1].toDouble(), 0.0).endVertex()
        tessellator.draw()

        postDraw()
        glLineWidth(1f)
        GlStateManager.popMatrix()
    }

    fun drawGradientRect(x: Number, y: Number, width: Number, height: Number, topLeft: Color, topRight: Color, bottomLeft: Color, bottomRight: Color) {
        GlStateManager.pushMatrix()
        preDraw()
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldRenderer.pos(x.toDouble(), y.toDouble() + height.toDouble(), .0).color(bottomLeft.red, bottomLeft.green, bottomLeft.blue, bottomLeft.alpha).endVertex()
        worldRenderer.pos(x.toDouble() + width.toDouble(), y.toDouble() + height.toDouble(), .0).color(bottomRight.red, bottomRight.green, bottomRight.blue, bottomRight.alpha).endVertex()
        worldRenderer.pos(x.toDouble() + width.toDouble(), y.toDouble(), .0).color(topRight.red, topRight.green, topRight.blue, topRight.alpha).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), .0).color(topLeft.red, topLeft.green, topLeft.blue, topLeft.alpha).endVertex()
        tessellator.draw()
        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawGradientRect(x: Number, y: Number, width: Number, height: Number, startColor: Color, endColor: Color) {
        drawGradientRect(x, y, width, height, startColor, startColor, endColor, endColor)
    }

    fun drawCheckerboard(x: Number, y: Number, width: Number, height: Number, squareSize: Number) {
        var currentX = x.toDouble()
        while (currentX < x.toDouble() + width.toDouble()) {
            var currentY = y.toDouble()
            var colSwitch = (((currentX - x.toDouble()) / squareSize.toDouble()).toInt() % 2 != 0)
            while (currentY < y.toDouble() + height.toDouble()) {
                val actualSquareWidth = (currentX + squareSize.toDouble()).coerceAtMost(x.toDouble() + width.toDouble()) - currentX
                val actualSquareHeight = (currentY + squareSize.toDouble()).coerceAtMost(y.toDouble() + height.toDouble()) - currentY
                drawRect(
                    if (colSwitch) Color(180, 180, 180) else Color(140, 140, 140),
                    currentX, currentY, actualSquareWidth, actualSquareHeight
                )
                currentY += squareSize.toDouble()
                colSwitch = ! colSwitch
            }
            currentX += squareSize.toDouble()
        }
    }

    fun drawPlayerHead(resourceLocation: ResourceLocation, x: Float, y: Float, width: Float, height: Float, radius: Float = 10f) {
        GlStateManager.pushMatrix()

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableLighting()
        GlStateManager.translate(x + width / 2, y + height / 2, 0f)

        StencilUtils.beginStencilClip {
            drawRoundedRect(Color.BLACK, - width / 2, - height / 2, width, height, radius)
        }

        bindColor(Color.WHITE)
        GlStateManager.enableTexture2D()

        mc.textureManager.bindTexture(resourceLocation)

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos((- width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(8.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(16.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (- width / 2).toDouble(), 0.0).tex(16.0 / 64.0, 8.0 / 64.0).endVertex()
        worldRenderer.pos((- width / 2).toDouble(), (- width / 2).toDouble(), 0.0).tex(8.0 / 64.0, 8.0 / 64.0).endVertex()
        tessellator.draw()

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos((- width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(40.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(48.0 / 64.0, 16.0 / 64.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), (- width / 2).toDouble(), 0.0).tex(48.0 / 64.0, 8.0 / 64.0).endVertex()
        worldRenderer.pos((- width / 2).toDouble(), (- width / 2).toDouble(), 0.0).tex(40.0 / 64.0, 8.0 / 64.0).endVertex()
        tessellator.draw()

        StencilUtils.endStencilClip()

        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun drawLine(color: Color, x1: Number, y1: Number, x2: Number, y2: Number, thickness: Number = 1f) {
        GlStateManager.pushMatrix()
        preDraw()
        glLineWidth(thickness.toFloat())
        bindColor(color)

        val hadSmooth = glIsEnabled(GL_LINE_SMOOTH)
        if (! hadSmooth) glEnable(GL_LINE_SMOOTH)
        worldRenderer.begin(GL_LINES, DefaultVertexFormats.POSITION)
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), 0.0).endVertex()
        tessellator.draw()
        if (! hadSmooth) glDisable(GL_LINE_SMOOTH)

        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawRoundedBorder(color: Color, x: Number, y: Number, width: Number, height: Number, radius: Number = 5f, thickness: Number = 2f) {
        val xd = x.toDouble()
        val yd = y.toDouble()
        val widthd = width.toDouble()
        val heightd = height.toDouble()
        val radiusd = radius.toDouble() * 0.5142857142857143

        GlStateManager.pushMatrix()
        preDraw()
        bindColor(color)

        glLineWidth(thickness.toFloat())

        val x1 = xd + radiusd
        val y1 = yd + radiusd
        val x2 = xd + widthd - radiusd
        val y2 = yd + heightd - radiusd

        worldRenderer.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION)

        for (i in 270 .. 360 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x1 + sin(angle) * radiusd, y2 + cos(angle) * radiusd, 0.0).endVertex()
        }

        for (i in 0 .. 90 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x2 + sin(angle) * radiusd, y2 + cos(angle) * radiusd, 0.0).endVertex()
        }

        for (i in 90 .. 180 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x2 + sin(angle) * radiusd, y1 + cos(angle) * radiusd, 0.0).endVertex()
        }

        for (i in 180 .. 270 step 3) {
            val angle = i * PI / 180
            worldRenderer.pos(x1 + sin(angle) * radiusd, y1 + cos(angle) * radiusd, 0.0).endVertex()
        }

        tessellator.draw()

        glLineWidth(1f)
        postDraw()
        GlStateManager.popMatrix()
    }

    fun drawRainbowRoundedBorder(x: Number, y: Number, width: Number, height: Number, radius: Number = 5f, thickness: Number = 2f, speed: Number = 1) {
        if (speed.toDouble() < 0) throw IllegalArgumentException("Speed must be positive")

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

        glLineWidth(thickness.toFloat())
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)

        glBegin(GL_LINE_LOOP)

        glBindColor(getRainbowColor(0 * speed.toFloat()))
        for (i in 0 .. 90 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xStart + radiusScaled + sin(angle) * (- radiusScaled),
                yStart + radiusScaled + cos(angle) * (- radiusScaled)
            )
        }

        glBindColor(getRainbowColor(0.66f * speed.toFloat()))
        for (i in 90 .. 180 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xStart + radiusScaled + sin(angle) * (- radiusScaled),
                yEnd - radiusScaled + cos(angle) * (- radiusScaled)
            )
        }

        glBindColor(getRainbowColor(1 * speed.toFloat()))
        for (i in 0 .. 90 step 3) {
            val angle = Math.toRadians(i.toDouble())
            glVertex2d(
                xEnd - radiusScaled + sin(angle) * radiusScaled,
                yEnd - radiusScaled + cos(angle) * radiusScaled
            )
        }

        glBindColor(getRainbowColor(0.33f * speed.toFloat()))
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

    fun drawTitle(title: String, subtitle: String, rainbow: Boolean = false) {
        val x = mc.getWidth() / 2f
        val y = mc.getHeight() / 2f - 60

        when (rainbow) {
            true -> {
                drawCenteredChromaWaveText(title.addColor(), x, y, 3f)
                drawCenteredChromaWaveText(subtitle.addColor(), x, y + 35, 1.5f)
            }

            false -> {
                drawCenteredText(title.addColor(), x, y, 3f)
                drawCenteredText(subtitle.addColor(), x, y + 35, 1.5f)
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    fun drawFloatingRect(x: Number, y: Number, width: Number, height: Number, color: Color) {
        val (x, y) = (x.toInt() to y.toInt())
        val (width, height) = (width.toInt() to height.toInt())

        val light = color.brighter().rgb
        val dark = color.darker().rgb

        Gui.drawRect(x, y, x + 1, y + height, light)
        Gui.drawRect(x + 1, y, x + width, y + 1, light)
        Gui.drawRect(x + width - 1, y + 1, x + width, y + height, dark)
        Gui.drawRect(x + 1, y + height - 1, x + width - 1, y + height, dark)
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, color.rgb)
    }

    fun drawSlotOverlay(color: Color, x1: Int, y1: Int, x2: Int, y2: Int) {
        preDraw()

        bindColor(color)
        // Rectangle
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(x1.toDouble(), y2.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), 0.0).endVertex()
        tessellator.draw()

        // Border lines
        glLineWidth(mc.getScaleFactor() / 1.5f)
        bindColor(color, 255)
        worldRenderer.begin(GL_LINES, DefaultVertexFormats.POSITION)
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x1.toDouble(), y2.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x1.toDouble(), y2.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), 0.0).endVertex()
        tessellator.draw()

        bindColor(Color.WHITE)
        postDraw()
        glLineWidth(1f)
    }

    fun drawTexturedModalRect(x: Int, y: Int, textureX: Int, textureY: Int, width: Int, height: Int) {
        val texFactor = 0.00390625 // 1/256, since Minecraft textures are usually 256x256
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 1.0).tex(textureX * texFactor, (textureY + height) * texFactor).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 1.0).tex((textureX + width) * texFactor, (textureY + height) * texFactor).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 1.0).tex((textureX + width) * texFactor, textureY * texFactor).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 1.0).tex(textureX * texFactor, textureY * texFactor).endVertex()
        tessellator.draw()
    }

    fun drawCircle(color: Color, x: Number, y: Number, r: Number) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_CULL_FACE)
        glDisable(GL_TEXTURE_2D)
        glBindColor(color)

        glBegin(GL_TRIANGLE_FAN)
        glVertex2d(x.toDouble(), y.toDouble())

        repeat(360) { i ->
            glVertex2d(
                (r.toDouble() * cos(Math.PI * i / 180) + x.toDouble()),
                (r.toDouble() * sin(Math.PI * i / 180) + y.toDouble())
            )
        }

        glEnd()
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_CULL_FACE)
        glDisable(GL_BLEND)
        glPopMatrix()
    }

    fun drawBlurredShadow(color: Color, x: Number, y: Number, width: Number, height: Number, radius: Number, blurRadius: Number) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0)

        for (i in 0 until blurRadius.toInt()) {
            val alpha = (color.alpha * ((blurRadius.toInt() - i).toFloat() / blurRadius.toInt())).toInt()
            val shadowColor = Color((alpha shl 24) or (color.rgb and 0x00FFFFFF), true)
            drawRoundedRect(shadowColor, x - i, y - i, width + i * 2, height + i * 2, radius + i)
        }

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }
}