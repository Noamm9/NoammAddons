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
import com.github.noamm9.utils.network.WebUtils
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
import com.github.noamm9.utils.network.data.StorageData as ApiStorageData

object StorageOverlay: Feature("Shows all storage pages in an overlay when opening storage.", toggled = true) {
    internal val columnsSetting by SliderSetting("Columns", 3, 1, 10, 1)
    internal val maxHeightSetting by SliderSetting("Max Height", 324, 80, 600, 1)
    internal val scrollSpeedSetting by SliderSetting("Scroll Speed", 10, 1, 50, 1)
    internal val retainScrollSetting by ToggleSetting("Retain Scroll", true)
    internal val enableTooltipInStorage by ToggleSetting("Tooltip Scroll", true)

    private val storageDir by lazy { File(mc.gameDirectory, "config/${NoammAddons.MOD_NAME}/storage").also(File::mkdirs) }
    private val dataFile by lazy { File(storageDir, "${mc.user.profileId}.nbt") }
    @Volatile internal var storageData = StorageData()

    private var currentHandler: StorageMenu? = null
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
        is StorageMenu.Overview -> saveStorageOverview(handler, storageData.storageInventories)
        is StorageMenu.Page -> savePage(handler, storageData.storageInventories)
    }

    private fun saveStorageOverview(handler: StorageMenu.Overview, data: SortedMap<StoragePage, StorageData.StorageInventory>) {
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
                data[slot] = StorageData.StorageInventory(slot.defaultName(), slot, null)
                changed = true
            }
        }
        if (changed) ThreadUtils.async(::saveData)
    }

    private fun savePage(handler: StorageMenu.Page, data: SortedMap<StoragePage, StorageData.StorageInventory>) {
        val slot = handler.storagePage
        val gui = handler.handler

        val end = (gui.rowCount * 9).takeIf { it > 9 } ?: return
        val chestItems = gui.slots.subList(9, end).map { it.item.copy() }
        if (chestItems.isEmpty()) return

        data.getOrPut(slot) {
            StorageData.StorageInventory(slot.defaultName(), slot, null)
        }.inventory = VirtualInventory(chestItems)

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
        if (storageData.storageInventories.isNotEmpty()) return
        if (! dataFile.exists()) return ThreadUtils.async(::loadFromApi)
        val root = NbtIo.readCompressed(dataFile.toPath(), NbtAccounter.unlimitedHeap()) ?: return
        val data = StorageData()

        for (i in 0 until 27) {
            val titleKey = "${i}_title"
            val invKey = "${i}_inv"
            if (! root.contains(titleKey)) continue
            val title = (root.getString(titleKey).getOrNull() ?: "").ifEmpty { continue }

            val slot = StoragePage(i)
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

    private fun loadFromApi() = runBlocking {
        WebUtils.getAs<ApiStorageData>("https://api.noamm.org/hypixel/storage/${mc.user.profileId}").onSuccess {
            val data = StorageData()

            it.enderchest.forEach { (i, stacks) ->
                val slot = StoragePage(i)
                val inventory = VirtualInventory(stacks)
                data.storageInventories[slot] = StorageData.StorageInventory(slot.defaultName(), slot, inventory)
            }

            it.backpack.forEach { (i, stacks) ->
                val slot = StoragePage(i + 9)
                val inventory = VirtualInventory(stacks)
                data.storageInventories[slot] = StorageData.StorageInventory(slot.defaultName(), slot, inventory)
            }

            storageData = data
        }
    }
}