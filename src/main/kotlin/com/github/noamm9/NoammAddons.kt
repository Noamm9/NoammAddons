package com.github.noamm9

import com.github.noamm9.commands.CommandManager
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventDispatcher
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.init.NetworkLoop
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.network.data.ElectionData
import com.github.noamm9.utils.render.NoammRenderPipelines
import com.github.noamm9.websocket.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.owdding.dfu.item.MeowddingItemDfu
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory
import java.util.concurrent.*

object NoammAddons: ClientModInitializer {
    const val MOD_NAME = "NoammAddons"
    const val MOD_ID = "noammaddons"
    val MOD_VERSION get() = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version.friendlyString
    const val PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"

    @JvmField
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

    var electionData = ElectionData.empty
    val priceData = ConcurrentHashMap<String, Long>()


    override fun onInitializeClient() {
        DataDownloader.downloadData()

        NoammRenderPipelines.init()
        EventDispatcher.init()
        DungeonListener.init()
        ServerUtils.init()
        ActionBarParser.init()
        PartyUtils.init()
        ChatUtils.init()
        MeowddingItemDfu.load()
        TestGround()

        NetworkLoop.init()

        FeatureManager.registerFeatures()
        CommandManager.registerAll()
        WebSocket.init()

        EventBus.register<TickEvent.Start> {
            mc.execute {
                if (screen == null) return@execute
                mc.setScreen(screen)
                screen = null
            }
        }

        isLoaded = true
    }
}