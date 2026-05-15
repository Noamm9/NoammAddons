package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.ContainerFullyOpenedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
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
    internal val enableTooltipInStorage by ToggleSetting("Enable Tooltip Scroll in Storage", true)

    private val storageDir by lazy { File(mc.gameDirectory, "config/${NoammAddons.MOD_NAME}/storage").also(File::mkdirs) }
    private val dataFile by lazy { File(storageDir, "${mc.user.profileId}.nbt") }
    @Volatile internal var storageData = StorageData()

    private var currentHandler: StorageBackingHandle? = null
    private var active: StorageOverlayScreen? = null

    @JvmStatic
    @JvmName("activeFor")
    internal fun activeFor(screen: ContainerScreen) = active?.takeIf { it.containerScreen === screen }

    private val emptyStorageSlotItems = listOf(
        Blocks.RED_STAINED_GLASS_PANE.asItem(),
        Blocks.BROWN_STAINED_GLASS_PANE.asItem(),
        Items.GRAY_DYE
    )

    override fun init() {
        ThreadUtils.addShutdownHook(::saveData)

        register<ContainerFullyOpenedEvent> {
            if (! LocationUtils.inSkyblock) return@register
            val screen = mc.screen as? ContainerScreen ?: return@register
            if (screen.menu.containerId != event.windowId) return@register
            if (screen.title.unformattedText != event.title.unformattedText) return@register
            val handler = StorageBackingHandle.fromScreen(screen) ?: currentHandler ?: return@register
            currentHandler = handler
            rememberContent(handler)
            active?.handler = handler
        }
    }

    @JvmStatic
    fun onScreenChange(oldScreen: Screen?, newScreen: Screen?): Screen? {
        if (! LocationUtils.inSkyblock) return null
        if (oldScreen == null && newScreen == null) return null

        val screen = newScreen as? ContainerScreen
        val overlay = oldScreen as? StorageOverlayScreen ?: active
        val handler = StorageBackingHandle.fromScreen(screen)

        if (currentHandler == null && handler == null) loadData()
        currentHandler?.let { rememberContent(it) }
        handler?.let { rememberContent(it) }
        currentHandler = handler

        if (oldScreen === active?.containerScreen) {
            active?.containerScreen = null
            active?.handler = null
            active = null
        }

        if (newScreen == null && overlay != null && ! overlay.isExiting) return overlay
        if (screen == null) return null
        if (overlay?.isExiting == true) return null
        val currentHandler = currentHandler ?: return null

        active = (overlay ?: StorageOverlayScreen()).also {
            it.containerScreen = screen
            it.handler = currentHandler
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
                    data.remove(slot)
                    changed = true
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
        if (! checkFile()) return
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
        if (! checkFile()) return
        if (! dataFile.exists()) return
        if (storageData.storageInventories.isNotEmpty()) return
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

    private fun checkFile(): Boolean {
        if (! dataFile.isDirectory) return true
        val children = dataFile.listFiles().orEmpty()
        return children.isEmpty() && dataFile.delete()
    }
}
