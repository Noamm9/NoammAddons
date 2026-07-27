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
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.render.GuiShapeRenderer
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
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

    private var wheelPage = 0
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
                wheelPage = 0
            }

            renderWheel(event.context, screen, event.mouseX, event.mouseY)
        }

        register<ContainerEvent.MouseClick> {
            if (! inPetMenu(event.screen)) return@register

            val buttonText = "Vanilla Menu"
            val buttonWidth = buttonText.width() + 16f
            val buttonHeight = 18f
            val buttonX = Resolution.width - buttonWidth - 5f
            val buttonY = Resolution.height - buttonHeight - 5f

            val rx = Resolution.getMouseX(event.mouseX)
            val ry = Resolution.getMouseY(event.mouseY)
            if (rx >= buttonX && rx <= buttonX + buttonWidth && ry >= buttonY && ry <= buttonY + buttonHeight) {
                event.cancel()
                tempDisabled = true
                return@register
            }

            event.cancel()

            val visiblePets = petsOnCurrentPage(petSlots(event.screen))
            val layout = wheelLayout(visiblePets.size)
            val pet = hoveredWheelIndex(event.mouseX, event.mouseY, layout)?.let(visiblePets::getOrNull)

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

            val pages = pageCount(petSlots(event.screen).size)
            if (pages <= 1) return@register
            wheelPage = Math.floorMod(wheelPage + if (event.verticalAmount < 0.0) 1 else - 1, pages)
        }

        register<ContainerEvent.Close> {
            if (! event.screen.title.unformattedText.matches(petMenuRegex)) return@register
            lastContainerId = - 1
            wheelPage = 0
            tempDisabled = false
        }
    }

    private fun renderWheel(ctx: GuiGraphicsExtractor, screen: AbstractContainerScreen<*>, vanillaMouseX: Int, vanillaMouseY: Int) {
        Resolution.refresh()
        Resolution.push(ctx)

        val pets = petSlots(screen)
        val pages = pageCount(pets.size)
        wheelPage = wheelPage.coerceIn(0, pages - 1)

        val visiblePets = petsOnCurrentPage(pets)
        val layout = wheelLayout(visiblePets.size)

        val hoveredIndex = hoveredWheelIndex(vanillaMouseX.toDouble(), vanillaMouseY.toDouble(), layout)
        val activePet = visiblePets.firstOrNull { slot -> slot.item.lore.any { it.removeFormatting().contains("Click to despawn!", ignoreCase = true) } }
        val selectedPet = hoveredIndex?.let(visiblePets::get) ?: activePet

        Render2D.drawRect(ctx, 0, 0, Resolution.width, Resolution.height, Color.BLACK.withAlpha(150))

        visiblePets.forEachIndexed { index, pet ->
            drawRingSegment(ctx, layout, index, segmentColor.value)
            if (pet === activePet) drawRingSegment(ctx, layout, index, ClickGui.accentColor.value)
            if (index == hoveredIndex) drawRingSegment(ctx, layout, index, hoverColor.value)
        }

        drawSegmentSeparators(ctx, layout)

        visiblePets.forEachIndexed { index, pet ->
            drawPetInSegment(ctx, pet, index, layout, index == hoveredIndex)
        }
        if (selectedPet != null) drawCenter(ctx, selectedPet, selectedPet === activePet, layout)
        ItemRenderer.endItemRendererBatch(ctx)

        Render2D.drawCenteredString(
            ctx,
            "Pets ${wheelPage + 1}/$pages",
            layout.centerX,
            layout.centerY - layout.outerRadius - 19f,
            Color.WHITE,
            0.85f
        )

        val buttonText = "Vanilla Menu"
        val buttonWidth = buttonText.width() + 16f
        val buttonHeight = 18f
        val buttonX = Resolution.width - buttonWidth - 5f
        val buttonY = Resolution.height - buttonHeight - 5f

        val rx = Resolution.getMouseX(vanillaMouseX.toDouble())
        val ry = Resolution.getMouseY(vanillaMouseY.toDouble())
        val hoveredButton = rx >= buttonX && rx <= buttonX + buttonWidth && ry >= buttonY && ry <= buttonY + buttonHeight

        Render2D.drawRect(ctx, buttonX, buttonY, buttonWidth, buttonHeight, if (hoveredButton) hoverColor.value else segmentColor.value)
        Render2D.drawCenteredString(ctx, buttonText, buttonX + buttonWidth / 2f, buttonY + buttonHeight / 2f - 3.5f)

        Resolution.pop(ctx)
    }

    private fun drawRingSegment(ctx: GuiGraphicsExtractor, layout: WheelLayout, index: Int, color: Color) {
        val centerAngle = - PI / 2.0 + index * layout.segmentAngle
        GuiShapeRenderer.drawAnnularSegment(
            ctx, layout.centerX, layout.centerY, layout.innerRadius, layout.outerRadius,
            centerAngle - layout.segmentAngle / 2.0, centerAngle + layout.segmentAngle / 2.0, color
        )
    }

    private fun drawSegmentSeparators(ctx: GuiGraphicsExtractor, layout: WheelLayout) {
        if (layout.segmentCount <= 1) return
        repeat(layout.segmentCount) { index ->
            val angle = - PI / 2.0 - layout.segmentAngle / 2.0 + index * layout.segmentAngle
            val cosAngle = cos(angle).toFloat()
            val sinAngle = sin(angle).toFloat()
            val innerX = layout.centerX + cosAngle * (layout.innerRadius - 1f)
            val innerY = layout.centerY + sinAngle * (layout.innerRadius - 1f)
            val outerX = layout.centerX + cosAngle * (layout.outerRadius + 1f)
            val outerY = layout.centerY + sinAngle * (layout.outerRadius + 1f)
            Render2D.drawLine(ctx, innerX, innerY, outerX, outerY, separatorColor.value, 2f)
        }
    }

    private fun drawPetInSegment(ctx: GuiGraphicsExtractor, slot: Slot, index: Int, layout: WheelLayout, hovered: Boolean) {
        val angle = - PI / 2.0 + index * layout.segmentAngle
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        val itemRadius = (layout.innerRadius + layout.outerRadius) / 2f
        val itemScale = 1.5f * 1.5f * if (hovered) 1.12f else 1f
        drawCenteredItem(ctx, slot.item, layout.centerX + cosAngle * itemRadius, layout.centerY + sinAngle * itemRadius, itemScale)

        if (showKeyLabels.value) {
            val keyName = run {
                if (useHotbarBinds.value) {
                    val keybind = mc.options.keyHotbarSlots.getOrNull(index) as? IKeyMapping
                    keybind?.key?.displayName?.string?.uppercase()
                }
                else keybinds.getOrNull(index)?.displayName()
            } ?: return

            val keyRadius = layout.outerRadius - 12f
            val keyX = layout.centerX + cosAngle * keyRadius
            val keyY = layout.centerY + sinAngle * keyRadius
            val keyWidth = keyName.width()
            val textScale = min(0.68f, 24f / keyWidth.coerceAtLeast(1))
            val badgeWidth = (keyWidth * textScale + 8f).coerceAtLeast(12f)
            Render2D.drawRect(ctx, keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, Color(15, 15, 15, 200))
            if (hovered) Render2D.drawRect(ctx, keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, hoverColor.value)
            Render2D.drawCenteredString(ctx, keyName, keyX, keyY - 3.5f, Color.WHITE, textScale)
        }
    }

    private fun drawCenter(ctx: GuiGraphicsExtractor, selected: Slot, active: Boolean, layout: WheelLayout) {
        val contentScale = (layout.innerRadius / 54f).coerceIn(0.78f, 1.12f)
        val availableTextWidth = (layout.innerRadius * 2f - 14f).coerceAtLeast(36f)
        val centerIconScale = min(1.65f * 1.5f, layout.innerRadius * 0.62f / 16f)
        drawCenteredItem(ctx, selected.item, layout.centerX, layout.centerY - 14f * contentScale, centerIconScale)

        fun drawText(text: String, y: Float, color: Color, maxScale: Float) = Render2D.drawCenteredString(
            ctx, text, layout.centerX, y, color,
            min(maxScale * contentScale, availableTextWidth / text.width().coerceAtLeast(1))
        )

        val nameY = layout.centerY + 3f * contentScale
        drawText(selected.item.hoverName.formattedText, nameY, Color.WHITE, 0.78f)

        val heldItemY = nameY + 9.5f * contentScale
        val heldItem = selected.item.lore.firstNotNullOfOrNull {
            it.substringAfter("Held Item:", "").trim().takeIf(String::isNotEmpty)
        } ?: "None"
        drawText("§fPet Item: $heldItem", heldItemY, Color.WHITE, 0.62f)
        if (active) drawText("ACTIVE", heldItemY + 10f * contentScale, Color.GREEN, 0.6f)
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

        petSlots(screen).getOrNull(wheelPage * PETS_PER_WHEEL + index)?.let {
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

    private fun wheelLayout(segmentCount: Int): WheelLayout {
        val desiredRadius = 138f * (menuScale.value / 100f)
        val maxRadius = min((Resolution.height - 96f) / 2f, (Resolution.width - 220f) / 2f).coerceAtLeast(82f)
        val outerRadius = min(desiredRadius, maxRadius)
        return WheelLayout(
            centerX = Resolution.width / 2f,
            centerY = Resolution.height / 2f - 8f,
            innerRadius = outerRadius * 0.55f,
            outerRadius = outerRadius,
            segmentCount = segmentCount
        )
    }

    private fun hoveredWheelIndex(mouseX: Double, mouseY: Double, layout: WheelLayout): Int? {
        if (layout.segmentCount == 0) return null
        val x = Resolution.getMouseX(mouseX) - layout.centerX
        val y = Resolution.getMouseY(mouseY) - layout.centerY
        if (hypot(x.toDouble(), y.toDouble()) <= layout.innerRadius) return null
        return Math.floorMod(
            floor(((atan2(y, x) + PI / 2.0 + layout.segmentAngle / 2.0) / layout.segmentAngle)).toInt(),
            layout.segmentCount
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

    private fun petsOnCurrentPage(pets: List<Slot>) = pets.drop(wheelPage * PETS_PER_WHEEL).take(PETS_PER_WHEEL)
    private fun inPetMenu(screen: AbstractContainerScreen<*>) = ! tempDisabled && screen.title.unformattedText.matches(petMenuRegex)
    private fun pageCount(petCount: Int) = Math.ceilDiv(petCount, PETS_PER_WHEEL).coerceAtLeast(1)
    override fun isActive() = lastContainerId != - 1

    private class WheelLayout(
        val centerX: Float,
        val centerY: Float,
        val innerRadius: Float,
        val outerRadius: Float,
        val segmentCount: Int
    ) {
        val segmentAngle = if (segmentCount == 0) 0.0 else PI * 2.0 / segmentCount
    }
}