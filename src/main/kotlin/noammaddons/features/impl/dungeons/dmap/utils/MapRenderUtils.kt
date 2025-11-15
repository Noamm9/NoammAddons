package noammaddons.features.impl.dungeons.dmap.utils

import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapElement.playerMarker
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderHelper.bindColor
import noammaddons.utils.Utils.equalsOneOf
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object MapRenderUtils {
    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer

    private fun preDraw() {
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.disableDepth()
        GlStateManager.disableLighting()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    }

    private fun postDraw() {
        GlStateManager.disableBlend()
        GlStateManager.enableDepth()
        GlStateManager.enableTexture2D()
    }

    private fun addQuadVertices(x: Double, y: Double, w: Double, h: Double) {
        worldRenderer.pos(x, y + h, 0.0).endVertex()
        worldRenderer.pos(x + w, y + h, 0.0).endVertex()
        worldRenderer.pos(x + w, y, 0.0).endVertex()
        worldRenderer.pos(x, y, 0.0).endVertex()
    }

    fun drawTexturedQuad(x: Double, y: Double, width: Double, height: Double) {
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x, y + height, 0.0).tex(0.0, 1.0).endVertex()
        worldRenderer.pos(x + width, y + height, 0.0).tex(1.0, 1.0).endVertex()
        worldRenderer.pos(x + width, y, 0.0).tex(1.0, 0.0).endVertex()
        worldRenderer.pos(x, y, 0.0).tex(0.0, 0.0).endVertex()
        tessellator.draw()
    }

    fun colorizeScore(score: Int): String {
        return when {
            score < 270 -> "§c${score}"
            score < 300 -> "§e${score}"
            else -> "§a${score}"
        }
    }

    fun renderRect(x: Double, y: Double, w: Double, h: Double, color: Color) {
        if (color.alpha == 0) return
        preDraw()
        bindColor(color)
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        addQuadVertices(x, y, w, h)
        tessellator.draw()
        postDraw()
    }

    fun renderRectBorder(x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        if (color.alpha == 0) return
        preDraw()
        bindColor(color)

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        addQuadVertices(x - thickness, y, thickness, h)
        addQuadVertices(x - thickness, y - thickness, w + thickness * 2, thickness)
        addQuadVertices(x + w, y, thickness, h)
        addQuadVertices(x - thickness, y + h, w + thickness * 2, thickness)
        tessellator.draw()

        postDraw()
    }

    fun renderCenteredText(text: List<String>, x: Float, y: Float, color: Int, scale: Float = 1.0f) {
        if (text.isEmpty()) return

        GlStateManager.enableTexture2D()

        if (scale == 1.0f) drawTextLinesInternal(text, x, y, color)
        else {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 0f)
            GlStateManager.scale(scale, scale, 1f)
            drawTextLinesInternal(text, 0f, 0f, color)
            GlStateManager.popMatrix()
        }
    }

    private fun drawTextLinesInternal(text: List<String>, x: Float, y: Float, color: Int) {
        val fontHeight = mc.fontRendererObj.FONT_HEIGHT + 1
        val totalTextHeight = text.size * fontHeight
        val startY = y - (totalTextHeight / 2f)

        text.forEachIndexed { index, line ->
            val startX = x - (RenderHelper.getStringWidth(line) / 2f)
            mc.fontRendererObj.drawString(line.addColor(), startX, startY + index * fontHeight, color, true)
        }
    }

    fun drawPlayerHead(
        name: String,
        skin: ResourceLocation,
        clazz: DungeonUtils.Classes,
        entity: EntityPlayer?,
        fallbackMapX: Float = 0f,
        fallbackMapZ: Float = 0f,
        fallbackYaw: Float = 0f
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.enableTexture2D()

        val currentYaw: Float
        val liveEntity = entity?.takeUnless { it.isDead }

        if (liveEntity != null) {
            val (x, z) = MapUtils.coordsToMap(liveEntity.positionVector)
            GlStateManager.translate(x, z, 0f)
            currentYaw = liveEntity.rotationYaw
        }
        else {
            GlStateManager.translate(fallbackMapX, fallbackMapZ, 0f)
            currentYaw = fallbackYaw
        }

        GlStateManager.rotate(currentYaw + 180f, 0f, 0f, 1f)
        GlStateManager.scale(DungeonMapConfig.playerHeadScale.value, DungeonMapConfig.playerHeadScale.value, 1f)

        if (DungeonMapConfig.mapVanillaMarker.value && name == mc.thePlayer.name) {
            GlStateManager.rotate(180f, 0f, 0f, 1f)
            bindColor(DungeonMapConfig.mapVanillaMarkerColor.value)
            mc.textureManager.bindTexture(playerMarker)
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
            worldRenderer.pos(- 6.0, 6.0, 0.0).tex(0.0, 0.0).endVertex()
            worldRenderer.pos(6.0, 6.0, 0.0).tex(1.0, 0.0).endVertex()
            worldRenderer.pos(6.0, - 6.0, 0.0).tex(1.0, 1.0).endVertex()
            worldRenderer.pos(- 6.0, - 6.0, 0.0).tex(0.0, 1.0).endVertex()
            tessellator.draw()
            GlStateManager.rotate(- 180f, 0f, 0f, 1f)
        }
        else {
            // @formatter:off
            renderRectBorder(- 6.0, - 6.0, 12.0, 12.0, 1.0, when {
                DungeonMapConfig.mapPlayerHeadColorClassBased.value -> clazz.color
                else -> DungeonMapConfig.mapPlayerHeadColor.value
            })

            preDraw()
            GlStateManager.enableTexture2D()
            bindColor(Color.WHITE)
            mc.textureManager.bindTexture(skin)

            Gui.drawScaledCustomSizeModalRect(- 6, - 6, 8f, 8f, 8, 8, 12, 12, 64f, 64f)
            Gui.drawScaledCustomSizeModalRect(- 6, - 6, 40f, 8f, 8, 8, 12, 12, 64f, 64f)

            postDraw()
        }

        if (DungeonMapConfig.playerHeads.value == 2 || (DungeonMapConfig.playerHeads.value == 1 && mc.thePlayer.heldItem.skyblockID.equalsOneOf(
            "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP", "HAUNT_ABILITY"
        ))) {
            GlStateManager.rotate(currentYaw + 180f, 0f, 0f, - 1f)
            GlStateManager.translate(0f, 8f, 0f)
            GlStateManager.scale(DungeonMapConfig.playerNameScale.value, DungeonMapConfig.playerNameScale.value, 1f)
            RenderUtils.drawCenteredText(name, 0, 0, 1f, if (DungeonMapConfig.mapPlayerNameClassColorBased.value && clazz != DungeonUtils.Classes.Empty) clazz.color else Color.WHITE)
        }

        GlStateManager.popMatrix()
    }
}