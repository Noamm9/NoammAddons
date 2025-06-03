package noammaddons.features.impl.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawRectBorder
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
            GlStateManager.enableDepth()
            GlStateManager.scale(getScale(), getScale(), getScale())
            GlStateManager.translate(getX() / getScale(), getY() / getScale(), 1f)

            drawRectBorder(borderColor.value, - 1, - 1, width, height, 2f)

            val numDisplaySlots = if (showHotbar.value) 36 else 27

            for (displayIndex in 0 until numDisplaySlots) {
                val sourceInventoryIndex: Int = if (showHotbar.value)
                    if (displayIndex < 27) 9 + displayIndex
                    else displayIndex - 27
                else 9 + displayIndex

                val itemStack = rawInventory.getOrNull(sourceInventoryIndex)
                val col = displayIndex % 9
                val row = displayIndex / 9

                val itemX = col * 18
                val itemY = if (row < 3) row * 18 else (3 * 18) + 2

                drawRect(slotBackgroundColor.value, itemX.toFloat(), itemY.toFloat(), 16f, 16f)

                if (itemStack != null) {
                    RenderHelper.enableGUIStandardItemLighting()
                    mc.renderItem.renderItemAndEffectIntoGUI(itemStack, itemX, itemY)
                    mc.renderItem.renderItemOverlays(mc.fontRendererObj, itemStack, itemX, itemY)
                    RenderHelper.disableStandardItemLighting()
                }
            }

            if (! depthWasEnabled) GlStateManager.disableDepth()
            GlStateManager.popMatrix()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) = InventoryDisplayElement.draw()
}
