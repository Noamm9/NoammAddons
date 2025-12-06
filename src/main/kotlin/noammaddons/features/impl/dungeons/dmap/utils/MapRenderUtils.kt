package noammaddons.features.impl.dungeons.dmap.utils

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
import noammaddons.utils.RenderHelper.renderVec
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object MapRenderUtils {
    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer

    fun addRectToBatch(x: Double, y: Double, w: Double, h: Double, color: Color) {
        val r = color.red
        val g = color.green
        val b = color.blue
        val a = color.alpha

        worldRenderer.pos(x, y + h, .0).color(r, g, b, a).endVertex()
        worldRenderer.pos(x + w, y + h, .0).color(r, g, b, a).endVertex()
        worldRenderer.pos(x + w, y, .0).color(r, g, b, a).endVertex()
        worldRenderer.pos(x, y, .0).color(r, g, b, a).endVertex()
    }

    fun addRectBorderToBatch(x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        addRectToBatch(x - thickness, y, thickness, h, color)
        addRectToBatch(x - thickness, y - thickness, w + thickness * 2, thickness, color)
        addRectToBatch(x + w, y, thickness, h, color)
        addRectToBatch(x - thickness, y + h, w + thickness * 2, thickness, color)
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

        val currentYaw: Float
        if (entity != null && ! entity.isDead) {
            val (x, z) = MapUtils.coordsToMap(entity.renderVec)
            GlStateManager.translate(x, z, 0f)
            currentYaw = entity.rotationYaw
        }
        else {
            GlStateManager.translate(fallbackMapX, fallbackMapZ, 0f)
            currentYaw = fallbackYaw
        }

        GlStateManager.rotate(currentYaw + 180f, 0f, 0f, 1f)
        GlStateManager.scale(DungeonMapConfig.playerHeadScale.value, DungeonMapConfig.playerHeadScale.value, 1f)

        if (DungeonMapConfig.mapVanillaMarker.value && name == mc.thePlayer.name) {
            GlStateManager.rotate(180f, 0f, 0f, 1f)
            RenderHelper.bindColor(DungeonMapConfig.mapVanillaMarkerColor.value)
            GlStateManager.enableTexture2D()
            mc.textureManager.bindTexture(playerMarker)
            drawTexturedQuad(- 6.0, - 6.0, 12.0, 12.0)
            GlStateManager.rotate(- 180f, 0f, 0f, 1f)
        }
        else {
            GlStateManager.disableTexture2D()
            GlStateManager.enableBlend()

            worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
            addRectBorderToBatch(- 6.0, - 6.0, 12.0, 12.0, 1.0, if (DungeonMapConfig.mapPlayerHeadColorClassBased.value) clazz.color else DungeonMapConfig.mapPlayerHeadColor.value)
            tessellator.draw()

            GlStateManager.enableTexture2D()
            GlStateManager.color(1f, 1f, 1f, 1f)
            mc.textureManager.bindTexture(skin)
            worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            worldRenderer.pos(- 6.0, 6.0, 0.0).tex(0.125, 0.25).endVertex()
            worldRenderer.pos(6.0, 6.0, 0.0).tex(0.25, 0.25).endVertex()
            worldRenderer.pos(6.0, - 6.0, 0.0).tex(0.25, 0.125).endVertex()
            worldRenderer.pos(- 6.0, - 6.0, 0.0).tex(0.125, 0.125).endVertex()
            worldRenderer.pos(- 6.0, 6.0, 0.0).tex(0.625, 0.25).endVertex()
            worldRenderer.pos(6.0, 6.0, 0.0).tex(0.75, 0.25).endVertex()
            worldRenderer.pos(6.0, - 6.0, 0.0).tex(0.75, 0.125).endVertex()
            worldRenderer.pos(- 6.0, - 6.0, 0.0).tex(0.625, 0.125).endVertex()
            tessellator.draw()
        }

        val heldItem = ServerPlayer.player.getHeldItem()
        if (DungeonMapConfig.playerHeads.value == 2 || (DungeonMapConfig.playerHeads.value == 1
                    && (heldItem != null && (heldItem.skyblockID == "SPIRIT_LEAP" || heldItem.skyblockID == "INFINITE_SPIRIT_LEAP"
                    || heldItem.skyblockID == "HAUNT_ABILITY")))
        ) {
            GlStateManager.rotate(currentYaw + 180f, 0f, 0f, - 1f)
            GlStateManager.translate(0f, 8f, 0f)
            GlStateManager.scale(DungeonMapConfig.playerNameScale.value, DungeonMapConfig.playerNameScale.value, 1f)
            mc.fontRendererObj.drawString(name, - mc.fontRendererObj.getStringWidth(name) / 2f, 0f,
                                          if (DungeonMapConfig.mapPlayerNameClassColorBased.value && clazz != DungeonUtils.Classes.Empty) clazz.color.rgb else 0xFFFFFF, true
            )
        }

        GlStateManager.popMatrix()
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
}