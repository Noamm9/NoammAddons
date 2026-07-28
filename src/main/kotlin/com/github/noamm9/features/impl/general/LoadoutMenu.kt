package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
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
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object LoadoutMenu: Feature("Replaces the Loadouts inventory with a custom loadout wheel."), ICustomMenu {
    private val menuScale by SliderSetting("Wheel Scale", 100, 70, 135, 5, "%").section("Settings")
    private val closeAfterUse by ToggleSetting("Auto Close On Use")
    private val showKeyLabels by ToggleSetting("Show Key Labels", true)
    private val equipSound by ToggleSetting("Equip Sound")
    private val equipSoundSettings = createSoundSettings("Equip Sound Type", SoundEvents.NOTE_BLOCK_PLING.value()) { equipSound.value }

    private val segmentColor by ColorSetting("Segment Color", Color(15, 15, 15, 200)).section("Colors")
    private val hoverColor by ColorSetting("Hover Color", Color(255, 255, 255, 30))
    private val separatorColor by ColorSetting("Separator Color", Color(255, 255, 255, 40))

    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds").section("Keybinds")
    private val keybinds = (1 .. LOADOUTS_PER_WHEEL).mapIndexed { index, slot ->
        KeybindSetting("Loadout Slot $slot", InputConstants.KEY_1 + index)
            .hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    private const val LOADOUTS_PER_WHEEL = 6

    private val loadoutMenuRegex = Regex("""^\(\d+/\d+\) Loadouts$""")
    private val lockedLoadoutRegex = Regex("""^Loadout \d+ Locked$""")
    private val loadoutSlotIndices = listOf(
        14, 15, 16,
        23, 24, 25,
        32, 33, 34,
        41, 42, 43
    )

    private val wheel = WheelMenu(
        pageSize = LOADOUTS_PER_WHEEL,
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
        isActive = ::isActiveLoadout,
        title = { page, pages -> "Loadouts $page/$pages" },
        renderEntry = ::drawLoadoutInSegment,
        renderCenter = ::drawCenter,
        finishContentRender = ItemRenderer::endItemRendererBatch
    )

    private var lastContainerId = - 1
    private var lastClickAt = 0L
    private var tempDisabled = false
    private var loadoutMenuOpen = false
    private var pendingAutoClose = false

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket) {
                loadoutMenuOpen = event.packet.title.unformattedText.matches(loadoutMenuRegex)
            }
            else if (event.packet is ClientboundContainerClosePacket && loadoutMenuOpen) {
                loadoutMenuOpen = false
            }
        }

        register<PacketEvent.Sent> {
            if (event.packet !is ServerboundContainerClosePacket || ! loadoutMenuOpen) return@register
            loadoutMenuOpen = false
            pendingAutoClose = false
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(loadoutMenuRegex)) return@register

            pendingAutoClose = false
            player.closeContainer()
        }

        register<ScreenEvent.PreRender> {
            val screen = event.screen as? AbstractContainerScreen<*> ?: return@register
            if (! inLoadoutMenu(screen)) return@register
            event.cancel()

            if (screen.menu.containerId != lastContainerId) {
                lastContainerId = screen.menu.containerId
                wheel.reset()
            }

            wheel.onRender(event.context, loadoutSlots(screen), event.mouseX, event.mouseY)
            renderVanillaButton(event.context, event.mouseX, event.mouseY)
        }

        register<ContainerEvent.MouseClick> {
            if (! inLoadoutMenu(event.screen)) return@register

            if (vanillaButtonHovered(event.mouseX, event.mouseY)) {
                event.cancel()
                tempDisabled = true
                return@register
            }

            event.cancel()

            val loadout = wheel.onClick(
                loadoutSlots(event.screen),
                event.mouseX,
                event.mouseY,
                event.button,
                event.modifiers
            )?.entry

            if (handleKeybind(event.screen, event.button, mouse = true)) return@register
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && loadout != null) equip(event.screen, loadout.index)
        }

        register<ContainerEvent.Keyboard> {
            if (! inLoadoutMenu(event.screen)) return@register
            if (handleKeybind(event.screen, event.key, mouse = false)) event.cancel()
        }

        register<ContainerEvent.MouseScroll> {
            if (! inLoadoutMenu(event.screen) || event.verticalAmount == 0.0) return@register
            event.cancel()
            wheel.onScroll(loadoutSlots(event.screen).size, event.verticalAmount)
        }

        register<ContainerEvent.SlotClick> {
            if (! isLoadoutMenu(event.screen) || ! equipSound.value) return@register
            if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.clickType != ContainerInput.PICKUP) return@register
            if (! isSlotEquipable(event.slotId)) return@register
            equipSoundSettings.action.invoke()
        }

        register<ContainerEvent.Close> {
            if (! event.screen.title.unformattedText.matches(loadoutMenuRegex)) return@register
            lastContainerId = - 1
            wheel.reset()
            tempDisabled = false
        }
    }

    private fun renderVanillaButton(context: GuiGraphicsExtractor, vanillaMouseX: Int, vanillaMouseY: Int) {
        Resolution.push(context)
        try {
            val bounds = vanillaButtonBounds()
            val hovered = vanillaButtonHovered(vanillaMouseX.toDouble(), vanillaMouseY.toDouble())
            context.drawRect(bounds.x, bounds.y, bounds.width, bounds.height, if (hovered) hoverColor.value else segmentColor.value)
            context.drawCenteredString("Vanilla Menu", bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f - 3.5f)
        }
        finally {
            Resolution.pop(context)
        }
    }

    private fun drawLoadoutInSegment(context: GuiGraphicsExtractor, render: WheelMenu.EntryRender<Slot>) {
        val layout = render.layout
        val angle = render.angle
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        val itemRadius = (layout.innerRadius + layout.outerRadius) / 2f
        val itemScale = 1.5f * 1.5f * if (render.hovered) 1.12f else 1f
        drawCenteredItem(
            context,
            render.entry.item,
            layout.centerX + cosAngle * itemRadius,
            layout.centerY + sinAngle * itemRadius,
            itemScale
        )

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
            context.drawRect(keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, Color(15, 15, 15, 200))
            if (render.hovered) context.drawRect(keyX - badgeWidth / 2f, keyY - 6, badgeWidth, 12, hoverColor.value)
            context.drawCenteredString(keyName, keyX, keyY - 3.5f, scale = textScale)
        }
    }

    private fun drawCenter(context: GuiGraphicsExtractor, render: WheelMenu.CenterRender<Slot>) {
        val item = render.entry.item
        val layout = render.layout
        val contentScale = (layout.innerRadius / 54f).coerceIn(0.78f, 1.12f)
        val availableTextWidth = (layout.innerRadius * 2f - 14f).coerceAtLeast(36f)
        val centerIconScale = min(1.65f * 1.5f, layout.innerRadius * 0.62f / 16f)
        drawCenteredItem(context, item, layout.centerX, layout.centerY - 14f * contentScale, centerIconScale)

        fun drawText(text: String, y: Float, color: Color, maxScale: Float) = context.drawCenteredString(
            text,
            layout.centerX,
            y,
            color,
            min(maxScale * contentScale, availableTextWidth / text.width().coerceAtLeast(1))
        )

        val nameY = layout.centerY + 3f * contentScale
        drawText(item.hoverName.formattedText, nameY, Color.WHITE, 0.78f)
        if (render.active) drawText("ACTIVE", nameY + 10f * contentScale, Color.GREEN, 0.6f)
    }

    private fun handleKeybind(screen: AbstractContainerScreen<*>, code: Int, mouse: Boolean): Boolean {
        val index = if (useHotbarBinds.value) {
            if (mouse) return false
            mc.options.keyHotbarSlots.take(LOADOUTS_PER_WHEEL).withIndex().find {
                (it.value as IKeyMapping).key.value == code
            }?.index ?: - 1
        }
        else keybinds.indexOfFirst { it.matches(code, mouse) }

        if (index < 0) return false
        wheel.entryAt(loadoutSlots(screen), index)?.let { equip(screen, it.index) }
        return true
    }

    private fun equip(screen: AbstractContainerScreen<*>, slotIndex: Int) {
        if (player.containerMenu !== screen.menu || ! isSlotEquipable(slotIndex)) return

        val now = System.currentTimeMillis()
        if (now - lastClickAt < 300) return

        lastClickAt = now
        GuiUtils.clickSlot(slotIndex, GuiUtils.ButtonType.LEFT)
        if (closeAfterUse.value) closeAfterReopen()
    }

    private fun closeAfterReopen() {
        player.closeContainer()
        ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
        pendingAutoClose = true
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
            y = Resolution.height - 23f,
            width = width,
            height = 18f
        )
    }

    private fun drawCenteredItem(context: GuiGraphicsExtractor, item: ItemStack, x: Float, y: Float, scale: Float) {
        ItemRenderer.drawBatchedItemStack(context, item, (x - 8f).roundToInt(), (y - 8f).roundToInt(), scale)
    }

    private fun loadoutSlots(screen: AbstractContainerScreen<*>) = loadoutSlotIndices.mapNotNull { index ->
        screen.menu.slots.getOrNull(index)?.takeIf {
            ! it.item.isEmpty && (isSlotEquipable(it) || isActiveLoadout(it))
        }
    }

    private fun isSlotEquipable(slotIndex: Int) =
        player.containerMenu.slots.getOrNull(slotIndex)?.let(::isSlotEquipable) == true

    private fun isSlotEquipable(slot: Slot) =
        slot.index in loadoutSlotIndices && slot.item.lore.any {
            it.contains("Left-click to equip!", ignoreCase = true)
        }

    private fun isActiveLoadout(slot: Slot): Boolean {
        if (slot.index !in loadoutSlotIndices || slot.item.hoverName.unformattedText.matches(lockedLoadoutRegex)) return false
        return slot.item.lore.none { it.contains("Left-click to equip!", ignoreCase = true) } &&
                slot.item.lore.none { it.contains("You must customize this loadout", ignoreCase = true) }
    }

    private fun inLoadoutMenu(screen: AbstractContainerScreen<*>) = ! tempDisabled && isLoadoutMenu(screen)
    private fun isLoadoutMenu(screen: AbstractContainerScreen<*>) =
        screen.title.unformattedText.matches(loadoutMenuRegex)

    override fun isActive() = lastContainerId != - 1

    private data class ButtonBounds(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )
}
