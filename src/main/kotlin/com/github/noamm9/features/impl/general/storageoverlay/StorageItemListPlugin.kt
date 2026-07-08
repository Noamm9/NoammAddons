package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.operationpotato.itemlist.api.ExcludedScreensManager
import com.operationpotato.itemlist.api.Plugin
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import java.util.Optional

/**
 * Hides the SkyBlock Item List panel while our storage overlay owns the container screen.
 * Without this the item list reads our full-width imageWidth, hides itself and persists
 * its enabled flag to false on close.
 *
 * @see resources/fabric.mod.json5
 */
@Suppress("unused")
class StorageItemListPlugin: Plugin {
    override fun registerExcludedScreens(excludedScreensManager: ExcludedScreensManager) {
        excludedScreensManager.addProvider(ContainerScreen::class.java) { screen ->
            if (StorageOverlay.activeFor(screen) != null) Optional.of(NoammAddons.MOD_NAME)
            else Optional.empty()
        }
    }
}
