package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ThreadUtils
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.io.File
import java.util.*
import kotlin.jvm.optionals.getOrNull

object StorageOverlay: Feature("Shows all storage pages in an overlay when opening storage.", toggled = true) {
    internal val columnsSetting by SliderSetting("Columns", 3, 1, 10, 1)
    internal val maxHeightSetting by SliderSetting("Max Height", 324, 80, 600, 1)
    internal val scrollSpeedSetting by SliderSetting("Scroll Speed", 10, 1, 50, 1)
    internal val inverseScrollSetting by ToggleSetting("Inverse Scroll", false)
    internal val retainScrollSetting by ToggleSetting("Retain Scroll", true)
    internal val lockScrollOnActiveSetting by ToggleSetting("Lock Scroll on Active", false)

    private val dataFile = File("config/${NoammAddons.MOD_NAME}/storage/${mc.user.profileId}.nbt").also { it.mkdirs() }
    private var currentHandler: StorageBackingHandle? = null
    internal var storageData = StorageData()

    internal var active: StorageOverlayScreen? = null

    @JvmStatic @JvmName("activeFor")
    internal fun activeFor(screen: ContainerScreen) = active?.takeIf { it.containerScreen === screen }

    private val emptyStorageSlotItems = listOf(
        Blocks.RED_STAINED_GLASS_PANE.asItem(),
        Blocks.BROWN_STAINED_GLASS_PANE.asItem(),
        Items.GRAY_DYE
    )

    override fun init() {
        ThreadUtils.addShutdownHook(::saveData)
        loadData()
    }

    @JvmStatic
    fun onScreenChange(oldScreen: Screen?, newScreen: Screen?): Screen? {
        if (oldScreen == null && newScreen == null) return null

        val screen = newScreen as? ContainerScreen
        val overlay = oldScreen as? StorageOverlayScreen ?: active

        currentHandler?.let { rememberContent(it) }
        currentHandler = StorageBackingHandle.fromScreen(screen)

        if (oldScreen === active?.containerScreen) {
            active?.handler = null
            active?.containerScreen = null
            active = null
        }

        if (newScreen == null && overlay != null && ! overlay.isExiting) return overlay
        if (screen == null) return null
        if (overlay?.isExiting == true) return null
        val handler = currentHandler ?: return null

        active = (overlay ?: StorageOverlayScreen()).also {
            it.handler = handler
            it.containerScreen = screen
        }

        return null
    }

    private fun rememberContent(handler: StorageBackingHandle) {
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
                if (isEmpty) {
                    data.remove(slot); changed = true
                }
                continue
            }
            if (! isEmpty) {
                data[slot] = StorageData.StorageInventory(slot.defaultName(), slot, null)
                changed = true
            }
        }
        if (changed) ThreadUtils.async(::saveData)
    }

    private fun rememberPage(handler: StorageBackingHandle.Page, data: SortedMap<StoragePageSlot, StorageData.StorageInventory>) {
        if (handler.storagePageSlot !in data) {
            data[handler.storagePageSlot] = StorageData.StorageInventory(handler.storagePageSlot.defaultName(), handler.storagePageSlot, null)
            ThreadUtils.async(::saveData)
        }

        val items = handler.handler.slots.map { it.item }
        val chestItems = items.take(handler.handler.rowCount * 9).drop(9)
        if (chestItems.isEmpty()) return

        val newStacks = VirtualInventory(chestItems.map { it.copy() })
        data.compute(handler.storagePageSlot) { slot, existing ->
            (existing ?: StorageData.StorageInventory(slot.defaultName(), slot, null)).also { it.inventory = newStacks }
        }

        ThreadUtils.async(::saveData)
    }

    @Synchronized
    private fun saveData() {
        val root = CompoundTag()
        for ((slot, inv) in storageData.storageInventories) {
            val prefix = slot.index.toString()
            root.putString("${prefix}_title", inv.title)
            inv.inventory?.let { root.putString("${prefix}_inv", it.encode()) }
        }
        NbtIo.writeCompressed(root, dataFile.toPath())
    }

    @Synchronized
    private fun loadData() {
        if (! dataFile.exists()) return
        val root = NbtIo.readCompressed(dataFile.toPath(), NbtAccounter.unlimitedHeap()) ?: return
        val data = StorageData()

        for (i in 0 until 27) {
            val titleKey = "${i}_title"
            val invKey = "${i}_inv"
            if (! root.contains(titleKey)) continue
            val title = (root.getString(titleKey).getOrNull() ?: "").ifEmpty { continue }

            val slot = StoragePageSlot(i)
            val inventory = if (root.contains(invKey)) VirtualInventory.decode(root.getString(invKey).getOrNull() ?: "") else null
            data.storageInventories[slot] = StorageData.StorageInventory(title, slot, inventory)
        }

        storageData = data
    }
}
