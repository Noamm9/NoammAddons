package com.github.noamm9.features.impl.general

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IAbstractSignEditScreen
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.ui.utils.componnents.UISearchBox
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.Utils.uppercaseFirst
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
    private val defaultMode by DropdownSetting("Default mode", 0, listOf("Normal", "Undercut"))
        .withDescription("The default input mode that will be used when you open the menu")

    private val rememberInput by MultiCheckboxSetting("Remember Input", mutableMapOf("Text" to false, "Mode" to false))
        .withDescription("Toggles for settings on the input menu that should be restored after reopening it")

    private enum class InputMode { NORMAL, UNDERCUT }

    private var item: ItemStack? = null
    private var mode: InputMode? = null
    private var input = ""

    override fun init() {
        ScreenEvents.AFTER_INIT.register { _, screen, width, height ->
            if (! enabled) return@register
            if (screen !is AbstractSignEditScreen) return@register
            val stack = item ?: return@register
            val sign = (screen as IAbstractSignEditScreen).getSign() ?: return@register
            val lines = Array(4) { i -> sign.frontText.getMessage(i, false).string }

            if (lines[1] == "^^^^^^^^^^^^^^^" && lines[2] == "Your auction" && lines[3] == "starting bid") mc.execute {
                // manually setting the screen so the sign gui wont close
                mc.screen = AuctionInputScreen(sign, lines, stack).apply { init(mc, width, height) }
            }
        }

        register<ContainerEvent.SlotClick> {
            if (event.screen.title.string != "Create BIN Auction") return@register
            if (event.slotId != 31) return@register
            item = event.screen.menu.getSlot(13).item.takeIf { it.skyblockId.isNotEmpty() }
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

            if (rememberInput.value["Text"] != true) input = ""
            mode = if (rememberInput.value["Mode"] == true && mode != null) mode else InputMode.entries[defaultMode.value]

            lowestBin = NoammAddons.priceData[stack.skyblockId] ?: 0L

            val centerX = width / 2
            val centerY = height / 2

            inputField = UISearchBox(centerX - 100, centerY - 20, 200, 22, Component.literal("Price"))
            inputField.value = input
            inputField.setMaxLength(32)
            recalculateValue()
            inputField.moveCursorToEnd(false)
            inputField.setHighlightPos(0)

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
                mode = if (mode == InputMode.NORMAL) InputMode.UNDERCUT else InputMode.NORMAL
                button.message = Component.literal(getModeText())
                recalculateValue()
            })
        }

        private fun getModeText() = "Mode: ${mode !!.name.lowercase().uppercaseFirst()}"

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
                    mc.font, lore, stack.tooltipImage, mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE)
                )
            }

            val headerText = if (mode == InputMode.UNDERCUT) "Lowest BIN: ${NumbersUtils.format(lowestBin)}" else "Set Auction Price"
            guiGraphics.drawCenteredString(font, headerText, centerX, centerY - 50, Color.ORANGE.rgb)

            val displayText = if (parsedValue != null) "§aValue: §e${NumbersUtils.formatComma(parsedValue)}"
            else if (input.isEmpty()) "§7Enter a value (e.g. 10m, 5k)"
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
            val finalLine0 = parsedValue?.toString() ?: input

            ServerboundSignUpdatePacket(
                sign.blockPos, true, finalLine0,
                originalText[1], originalText[2], originalText[3]
            ).send()

            onClose()
        }

        private fun recalculateValue() {
            val textValue = NumbersUtils.parseCompactNumber(input)

            if (textValue == null) {
                parsedValue = null
                return
            }

            parsedValue = if (mode == InputMode.UNDERCUT) (lowestBin - textValue).coerceAtLeast(0)
            else textValue
        }
    }
}