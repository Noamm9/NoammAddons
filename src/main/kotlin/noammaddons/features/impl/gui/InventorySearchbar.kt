package noammaddons.features.impl.gui

import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.ui.components.TextField
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.highlight
import noammaddons.utils.Utils.favoriteColor
import org.lwjgl.input.Keyboard

object InventorySearchbar: Feature("Allows to search for items in any container") {
    private val searchLore by ToggleSetting("Search Lore")
    private val highlightColor by ColorSetting("Hightlight Color", favoriteColor)

    private val searchbar = TextField(0, 0, 200, 20, 2, textRenderer)

    @SubscribeEvent
    fun onGuiRender(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (event.gui is GuiContainer) searchbar.run {
            val sr = ScaledResolution(mc)
            val sf = 2.0 / sr.scaleFactor
            val screenWidth = sr.scaledWidth / sf
            val screenHeight = sr.scaledHeight / sf
            x = ((screenWidth / 2 - (width.toInt()) / 2) + 5)
            y = (screenHeight * 0.95 - height.toInt() / 2)

            GlStateManager.pushMatrix()
            GlStateManager.scale(sf, sf, sf)
            GlStateManager.translate(0f, 0f, 300f)
            draw(0, 0)
            GlStateManager.popMatrix()
        }
    }

    @SubscribeEvent
    fun onGuiKey(event: GuiKeybourdInputEvent) {
        if (event.gui !is GuiContainer) return

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && event.keyCode == Keyboard.KEY_F) {
            searchbar.focused = ! searchbar.focused
            return
        }

        event.isCanceled = searchbar.keyTyped(event.keyChar, event.keyCode)
    }

    @SubscribeEvent
    fun onGuiMouseClick(event: GuiMouseClickEvent) {
        if (event.gui !is GuiContainer) return
        val sf = 2.0 / mc.getScaleFactor()
        searchbar.mouseClicked(event.mouseX.toDouble() / sf, event.mouseY.toDouble() / sf, event.button)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDrawSlot(event: DrawSlotEvent) {
        val text = searchbar.value.lowercase().removeFormatting().takeIf { it.isNotBlank() } ?: return
        val itemstack = event.slot.stack ?: return
        val itemName = itemstack.displayName.removeFormatting().trim().lowercase()
        val lore = (if (searchLore) itemstack.lore.map { it.removeFormatting().lowercase() } else emptyList()) + itemName
        if (text !in lore.joinToString(" ")) return
        event.slot.highlight(highlightColor)
    }
}
