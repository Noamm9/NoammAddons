package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.ScreenChangeEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.customgui.CustomGui
import com.github.noamm9.ui.customgui.customGui
import com.github.noamm9.ui.customgui.setSlotX
import com.github.noamm9.ui.customgui.setSlotY
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.io.File
import java.util.SortedMap
import java.util.TreeMap

data class StoragePageSlot(val index: Int) : Comparable<StoragePageSlot> {
    val isEnderChest get() = index < 9
    val isBackPack get() = !isEnderChest
    val slotIndexInOverviewPage get() = if (isEnderChest) index + 9 else index + 18

    fun defaultName() = if (isEnderChest) "Ender Chest #${index + 1}" else "Backpack #${index - 9 + 1}"
    fun navigateTo() = ChatUtils.sendCommand(if (isBackPack) "backpack ${index - 9 + 1}" else "enderchest ${index + 1}")

    override fun compareTo(other: StoragePageSlot) = this.index - other.index

    companion object {
        fun fromOverviewSlotIndex(slot: Int) = when (slot) { in 9 until 18 -> StoragePageSlot(slot - 9); in 27 until 45 -> StoragePageSlot(slot - 27 + 9); else -> null }
        fun ofEnderChestPage(slot: Int) = StoragePageSlot(slot - 1)
        fun ofBackPackPage(slot: Int) = StoragePageSlot(slot - 1 + 9)
    }
}

data class StorageData(val storageInventories: SortedMap<StoragePageSlot, StorageInventory> = TreeMap()) {
    data class StorageInventory(var title: String, val slot: StoragePageSlot, var inventory: VirtualInventory?)
}

sealed interface StorageBackingHandle {
    sealed interface HasBackingScreen { val handler: ChestMenu }

    data class Overview(override val handler: ChestMenu) : StorageBackingHandle, HasBackingScreen
    data class Page(override val handler: ChestMenu, val storagePageSlot: StoragePageSlot) : StorageBackingHandle, HasBackingScreen

    companion object {
        private val enderChestName = "^Ender Chest (?:✦ )?\\(([1-9])/[1-9]\\)$".toRegex()
        private val backPackName = "^.+Backpack (?:✦ )?\\(Slot #([0-9]+)\\)$".toRegex()

        fun fromScreen(screen: Screen?): StorageBackingHandle? {
            if (screen == null || screen !is ContainerScreen) return null
            val title = screen.title.unformattedText
            if (title == "Storage") return Overview(screen.menu)
            enderChestName.matchEntire(title)?.let { return Page(screen.menu, StoragePageSlot.ofEnderChestPage(it.groupValues[1].toInt())) }
            backPackName.matchEntire(title)?.let { return Page(screen.menu, StoragePageSlot.ofBackPackPage(it.groupValues[1].toInt())) }
            return null
        }
    }
}

class StorageOverlayCustom(
    val handler: StorageBackingHandle,
    val screen: ContainerScreen,
    val overview: StorageOverlayScreen,
) : CustomGui() {
    override fun onVoluntaryExit(): Boolean {
        overview.isExiting = true
        StorageOverlayScreen.resetScroll()
        return super.onVoluntaryExit()
    }

    override fun onInit() {
        overview.init(mc, screen.width, screen.height)
        val accessor = screen as IAbstractContainerScreen
        accessor.setLeftPos(overview.measurements.x)
        accessor.setTopPos(overview.measurements.y)
        accessor.setImageWidth(overview.measurements.totalWidth)
        accessor.setImageHeight(overview.measurements.totalHeight)
    }

    override fun shouldDrawForeground() = false

    override fun mouseClick(click: MouseButtonEvent, doubled: Boolean) = overview.mouseClicked(click, doubled, (handler as? StorageBackingHandle.Page)?.storagePageSlot)
    override fun mouseReleased(click: MouseButtonEvent) = overview.mouseReleased(click)
    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double) = overview.mouseDragged(click, deltaX, deltaY)
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double) = overview.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)

    override fun render(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.fill(0, 0, screen.width, screen.height, Color(0, 0, 0, 200).rgb)
        Render2D.drawRect(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(24, 24, 27))
        Render2D.drawBorder(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(60, 60, 65))
        val activeSlot = (handler as? StorageBackingHandle.Page)?.storagePageSlot
        val chestSlots = screen.menu.slots.take(screen.menu.rowCount * 9).drop(9)
        overview.drawPages(context, mouseX, mouseY, delta, activeSlot, chestSlots)
        overview.drawScrollBar(context)
        overview.drawPlayerInventory(context, mouseX, mouseY)
    }

    override fun moveSlot(slot: Slot) {
        if (slot.container is Inventory) {
            val (x, y) = overview.getPlayerInventorySlotPosition(slot.containerSlot)
            val accessor = screen as IAbstractContainerScreen
            slot.setSlotX(x - accessor.leftPos)
            slot.setSlotY(y - accessor.topPos)
        } else {
            slot.setSlotX(-100000)
            slot.setSlotY(-100000)
        }
    }

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double) = false
}

