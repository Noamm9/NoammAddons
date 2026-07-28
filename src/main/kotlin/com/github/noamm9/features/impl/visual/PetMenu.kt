package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.WheelMenu
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.*

object PetMenu: Feature("Replaces the Pets inventory with a custom pet wheel."), ICustomMenu {
    private val menuScale by SliderSetting("Wheel Scale", 100, 70, 135, 5, "%").section("Settings")
    private val showKeyLabels by ToggleSetting("Show Key Labels", true)
    private val favouritePetsOnly by ToggleSetting("Favourite Pets Only").withDescription("Only shows pets favourited in Hypixel's Pets Menu.")

    private val segmentColor by ColorSetting("Segment Color", Color(15, 15, 15, 200)).section("Colors")
    private val hoverColor by ColorSetting("Hover Color", Color(255, 255, 255, 30))
    private val separatorColor by ColorSetting("Separator Color", Color(255, 255, 255, 40))

    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds").section("Keybinds")
    private val keybinds = (1 .. PETS_PER_WHEEL).mapIndexed { index, slot ->
        KeybindSetting("Pet Slot $slot", InputConstants.KEY_1 + index)
            .hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    private val petMenuRegex = Regex("^(?:\\(\\d+/\\d+\\) )?Pets(?: \\(\\d+/\\d+\\))?$", RegexOption.IGNORE_CASE)
    private val petSlots = (10 .. 43).filter { it % 9 in 1 .. 7 }

    private const val PETS_PER_WHEEL = 9

    private val wheel = WheelMenu(
        pageSize = PETS_PER_WHEEL,
        scale = { menuScale.value / 100f },
        style = {
            WheelMenu.Style(
                segmentColor = segmentColor.value,
                hoverColor = hoverColor.value,
                activeColor = ClickGui.accentColor.value,
                separatorColor = separatorColor.value,
                backgroundColor = Color.BLACK.withAlpha(150)
            )
        },
        isActive = ::isActivePet,
        title = { page, pages -> "Pets $page/$pages" },
        renderEntry = ::drawPetInSegment,
        renderCenter = ::drawCenter,
        finishContentRender = ItemRenderer::endItemRendererBatch
    )

    private var lastContainerId = - 1
    private var lastClickAt = 0L
    private var tempDisabled = false

    override fun init() {
        register<ScreenEvent.PreRender> {
            val screen = event.screen as? AbstractContainerScreen<*> ?: return@register
            if (! inPetMenu(screen)) return@register
            event.cancel()

            if (screen.menu.containerId != lastContainerId) {
                lastContainerId = screen.menu.containerId
                wheel.reset()
            }

            wheel.onRender(event.context, petSlots(screen), event.mouseX, event.mouseY)
            renderVanillaButton(event.context, event.mouseX, event.mouseY)
        }

        register<ContainerEvent.MouseClick> {
            if (! inPetMenu(event.screen)) return@register

            if (vanillaButtonHovered(event.mouseX, event.mouseY)) {
                event.cancel()
                tempDisabled = true
                return@register
            }

            event.cancel()

            val pets = petSlots(event.screen)
            val pet = wheel.onClick(pets, event.mouseX, event.mouseY, event.button, event.modifiers)?.entry

            if (event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && InputConstants.isKeyDown(mc.window, InputConstants.KEY_LSHIFT) && pet != null) {
                val now = System.currentTimeMillis()
                if (now - lastClickAt >= 300) {
                    lastClickAt = now
                    GuiUtils.clickSlot(pet.index, GuiUtils.ButtonType.LEFT, shift = true)
                }
                return@register
            }

            if (handleKeybind(event.screen, event.button, mouse = true)) return@register
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && pet != null) click(event.screen, pet.index)
        }

        register<ContainerEvent.Keyboard> {
            if (! inPetMenu(event.screen)) return@register
            if (handleKeybind(event.screen, event.key, mouse = false)) event.cancel()
        }

        register<ContainerEvent.MouseScroll> {
            if (! inPetMenu(event.screen) || event.verticalAmount == 0.0) return@register
            event.cancel()

            wheel.onScroll(petSlots(event.screen).size, event.verticalAmount)
        }

        register<ContainerEvent.Close> {
            if (! event.screen.title.unformattedText.matches(petMenuRegex)) return@register
            lastContainerId = - 1
            wheel.reset()
            tempDisabled = false
        }
    }

    private fun renderVanillaButton(ctx: GuiGraphicsExtractor, vanillaMouseX: Int, vanillaMouseY: Int) {
        Resolution.push(ctx)
        try {
            val bounds = vanillaButtonBounds()
            val hovered = vanillaButtonHovered(vanillaMouseX.toDouble(), vanillaMouseY.toDouble())
            ctx.drawRect(bounds.x, bounds.y, bounds.width, bounds.height, if (hovered) hoverColor.value else segmentColor.value)
            ctx.drawCenteredString("Vanilla Menu", bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f - 3.5f)
        }
        finally {
            Resolution.pop(ctx)
        }
    }

