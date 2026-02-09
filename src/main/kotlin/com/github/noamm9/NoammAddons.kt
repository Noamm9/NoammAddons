package com.github.noamm9

import com.github.noamm9.commands.CommandManager
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventDispatcher
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.utils.network.data.ElectionData
import com.github.noamm9.utils.render.RoundedRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

object NoammAddons: ClientModInitializer {
    const val MOD_NAME = "NoammAddons"
    const val PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
    const val MOD_ID = "noammaddons"

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmField
    val mc = Minecraft.getInstance()
    val logger = LoggerFactory.getLogger(MOD_NAME)

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())
    val debugFlags = mutableSetOf<String>()
    var screen: Screen? = null

    var electionData = ElectionData.empty

    override fun onInitializeClient() {
        DataDownloader.downloadData()

        EventDispatcher.init()
        ThreadUtils.init()
        DungeonListener.init()
        ServerUtils.init()
        ActionBarParser.init()
        PartyUtils.init()
        ChatUtils.init()
        TestGround()

        this.initNetworkLoop()

        FeatureManager.registerFeatures()
        CommandManager.registerAll()

        SpecialGuiElementRegistry.register { buffer -> RoundedRect(buffer.vertexConsumers()) }

        EventBus.register<TickEvent.Start> {
            mc.execute {
                if (screen == null) return@execute
                mc.setScreen(screen)
                screen = null
            }
        }
    }

    private fun initNetworkLoop() = ThreadUtils.loop(600_000) {
        WebUtils.get<ElectionData>("https://api.noammaddons.workers.dev/mayor").onSuccess {
            electionData = it
        }
    }
}