package com.github.noamm9.init

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.utils.catch
import net.fabricmc.loader.api.FabricLoader

object ModCompatibility {
    val customMenus = mutableListOf<ICustomMenu>()

    @JvmStatic fun isModLoaded(modid: String) = FabricLoader.getInstance().isModLoaded(modid)
    @JvmStatic fun isCustomMenuActive() = customMenus.any(ICustomMenu::isActive)

    fun disableBlockstateCulling() = catch {
        if (! isModLoaded("moreculling")) return@catch
        val main = Class.forName("ca.fxco.moreculling.MoreCulling")
        val config = main.getDeclaredField("CONFIG").get(null)

        val blockStateCulling = config?.javaClass?.getDeclaredField("useBlockStateCulling")
        blockStateCulling?.isAccessible = true
        blockStateCulling?.setBoolean(config, false)
        mc.levelRenderer.allChanged()
    }
}