    private fun drawPetInSegment(ctx: GuiGraphicsExtractor, render: WheelMenu.EntryRender<Slot>) {
        val layout = render.layout
        val angle = render.angle
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        val itemRadius = (layout.innerRadius + layout.outerRadius) / 2f
        val itemScale = 1.5f * 1.5f * if (render.hovered) 1.12f else 1f
        drawCenteredItem(ctx, render.entry.item, layout.centerX + cosAngle * itemRadius, layout.centerY + sinAngle * itemRadius, itemScale)

        if (showKeyLabels.value) {
            val keyName = run {
                if (useHotbarBinds.value) {
                    val keybind = mc.options.keyHotbarSlots.getOrNull(render.index) as? IKeyMapping
                    keybind?.key?.displayName?.string?.uppercase()
                }
                else keybinds.getOrNull(render.index)?.displayName()
            } ?: return

            val keyRadius = layout.outerRadius - 12f
            val keyX = layout.centerX + cosAngle * keyRadius
            val keyY = layout.centerY + sinAngle * keyRadius
            val keyWidth = keyName.width()
            val textScale = min(0.68f, 24f / keyWidth.coerceAtLeast(1))
            val badgeWidth = (keyWidth * textScale + 8f).coerceAtLeast(12f)
            ctx.drawRect(keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, Color(15, 15, 15, 200))
            if (render.hovered) ctx.drawRect(keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, hoverColor.value)
            ctx.drawCenteredString(keyName, keyX, keyY - 3.5f, scale = textScale)
        }
    }

    private fun drawCenter(ctx: GuiGraphicsExtractor, render: WheelMenu.CenterRender<Slot>) {
        val selected = render.entry
        val layout = render.layout
        val contentScale = (layout.innerRadius / 54f).coerceIn(0.78f, 1.12f)
        val availableTextWidth = (layout.innerRadius * 2f - 14f).coerceAtLeast(36f)
        val centerIconScale = min(1.65f * 1.5f, layout.innerRadius * 0.62f / 16f)
        drawCenteredItem(ctx, selected.item, layout.centerX, layout.centerY - 14f * contentScale, centerIconScale)

        fun drawText(text: String, y: Float, color: Color, maxScale: Float) = ctx.drawCenteredString(
            text, layout.centerX, y, color, min(maxScale * contentScale, availableTextWidth / text.width().coerceAtLeast(1))
        )

        val nameY = layout.centerY + 3f * contentScale
        drawText(selected.item.hoverName.formattedText, nameY, Color.WHITE, 0.78f)

        val heldItemY = nameY + 9.5f * contentScale
        val heldItem = selected.item.lore.firstNotNullOfOrNull {
            it.substringAfter("Held Item:", "").trim().takeIf(String::isNotEmpty)
        } ?: "None"
        drawText("§fPet Item: $heldItem", heldItemY, Color.WHITE, 0.62f)
        if (render.active) drawText("ACTIVE", heldItemY + 10f * contentScale, Color.GREEN, 0.6f)
    }

    private fun handleKeybind(screen: AbstractContainerScreen<*>, code: Int, mouse: Boolean): Boolean {
        val index = if (useHotbarBinds.value) {
            if (mouse) return false
            mc.options.keyHotbarSlots.take(PETS_PER_WHEEL).withIndex().find {
                (it.value as IKeyMapping).key.value == code
            }?.index ?: - 1
        }
        else keybinds.indexOfFirst { it.matches(code, mouse) }

        if (index < 0) return false

        wheel.entryAt(petSlots(screen), index)?.let {
            click(screen, it.index)
        }

        return true
    }

    private fun click(screen: AbstractContainerScreen<*>, slotIndex: Int) {
        if (player.containerMenu !== screen.menu) return
        val slot = screen.menu.slots.getOrNull(slotIndex) ?: return
        if (slot.item.isEmpty || slot.index != slotIndex) return
        val now = System.currentTimeMillis()
        if (now - lastClickAt < 300) return

        lastClickAt = now
        GuiUtils.clickSlot(slotIndex, GuiUtils.ButtonType.LEFT)
        player.closeContainer()
    }

    private fun vanillaButtonHovered(mouseX: Double, mouseY: Double): Boolean {
        val bounds = vanillaButtonBounds()
        val x = Resolution.getMouseX(mouseX)
        val y = Resolution.getMouseY(mouseY)
        return x >= bounds.x && x <= bounds.x + bounds.width && y >= bounds.y && y <= bounds.y + bounds.height
    }

    private fun vanillaButtonBounds(): ButtonBounds {
        val width = "Vanilla Menu".width() + 16f
        return ButtonBounds(
            x = Resolution.width - width - 5f,
            y = Resolution.height - 13f,
            width = width,
            height = 18f
        )
    }

    private fun drawCenteredItem(ctx: GuiGraphicsExtractor, item: ItemStack, x: Float, y: Float, scale: Float) {
        ItemRenderer.drawBatchedItemStack(ctx, item, (x - 8f).roundToInt(), (y - 8f).roundToInt(), scale)
    }

    private fun petSlots(screen: AbstractContainerScreen<*>) = petSlots.mapNotNull { index ->
        screen.menu.slots.getOrNull(index)?.takeIf {
            val isHead = ! it.item.isEmpty && it.item.`is`(Items.PLAYER_HEAD)
            val favorite = it.item.hoverName.unformattedText.startsWith("⭐ ")
            isHead && if (favouritePetsOnly.value) favorite else true
        }
    }

    private fun isActivePet(slot: Slot) = slot.item.lore.any {
        it.removeFormatting().contains("Click to despawn!", ignoreCase = true)
    }

    private fun inPetMenu(screen: AbstractContainerScreen<*>) = ! tempDisabled && screen.title.unformattedText.matches(petMenuRegex)
    override fun isActive() = lastContainerId != - 1

    private data class ButtonBounds(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )
}