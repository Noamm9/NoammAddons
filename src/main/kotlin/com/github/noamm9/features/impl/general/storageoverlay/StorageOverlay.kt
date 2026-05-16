package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.ContainerFullyOpenedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.misc.ScrollableTooltip
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.utils.network.data.StorageData
import kotlinx.coroutines.runBlocking
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
    val columnsSetting by SliderSetting("Columns", 3, 1, 10, 1).withDescription("The number of pages to show next to each other horizontally")
    val maxHeightSetting by SliderSetting("Max Height", 324, 80, 600, 1).withDescription("the maximum height of the entire menu")
    val scrollSpeedSetting by SliderSetting("Scroll Speed", 10, 1, 50, 1).withDescription("how fast you scroll")
    val retainScrollSetting by ToggleSetting("Retain Scroll", true).withDescription("Whether to it keep the scroll offset after closing the menu")
    val enableTooltipInStorage by ToggleSetting("Tooltip Scroll").withDescription("Whether to enable Item Tooltip Scrolling. (requires ${ScrollableTooltip.name} to be enabled)")

    private val storageDir by lazy { File(mc.gameDirectory, "config/${NoammAddons.MOD_NAME}/storage").also(File::mkdirs) }
    private val dataFile get() = File(storageDir, "${mc.user.profileId}.nbt")
    @Volatile var storageMenuData = StorageMenuData()

    private var currentHandler: StorageMenu? = null
    private var active: StorageOverlayScreen? = null

    @JvmStatic
    fun activeFor(screen: ContainerScreen) = active?.takeIf { it.containerScreen === screen }

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
            val handler = currentHandler ?: return@register
            saveContent(handler)
        }
    }

    @JvmStatic
    fun onScreenChange(oldScreen: Screen?, newScreen: Screen?): Screen? {
        if (! LocationUtils.inSkyblock) return null
        if (oldScreen == null && newScreen == null) return null

        val screen = newScreen as? ContainerScreen
        val overlay = oldScreen as? StorageOverlayScreen ?: active
        val handler = StorageMenu.fromScreen(screen)

        if (currentHandler == null && handler == null) loadData()
        currentHandler?.let { saveContent(it) }
        handler?.let { saveContent(it) }
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

    private fun saveContent(handler: StorageMenu) = when (handler) {
        is StorageMenu.Overview -> saveStorageOverview(handler, storageMenuData.storageInventories)
        is StorageMenu.Page -> savePage(handler, storageMenuData.storageInventories)
    }

    private fun saveStorageOverview(handler: StorageMenu.Overview, data: SortedMap<StoragePage, StorageMenuData.StorageInventory>) {
        var changed = false
        for ((index, stack) in handler.handler.slots.map { it.item }.withIndex()) {
            if (stack.isEmpty) continue
            val slot = StoragePage.fromOverviewSlotIndex(index) ?: continue
            val isEmpty = stack.item in emptyStorageSlotItems
            if (slot in data) {
                if (isEmpty) {
                    data.remove(slot)
                    changed = true
                }
                continue
            }
            if (! isEmpty) {
                data[slot] = StorageMenuData.StorageInventory(slot.defaultName(), slot, null)
                changed = true
            }
        }
        if (changed) ThreadUtils.async(::saveData)
    }

    private fun savePage(handler: StorageMenu.Page, data: SortedMap<StoragePage, StorageMenuData.StorageInventory>) {
        val slot = handler.storagePage
        val gui = handler.handler

        val end = (gui.rowCount * 9).takeIf { it > 9 } ?: return
        val chestItems = gui.slots.subList(9, end).map { it.item.copy() }
        if (chestItems.isEmpty()) return

        data.getOrPut(slot) {
            StorageMenuData.StorageInventory(slot.defaultName(), slot, null)
        }.inventory = NBTInventory(chestItems)

        ThreadUtils.async(::saveData)
    }

    @Synchronized
    private fun saveData() {
        val file = dataFile
        if (! checkFile(file)) return
        val root = CompoundTag()
        for ((slot, inv) in storageMenuData.storageInventories) {
            val prefix = slot.index.toString()
            root.putString("${prefix}_title", inv.title)
            inv.inventory?.let { root.putString("${prefix}_inv", it.encode()) }
        }

        NbtIo.writeCompressed(root, file.toPath())
    }

    @Synchronized
    private fun loadData() {
        val file = dataFile
        if (! checkFile(file)) return
        if (storageMenuData.storageInventories.isNotEmpty()) return
        if (! file.exists()) return ThreadUtils.async(::loadFromApi)
        val root = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap()) ?: return
        val data = StorageMenuData()

        for (i in 0 until 27) {
            val titleKey = "${i}_title"
            val invKey = "${i}_inv"
            if (! root.contains(titleKey)) continue
            val title = (root.getString(titleKey).getOrNull() ?: "").ifEmpty { continue }

            val slot = StoragePage(i)
            val inventory = if (root.contains(invKey)) NBTInventory.decode(root.getString(invKey).getOrNull() ?: "") else null
            data.storageInventories[slot] = StorageMenuData.StorageInventory(title, slot, inventory)
        }

        storageMenuData = data
    }

    private fun checkFile(file: File): Boolean {
        if (! file.isDirectory) return true
        val children = file.listFiles().orEmpty()
        return children.isEmpty() && file.delete()
    }

    private fun loadFromApi() = runBlocking {
        WebUtils.getAs<StorageData>("https://api.noamm.org/hypixel/storage/${mc.user.profileId}").onSuccess {
            val data = StorageMenuData()

            it.enderchest.forEach { (i, stacks) ->
                val slot = StoragePage(i)
                val title = slot.defaultName()
                val inventory = NBTInventory(stacks)
                data.storageInventories[slot] = StorageMenuData.StorageInventory(title, slot, inventory)
            }

            it.backpack.forEach { (i, stacks) ->
                val slot = StoragePage(i + 9)
                val title = slot.defaultName()
                val inventory = NBTInventory(stacks)
                data.storageInventories[slot] = StorageMenuData.StorageInventory(title, slot, inventory)
            }

            storageMenuData = data
        }
    }
}