package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.render.GuiShapeRenderer
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

object CustomPetMenu: Feature(
    description = "Replaces the Pets inventory with a custom pet wheel.",
    name = "Custom Pet Menu"
) {
    private val menuScale by SliderSetting("Wheel Scale", 100, 70, 135, 5, "%").section("Settings")
    private val centerSize by SliderSetting("Center Size", 39, 28, 55, 1, "%")
    private val iconScale by SliderSetting("Pet Icon Scale", 100, 70, 150, 5, "%")
    private val showKeyLabels by ToggleSetting("Show Key Labels", true)
    private val clickSound by ToggleSetting("Click Sound", false)
    private val clickSoundSettings = createSoundSettings("Click Sound Type", SoundEvents.UI_BUTTON_CLICK.value()) { clickSound.value }
    private val closeAfterUse by ToggleSetting("Auto Close On Use")

    private val backdropColor by ColorSetting("Backdrop Color", Color(0, 0, 0, 150)).section("Colors")
    private val segmentColor by ColorSetting("Segment Color", Color(15, 15, 15, 200))
    private val activeColor by ColorSetting("Active Color", Color(99, 176, 217, 100))
    private val hoverColor by ColorSetting("Hover Color", Color(255, 255, 255, 30))
    private val separatorColor by ColorSetting("Separator Color", Color(255, 255, 255, 40))

    private val bind1 by KeybindSetting("Wheel Slot 1", GLFW.GLFW_KEY_1).section("Keybinds")
    private val keybinds = listOf(bind1) + (2 .. 8).map { slot ->
        KeybindSetting("Wheel Slot $slot", GLFW.GLFW_KEY_1 + slot - 1).apply(configSettings::add)
    }
    private val autopetRulesBind by KeybindSetting("Autopet Rules", InputConstants.UNKNOWN.value)

    private val petMenuRegex = Regex("^(?:\\(\\d+/\\d+\\) )?Pets(?: \\(\\d+/\\d+\\))?$", RegexOption.IGNORE_CASE)
    private val petSlotIndices = (10 .. 43).filter { it % 9 in 1 .. 7 }

    private const val PETS_PER_WHEEL = 8
    private const val SEGMENT_ANGLE = PI * 2.0 / PETS_PER_WHEEL

    private var wheelPage = 0
    private var lastContainerId = - 1
    private var lastClickAt = 0L
    private var pendingAutoClose = false

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
            event.cancel()

            if (handleKeybind(event.screen, event.button, mouse = true)) return@register
            if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return@register

            val pets = petSlots(event.screen)
            val layout = wheelLayout()
            val index = hoveredWheelIndex(event.mouseX, event.mouseY, layout) ?: return@register
            val pet = pets.getOrNull(wheelPage * PETS_PER_WHEEL + index) ?: return@register
            click(event.screen, pet.index, autoClose = true)
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
            if (clickSound.value) clickSoundSettings.play.action.invoke()
        }

        register<ContainerEvent.Close> {
            if (! inPetMenu(event.screen)) return@register
            lastContainerId = - 1
            wheelPage = 0
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.string.removeFormatting().matches(petMenuRegex)) return@register

            pendingAutoClose = false
            mc.player?.closeContainer()
        }
    }

    private fun renderWheel(ctx: GuiGraphicsExtractor, screen: AbstractContainerScreen<*>, vanillaMouseX: Int, vanillaMouseY: Int) {
        Resolution.refresh()
        Resolution.push(ctx)

        val pets = petSlots(screen)
        val pages = pageCount(pets.size)
        wheelPage = wheelPage.coerceIn(0, pages - 1)
        val visiblePets = pets.drop(wheelPage * PETS_PER_WHEEL).take(PETS_PER_WHEEL)
        val layout = wheelLayout()
        val hoveredIndex = hoveredWheelIndex(vanillaMouseX.toDouble(), vanillaMouseY.toDouble(), layout)
            ?.takeIf { it in visiblePets.indices }
        val activePet = visiblePets.firstOrNull { slot ->
            slot.item.lore.any { it.removeFormatting().contains("Click to despawn!", ignoreCase = true) }
        }
        val selectedPet = hoveredIndex?.let(visiblePets::get)
            ?: activePet
            ?: visiblePets.firstOrNull()

        Render2D.drawRect(ctx, 0, 0, Resolution.width, Resolution.height, backdropColor.value)

        repeat(PETS_PER_WHEEL) { index ->
            val pet = visiblePets.getOrNull(index)
            drawRingSegment(ctx, layout, index, segmentColor.value)
            if (pet === activePet) drawRingSegment(ctx, layout, index, activeColor.value)
            if (index == hoveredIndex) drawRingSegment(ctx, layout, index, hoverColor.value)
        }

        drawSegmentSeparators(ctx, layout)

        visiblePets.forEachIndexed { index, pet ->
            drawPetInSegment(ctx, pet, index, layout, index == hoveredIndex)
        }

        drawCenter(ctx, selectedPet, selectedPet === activePet, layout)
        Render2D.drawCenteredString(
            ctx,
            "Pets ${wheelPage + 1}/$pages",
            layout.centerX,
            layout.centerY - layout.outerRadius - 19f,
            Color.WHITE,
            0.85f
        )

        Resolution.pop(ctx)
    }

    private fun drawRingSegment(ctx: GuiGraphicsExtractor, layout: WheelLayout, index: Int, color: Color) {
        val centerAngle = - PI / 2.0 + index * SEGMENT_ANGLE
        GuiShapeRenderer.drawAnnularSegment(
            ctx, layout.centerX, layout.centerY, layout.innerRadius, layout.outerRadius,
            centerAngle - SEGMENT_ANGLE / 2.0, centerAngle + SEGMENT_ANGLE / 2.0, color
        )
    }

    private fun drawSegmentSeparators(ctx: GuiGraphicsExtractor, layout: WheelLayout) {
        repeat(PETS_PER_WHEEL) { index ->
            val angle = - PI / 2.0 - SEGMENT_ANGLE / 2.0 + index * SEGMENT_ANGLE
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
        val angle = - PI / 2.0 + index * SEGMENT_ANGLE
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        val itemRadius = (layout.innerRadius + layout.outerRadius) / 2f
        val itemScale = 1.5f * iconScale.value / 100f * if (hovered) 1.12f else 1f
        drawCenteredItem(ctx, slot.item, layout.centerX + cosAngle * itemRadius, layout.centerY + sinAngle * itemRadius, itemScale)

        if (showKeyLabels.value) {
            val keyRadius = layout.outerRadius - 12f
            val keyX = layout.centerX + cosAngle * keyRadius
            val keyY = layout.centerY + sinAngle * keyRadius
            val keyName = keybinds[index].displayName()
            val keyWidth = keyName.width()
            val textScale = min(0.68f, 24f / keyWidth.coerceAtLeast(1))
            val badgeWidth = (keyWidth * textScale + 8f).coerceAtLeast(12f)
            Render2D.drawRect(ctx, keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, Color(15, 15, 15, 200))
            if (hovered) Render2D.drawRect(ctx, keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, hoverColor.value)
            Render2D.drawCenteredString(ctx, keyName, keyX, keyY - 3.5f, Color.WHITE, textScale)
        }
    }

    private fun drawCenteredItem(ctx: GuiGraphicsExtractor, item: ItemStack, x: Float, y: Float, scale: Float) {
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(x - 8f * scale, y - 8f * scale)
        pose.scale(scale, scale)
        ctx.item(item, 0, 0)
        pose.popMatrix()
    }

    private fun drawCenter(ctx: GuiGraphicsExtractor, selected: Slot?, active: Boolean, layout: WheelLayout) {
        if (selected == null) {
            Render2D.drawCenteredString(ctx, "NO PETS", layout.centerX, layout.centerY - 5, Color.WHITE, 0.8f)
            return
        }

        val contentScale = (layout.innerRadius / 54f).coerceIn(0.78f, 1.12f)
        val availableTextWidth = (layout.innerRadius * 2f - 14f).coerceAtLeast(36f)
        val centerIconScale = min(1.65f * iconScale.value / 100f, layout.innerRadius * 0.62f / 16f)
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

    private fun petSlots(screen: AbstractContainerScreen<*>): List<Slot> = petSlotIndices.mapNotNull { index ->
        screen.menu.slots.getOrNull(index)?.takeIf { ! it.item.isEmpty && it.item.`is`(Items.PLAYER_HEAD) }
    }

    private fun handleKeybind(screen: AbstractContainerScreen<*>, code: Int, mouse: Boolean): Boolean {
        if (autopetRulesBind.matches(code, mouse)) {
            click(screen, 46)
            return true
        }

        val index = keybinds.indexOfFirst { it.matches(code, mouse) }
        if (index < 0) return false
        petSlots(screen).getOrNull(wheelPage * PETS_PER_WHEEL + index)
            ?.let { click(screen, it.index, autoClose = true) }
        return true
    }

    private fun KeybindSetting.matches(code: Int, mouse: Boolean) =
        value != InputConstants.UNKNOWN.value && isMouse == mouse && value == code

    private fun click(screen: AbstractContainerScreen<*>, slotIndex: Int, autoClose: Boolean = false) {
        if (mc.player?.containerMenu !== screen.menu) return
        val slot = screen.menu.slots.getOrNull(slotIndex) ?: return
        if (slot.item.isEmpty || slot.index != slotIndex) return
        val now = System.currentTimeMillis()
        if (now - lastClickAt < 300) return

        lastClickAt = now
        GuiUtils.clickSlot(slotIndex, GuiUtils.ButtonType.LEFT)
        if (clickSound.value) clickSoundSettings.play.action.invoke()
        if (autoClose && closeAfterUse.value) {
            mc.player?.closeContainer()
            ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
            pendingAutoClose = true
        }
    }

    private fun inPetMenu(screen: AbstractContainerScreen<*>) =
        enabled && screen.title.string.removeFormatting().matches(petMenuRegex)

    private fun pageCount(petCount: Int) = Math.ceilDiv(petCount, PETS_PER_WHEEL).coerceAtLeast(1)

    private fun wheelLayout(): WheelLayout {
        val desiredRadius = 138f * (menuScale.value / 100f)
        val maxRadius = min((Resolution.height - 96f) / 2f, (Resolution.width - 220f) / 2f).coerceAtLeast(82f)
        val outerRadius = min(desiredRadius, maxRadius)
        return WheelLayout(
            centerX = Resolution.width / 2f - 18f,
            centerY = Resolution.height / 2f - 8f,
            innerRadius = outerRadius * (centerSize.value / 100f),
            outerRadius = outerRadius
        )
    }

    private fun hoveredWheelIndex(mouseX: Double, mouseY: Double, layout: WheelLayout): Int? {
        val x = Resolution.getMouseX(mouseX) - layout.centerX
        val y = Resolution.getMouseY(mouseY) - layout.centerY
        if (hypot(x.toDouble(), y.toDouble()) <= layout.innerRadius) return null
        return Math.floorMod(
            floor(((atan2(y, x) + PI / 2.0 + SEGMENT_ANGLE / 2.0) / SEGMENT_ANGLE)).toInt(),
            PETS_PER_WHEEL
        )
    }

    private data class WheelLayout(
        val centerX: Float,
        val centerY: Float,
        val innerRadius: Float,
        val outerRadius: Float
    )
}
