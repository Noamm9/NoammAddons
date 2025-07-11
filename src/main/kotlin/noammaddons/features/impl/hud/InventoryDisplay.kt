package noammaddons.features.impl.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.RenderHelper.bindColor
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawRectBorder
import noammaddons.utils.RenderUtils.tessellator
import noammaddons.utils.RenderUtils.worldRenderer
import org.lwjgl.opengl.GL11
import java.awt.Color

object InventoryDisplay: Feature() {
    private val showHotbar = ToggleSetting("Show Hotbar")
    private val borderColor = ColorSetting("Border Color", Color.WHITE)
    private val slotBackgroundColor = ColorSetting("Slot Background Color", Color(33, 33, 33, 150))

    override fun init() = addSettings(
        showHotbar,
        SeperatorSetting("Colors"),
        borderColor, slotBackgroundColor
    )

    object InventoryDisplayElement: GuiElement(hudData.getData().inventoryDisplay) {
        override val enabled get() = InventoryDisplay.enabled
        override val width: Float get() = 9 * 18f
        override val height: Float get() = if (showHotbar.value) 3 * 18f + 20f else 3 * 18f

        override fun draw() {
            val rawInventory = mc.thePlayer.inventory.mainInventory

            GlStateManager.pushMatrix()
            val depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
            if (! depthWasEnabled) GlStateManager.enableDepth()
            GlStateManager.scale(getScale(), getScale(), getScale())
            GlStateManager.translate(getX() / getScale(), getY() / getScale(), 1f)

            drawRectBorder(borderColor.value, - 1, - 1, width, height, 2f)

            val numDisplaySlots = if (showHotbar.value) 36 else 27

            RenderUtils.preDraw()
            bindColor(slotBackgroundColor.value)
            worldRenderer.begin(7, DefaultVertexFormats.POSITION)

            for (displayIndex in 0 until numDisplaySlots) {
                val col = displayIndex % 9
                val row = displayIndex / 9
                val itemX = col * 18f
                val itemY = if (row < 3) row * 18f else (3 * 18f) + 2f

                worldRenderer.pos(itemX.toDouble(), (itemY + 16f).toDouble(), 0.0).endVertex()
                worldRenderer.pos((itemX + 16f).toDouble(), (itemY + 16f).toDouble(), 0.0).endVertex()
                worldRenderer.pos((itemX + 16f).toDouble(), itemY.toDouble(), 0.0).endVertex()
                worldRenderer.pos(itemX.toDouble(), itemY.toDouble(), 0.0).endVertex()
            }
            tessellator.draw()
            RenderUtils.postDraw()

            RenderHelper.enableGUIStandardItemLighting()
            for (displayIndex in 0 until numDisplaySlots) {
                val sourceInventoryIndex: Int = if (showHotbar.value) {
                    if (displayIndex < 27) 9 + displayIndex else displayIndex - 27
                }
                else 9 + displayIndex

                val itemStack = rawInventory.getOrNull(sourceInventoryIndex) ?: continue
                val col = displayIndex % 9
                val row = displayIndex / 9
                val itemX = col * 18
                val itemY = if (row < 3) row * 18 else (3 * 18) + 2

                mc.renderItem.renderItemAndEffectIntoGUI(itemStack, itemX, itemY)
                if (itemStack.stackSize > 1) mc.renderItem.renderItemOverlays(mc.fontRendererObj, itemStack, itemX, itemY)
            }
            RenderHelper.disableStandardItemLighting()

            if (! depthWasEnabled) GlStateManager.disableDepth()
            GlStateManager.popMatrix()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) = InventoryDisplayElement.draw()
}
