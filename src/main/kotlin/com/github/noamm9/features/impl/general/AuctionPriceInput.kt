package com.github.noamm9.features.impl.general

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IAbstractSignEditScreen
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.ui.utils.componnents.UISearchBox
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.network.PacketUtils.send
import com.github.noamm9.utils.render.Render2D
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.entity.SignBlockEntity
import org.lwjgl.glfw.GLFW
import java.awt.Color

object AuctionPriceInput: Feature("Replaces the sign input with a proper textbox and undercut mode.") {
    private var stack: ItemStack? = null
    private var input = ""
    private var undercut = false

    override fun init() {
        ScreenEvents.AFTER_INIT.register { _, screen, width, height ->
            if (! enabled) return@register
            if (screen !is AbstractSignEditScreen) return@register
            val stack = stack ?: return@register
            val sign = (screen as IAbstractSignEditScreen).getSign() ?: return@register

            val line1 = sign.frontText.getMessage(1, false).string
            val line2 = sign.frontText.getMessage(2, false).string
            val line3 = sign.frontText.getMessage(3, false).string

            if (line1 == "^^^^^^^^^^^^^^^" && line2 == "Your auction" && line3 == "starting bid") mc.execute {
                val existingText = Array(4) { i -> sign.frontText.getMessage(i, false).string }

                // manually setting the screen so the sign gui wont close
                val newscreen = AuctionInputScreen(sign, existingText, stack)
                newscreen.init(mc, width, height)
                mc.screen = newscreen
            }
        }

        register<ContainerEvent.SlotClick> {
            if (event.screen.title.string != "Create BIN Auction") return@register
            if (event.slotId != 31) return@register
            stack = event.screen.menu.getSlot(13).item.takeIf { it.skyblockId.isNotEmpty() }
        }
    }

    private class AuctionInputScreen(
        private val sign: SignBlockEntity,
        private val originalText: Array<String>,
        private val stack: ItemStack
    ): Screen(Component.literal("Auction Price Input")) {

        private lateinit var inputField: UISearchBox
        private var parsedValue: Long? = null
        private var lowestBin = 0L

        override fun init() {
            super.init()
            lowestBin = NoammAddons.priceData[stack.skyblockId] ?: 0L

            val centerX = width / 2
            val centerY = height / 2

            inputField = UISearchBox(centerX - 100, centerY - 20, 200, 22, Component.literal("Price"))
            inputField.value = input
            inputField.setMaxLength(32)
            recalculateValue()

            inputField.setResponder {
                input = it
                recalculateValue()
            }

            addRenderableWidget(inputField)
            setInitialFocus(inputField)

            addRenderableWidget(UIButton(centerX - 100, centerY + 10, 200, 20, "Done") {
                finish()
            })

            addRenderableWidget(UIButton(centerX - 100, centerY + 35, 200, 20, getModeText()) { button ->
                undercut = ! undercut
                button.message = Component.literal(getModeText())
                recalculateValue()
            })
        }

        private fun getModeText() = "Mode: ${if (undercut) "UnderCut" else "Normal"}"

        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val centerX = width / 2
            val centerY = height / 2

            val itemX = centerX - 8
            val itemY = centerY - 75
            guiGraphics.renderItem(stack, itemX, itemY)
            guiGraphics.renderItemDecorations(font, stack, itemX, itemY)

            if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16) {
                val lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).styledLines().drop(1)
                guiGraphics.setTooltipForNextFrame(
                    mc.font,
                    lore,
                    stack.tooltipImage,
                    mouseX, mouseY,
                    stack.get(DataComponents.TOOLTIP_STYLE)
                )
            }

            guiGraphics.drawCenteredString(font,
                if (undercut) "Lowest BIN: ${NumbersUtils.format(lowestBin)}" else "Set Auction Price",
                centerX,
                centerY - 50,
                Color.ORANGE.rgb
            )

            val displayText = if (parsedValue != null) "§aValue: §e${NumbersUtils.formatComma(parsedValue)}"
            else if (inputField.value.isEmpty()) "§7Enter a value (e.g. 10m, 5k)"
            else "§cInvalid format"

            Render2D.drawCenteredString(guiGraphics, displayText, centerX, centerY - 35)

            super.render(guiGraphics, mouseX, mouseY, partialTick)
        }

        override fun keyPressed(event: KeyEvent): Boolean {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                finish()
                return true
            }
            return super.keyPressed(event)
        }

        private fun finish() {
            val finalLine0 = parsedValue?.toString() ?: inputField.value

            ServerboundSignUpdatePacket(
                sign.blockPos, true, finalLine0,
                originalText[1], originalText[2], originalText[3]
            ).send()

            onClose()
        }

        private fun recalculateValue() {
            val textValue = NumbersUtils.parseCompactNumber(inputField.value)

            if (textValue == null) {
                parsedValue = null
                return
            }

            parsedValue = if (undercut) (lowestBin - textValue).coerceAtLeast(0)
            else textValue
        }
    }
}