package com.github.noamm9.init

import com.github.noamm9.NoammAddons
import com.github.noamm9.ui.gui.ICustomMenu
import com.github.noamm9.utils.catch
import io.github.classgraph.ClassGraph
import net.fabricmc.loader.api.FabricLoader

object ModCompatibility {
    private val customMenus = buildList {
        val scan = ClassGraph()
            .enableAllInfo()
            .acceptPackages(NoammAddons::class.java.`package`.name)
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        scan.use {
            it.getSubclasses(ICustomMenu::class.qualifiedName).forEach { ci ->
                val i = catch { ci.loadClass().getDeclaredField("INSTANCE").get(null) as? ICustomMenu }
                i?.let(::add)
            }
        }
    }

    @JvmStatic fun isModLoaded(modid: String) = FabricLoader.getInstance().isModLoaded(modid)
    @JvmStatic fun isCustomMenuActive() = customMenus.any(ICustomMenu::isActive)

    fun disableBlockstateCulling() = catch {
        if (! isModLoaded("moreculling")) return@catch
        val main = Class.forName("ca.fxco.moreculling.MoreCulling")
        val config = main.getDeclaredField("CONFIG").get(null)

        val blockStateCulling = config?.javaClass?.getDeclaredField("useBlockStateCulling")
        blockStateCulling?.isAccessible = true
        blockStateCulling?.setBoolean(config, false)
    }
}