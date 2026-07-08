package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.operationpotato.itemlist.api.ExcludedScreensManager
import com.operationpotato.itemlist.api.Plugin
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import java.util.Optional

@Suppress("unused")
class StorageItemListPlugin: Plugin {
    override fun registerExcludedScreens(excludedScreensManager: ExcludedScreensManager) {
        excludedScreensManager.addProvider(ContainerScreen::class.java) { screen ->
            Optional.ofNullable(NoammAddons.MOD_NAME.takeIf { StorageOverlay.activeFor(screen) != null })
        }
    }
}