@AlwaysActive
object StorageOverlay : Feature(
    description = "Shows all storage pages in an overlay when opening storage.",
    name = "Storage Overlay",
    toggled = true,
) {
    private val columnsSetting by SliderSetting("Columns", 3, 1, 10, 1)
    private val maxHeightSetting by SliderSetting("Max Height", 324, 80, 600, 1)
    private val scrollSpeedSetting by SliderSetting("Scroll Speed", 10, 1, 50, 1)
    private val inverseScrollSetting by ToggleSetting("Inverse Scroll", false)
    private val retainScrollSetting by ToggleSetting("Retain Scroll", true)

    val columns get() = columnsSetting.value
    val maxHeight get() = maxHeightSetting.value
    val retainScroll get() = retainScrollSetting.value

    fun adjustScrollSpeed(amount: Double) = amount * scrollSpeedSetting.value * (if (inverseScrollSetting.value) 1 else -1)

    var storageData = StorageData()
        private set

    var currentHandler: StorageBackingHandle? = null
        private set

    val emptyStorageSlotItems = listOf(
        Blocks.RED_STAINED_GLASS_PANE.asItem(),
        Blocks.BROWN_STAINED_GLASS_PANE.asItem(),
        Items.GRAY_DYE
    )

    private val dataDir = File("config/${NoammAddons.MOD_NAME}/storage")
    private var dirty = false
    private var currentPlayerUUID: String? = null

    @Volatile private var handlingScreenChange = false

    override fun init() {
        register<ScreenChangeEvent> { if (enabled) onScreenChange(event) }
        register<WorldChangeEvent> { ensureDataLoaded() }
        register<TickEvent.Start> {
            if (!enabled) return@register
            currentHandler?.let { rememberContent(it) }
        }
        ThreadUtils.addShutdownHook { saveData() }
    }

    private fun onScreenChange(event: ScreenChangeEvent) {
        if (handlingScreenChange) return
        handlingScreenChange = true
        try {
            onScreenChangeInner(event)
        } finally {
            handlingScreenChange = false
        }
    }

    private fun onScreenChangeInner(event: ScreenChangeEvent) {
        if (event.old == null && event.new == null) return
        ensureDataLoaded()

        val storageOverlayScreen = event.old as? StorageOverlayScreen ?: ((event.old as? AbstractContainerScreen<*>)?.customGui as? StorageOverlayCustom)?.overview
        val screen = event.new as? ContainerScreen

        rememberContent(currentHandler)
        currentHandler = StorageBackingHandle.fromScreen(screen)

        if (event.new == null && storageOverlayScreen != null && !storageOverlayScreen.isExiting) {
            event.overrideScreen = storageOverlayScreen
            return
        }

        screen ?: return
        if (storageOverlayScreen?.isExiting == true) return

        screen.customGui = StorageOverlayCustom(currentHandler ?: return, screen, storageOverlayScreen ?: StorageOverlayScreen())
    }

    fun rememberContent(handler: StorageBackingHandle?) {
        handler ?: return
        val data = storageData.storageInventories
        when (handler) {
            is StorageBackingHandle.Overview -> rememberStorageOverview(handler, data)
            is StorageBackingHandle.Page -> rememberPage(handler, data)
        }
    }

    private fun rememberStorageOverview(handler: StorageBackingHandle.Overview, data: SortedMap<StoragePageSlot, StorageData.StorageInventory>) {
        var changed = false
        for ((index, stack) in handler.handler.slots.map { it.item }.withIndex()) {
            if (stack.isEmpty) continue
            val slot = StoragePageSlot.fromOverviewSlotIndex(index) ?: continue
            val isEmpty = stack.item in emptyStorageSlotItems
            if (slot in data) {
                if (isEmpty) { data.remove(slot); changed = true }
                continue
            }
            if (!isEmpty) {
                data[slot] = StorageData.StorageInventory(slot.defaultName(), slot, null)
                changed = true
            }
        }
        if (changed) markDirty()
    }

    private fun rememberPage(handler: StorageBackingHandle.Page, data: SortedMap<StoragePageSlot, StorageData.StorageInventory>) {
        if (handler.storagePageSlot !in data) {
            data[handler.storagePageSlot] = StorageData.StorageInventory(handler.storagePageSlot.defaultName(), handler.storagePageSlot, null)
            markDirty()
        }

        val items = handler.handler.slots.map { it.item }
        val chestItems = items.take(handler.handler.rowCount * 9).drop(9)
        if (chestItems.isEmpty()) return

        val newStacks = VirtualInventory(chestItems.map { it.copy() })
        data.compute(handler.storagePageSlot) { slot, existing ->
            (existing ?: StorageData.StorageInventory(slot.defaultName(), slot, null)).also { it.inventory = newStacks }
        }
        markDirty()
    }

    private fun ensureDataLoaded() {
        val uuid = mc.player?.uuid?.toString() ?: return
        if (uuid != currentPlayerUUID) {
            saveData()
            currentPlayerUUID = uuid
            loadData()
        }
    }

    private fun getDataFile(): File {
        dataDir.mkdirs()
        return File(dataDir, "${currentPlayerUUID ?: "unknown"}.nbt")
    }

    private fun markDirty() {
        dirty = true
        ThreadUtils.async { saveData() }
    }

    @Synchronized
    private fun saveData() {
        if (!dirty) return
        dirty = false
        try {
            val file = getDataFile()
            val root = CompoundTag()
            for ((slot, inv) in storageData.storageInventories) {
                val prefix = slot.index.toString()
                root.putString("${prefix}_title", inv.title)
                inv.inventory?.let { root.putString("${prefix}_inv", it.serialize()) }
            }
            NbtIo.writeCompressed(root, file.toPath())
        } catch (e: Exception) {
            NoammAddons.logger.error("Failed to save storage data", e)
        }
    }

    @Synchronized
    private fun loadData() {
        storageData = StorageData()
        try {
            val file = getDataFile()
            if (!file.exists()) {
                val oldFile = File("config/${NoammAddons.MOD_NAME}/storage_data.nbt")
                if (oldFile.exists()) {
                    oldFile.copyTo(file, overwrite = false)
                    oldFile.delete()
                }
            }
            if (!file.exists()) return
            val root = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap())
            val data = StorageData()
            for (i in 0 until 27) {
                val titleKey = "${i}_title"
                val invKey = "${i}_inv"
                if (!root.contains(titleKey)) continue
                val title = root.getString(titleKey).orElse("")
                if (title.isEmpty()) continue
                val slot = StoragePageSlot(i)
                val inventory = if (root.contains(invKey)) VirtualInventory.deserialize(root.getString(invKey).orElse("")) else null
                data.storageInventories[slot] = StorageData.StorageInventory(title, slot, inventory)
            }
            storageData = data
        } catch (e: Exception) {
            NoammAddons.logger.error("Failed to load storage data", e)
        }
    }
}
