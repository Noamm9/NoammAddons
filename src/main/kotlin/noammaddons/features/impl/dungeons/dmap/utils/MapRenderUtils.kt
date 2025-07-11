package noammaddons.features.impl.dungeons.dmap.utils

import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapElement.playerMarker
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapPlayer
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.DungeonUtils
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.RenderHelper.bindColor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils
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

    fun renderCenteredText(text: List<String>, x: Int, y: Int, color: Int, scale: Float = DungeonMapConfig.textScale) {
        if (text.isEmpty()) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale, scale, 1f)

        val fontHeight = mc.fontRendererObj.FONT_HEIGHT + 1
        val yTextOffset = text.size * fontHeight / - 2f

        text.forEachIndexed { index, str ->
            mc.fontRendererObj.drawString(
                str.addColor(),
                getStringWidth(str) / - 2f,
                yTextOffset + index * fontHeight,
                color,
                true
            )
        }

        GlStateManager.popMatrix()
    }

    fun drawPlayerHead(player: DungeonMapPlayer) {
        GlStateManager.pushMatrix()

        if (player.teammate.entity == null) {
            GlStateManager.translate(player.mapX, player.mapZ, 0f)
            GlStateManager.rotate(player.yaw + 180f, 0f, 0f, 1f)
        }
        else {
            player.teammate.entity?.let { entityPlayer ->
                val (x, z) = MapUtils.coordsToMap(entityPlayer.renderVec)
                GlStateManager.translate(x, z, 0f)
                GlStateManager.rotate(entityPlayer.rotationYaw + 180f, 0f, 0f, 1f)
            }
        }


        GlStateManager.scale(DungeonMapConfig.playerHeadScale, DungeonMapConfig.playerHeadScale, 1f)

        if (DungeonMapConfig.mapVanillaMarker && player.teammate.name == mc.thePlayer.name) {
            GlStateManager.rotate(180f, 0f, 0f, 1f)
            bindColor(DungeonMapConfig.mapVanillaMarkerColor)
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
            // @formatter:off Render border around the player head
                renderRectBorder(- 6.0, - 6.0, 12.0, 12.0, 1.0, when {
                    DungeonMapConfig.mapPlayerHeadColorClassBased -> player.teammate.clazz.color
                    else -> DungeonMapConfig.mapPlayerHeadColor
                })

                preDraw()
                GlStateManager.enableTexture2D()
                GlStateManager.color(1f, 1f, 1f, 1f)

                mc.textureManager.bindTexture(player.skin)

                Gui.drawScaledCustomSizeModalRect(- 6, - 6, 8f, 8f, 8, 8, 12, 12, 64f, 64f)
                Gui.drawScaledCustomSizeModalRect(- 6, - 6, 40f, 8f, 8, 8, 12, 12, 64f, 64f)

                postDraw()
            }

            // Handle player names
            if (DungeonMapConfig.playerHeads == 2 || DungeonMapConfig.playerHeads == 1 && mc.thePlayer.heldItem.skyblockID.equalsOneOf(
                    "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP", "HAUNT_ABILITY"
                )
            ) {
                if (player.teammate.entity == null) GlStateManager.rotate(player.yaw + 180f, 0f, 0f, - 1f)
                else player.teammate.entity?.let { GlStateManager.rotate(it.rotationYaw + 180f, 0f, 0f, - 1f) }
                GlStateManager.translate(0f, 8f, 0f)
                GlStateManager.scale(DungeonMapConfig.playerNameScale, DungeonMapConfig.playerNameScale, 1f)
                RenderUtils.drawCenteredText(
                    player.teammate.name,
                    0, 0, 1f,
                    if (DungeonMapConfig.mapPlayerNameClassColorBased && player.teammate.clazz != DungeonUtils.Classes.Empty) player.teammate.clazz.color
                    else Color.WHITE
                )
            }
        GlStateManager.popMatrix()
    }

    fun drawPlayerHead(name: String, skin:ResourceLocation, entity: EntityPlayer? = null) {
        GlStateManager.pushMatrix()

        val playerEntity = entity ?: return
        val (x, z) = MapUtils.coordsToMap(playerEntity.renderVec)

        GlStateManager.translate(x, z, 0f)
        GlStateManager.rotate(playerEntity.rotationYaw + 180f, 0f, 0f, 1f)
        GlStateManager.scale(DungeonMapConfig.playerHeadScale, DungeonMapConfig.playerHeadScale, 1f)

        if (DungeonMapConfig.mapVanillaMarker && name == mc.thePlayer.name) {
            GlStateManager.rotate(180f, 0f, 0f, 1f)
            bindColor(DungeonMapConfig.mapVanillaMarkerColor)
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
            // @formatter:off Render border around the player head
            renderRectBorder(- 6.0, - 6.0, 12.0, 12.0, 1.0, when {
                DungeonMapConfig.mapPlayerHeadColorClassBased -> DungeonUtils.Classes.Empty.color
                else -> DungeonMapConfig.mapPlayerHeadColor
            })

            preDraw()
            GlStateManager.enableTexture2D()
            GlStateManager.color(1f, 1f, 1f, 1f)

            mc.textureManager.bindTexture(skin)

            Gui.drawScaledCustomSizeModalRect(- 6, - 6, 8f, 8f, 8, 8, 12, 12, 64f, 64f)
            Gui.drawScaledCustomSizeModalRect(- 6, - 6, 40f, 8f, 8, 8, 12, 12, 64f, 64f)

            postDraw()
        }

        if (DungeonMapConfig.playerHeads == 2 || DungeonMapConfig.playerHeads == 1 && mc.thePlayer.heldItem.skyblockID.equalsOneOf(
                "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP", "HAUNT_ABILITY"
        )) {
            GlStateManager.rotate(playerEntity.rotationYaw + 180f, 0f, 0f, - 1f)
            GlStateManager.translate(0f, 8f, 0f)
            GlStateManager.scale(DungeonMapConfig.playerNameScale, DungeonMapConfig.playerNameScale, 1f)
            RenderUtils.drawCenteredText(name, 0, 0, 1f, Color.WHITE)
        }
        GlStateManager.popMatrix()
    }
}
