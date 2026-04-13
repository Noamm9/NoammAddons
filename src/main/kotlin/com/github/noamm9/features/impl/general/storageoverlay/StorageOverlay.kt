package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.ScreenChangeEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.customgui.customGui
import com.github.noamm9.utils.ThreadUtils
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.io.File
import java.util.SortedMap

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

    private val dataDir = File("config/${NoammAddons.MOD_NAME}/storage")
    private var dirty = false
    private var currentPlayerUUID: String? = null

    var currentHandler: StorageBackingHandle? = null
        private set

    val emptyStorageSlotItems = listOf(
        Blocks.RED_STAINED_GLASS_PANE.asItem(),
        Blocks.BROWN_STAINED_GLASS_PANE.asItem(),
        Items.GRAY_DYE
    )

    override fun init() {
        register<ScreenChangeEvent> {
            if (!enabled) return@register
            onScreenChange(event)
        }

        register<WorldChangeEvent> {
            ensureDataLoaded()
        }

        register<TickEvent.Start> {
            if (!enabled) return@register
            val handler = currentHandler ?: return@register
            rememberContent(handler)
        }

        ThreadUtils.addShutdownHook { saveData() }
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
        val uuid = currentPlayerUUID ?: "unknown"
        return File(dataDir, "${uuid}.nbt")
    }

    @Volatile private var handlingScreenChange = false

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
        val oldHandler = currentHandler
        currentHandler = StorageBackingHandle.fromScreen(screen)

        if (event.new == null && storageOverlayScreen != null && !storageOverlayScreen.isExiting) {
            event.overrideScreen = storageOverlayScreen
            return
        }

        screen ?: return
        if (storageOverlayScreen?.isExiting == true) return

        screen.customGui = StorageOverlayCustom(
            currentHandler ?: return,
            screen,
            storageOverlayScreen ?: StorageOverlayScreen()
        )
    }

    fun rememberContent(handler: StorageBackingHandle?) {
        handler ?: return
        val data = storageData.storageInventories
        when (handler) {
            is StorageBackingHandle.Overview -> rememberStorageOverview(handler, data)
            is StorageBackingHandle.Page -> rememberPage(handler, data)
        }
    }

    private fun rememberStorageOverview(
        handler: StorageBackingHandle.Overview,
        data: SortedMap<StoragePageSlot, StorageData.StorageInventory>
    ) {
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

    private fun rememberPage(
        handler: StorageBackingHandle.Page,
        data: SortedMap<StoragePageSlot, StorageData.StorageInventory>
    ) {
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
                inv.inventory?.let { virtualInv ->
                    root.putString("${prefix}_inv", virtualInv.serialize())
                }
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
                val inventory = if (root.contains(invKey)) {
                    VirtualInventory.deserialize(root.getString(invKey).orElse(""))
                } else null
                data.storageInventories[slot] = StorageData.StorageInventory(title, slot, inventory)
            }
            storageData = data
        } catch (e: Exception) {
            NoammAddons.logger.error("Failed to load storage data", e)
        }
    }
}
