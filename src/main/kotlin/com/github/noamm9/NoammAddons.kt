package com.github.noamm9

import com.github.noamm9.commands.CommandManager
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventDispatcher
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.utils.network.data.ElectionData
import com.github.noamm9.websocket.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

object NoammAddons: ClientModInitializer {
    const val MOD_NAME = "NoammAddons"
    const val MOD_ID = "noammaddons"
    val MOD_VERSION get() = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version.friendlyString
    const val PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
    const val BASE_URL = "https://api.noamm.org"

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
    var priceData = mutableMapOf<String, Long>()


    override fun onInitializeClient() {
        DataDownloader.downloadData()

        EventDispatcher.init()
        DungeonListener.init()
        ServerUtils.init()
        ActionBarParser.init()
        PartyUtils.init()
        ChatUtils.init()
        ItemUtils.init()
        TestGround()

        this.initNetworkLoop()

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

    private fun initNetworkLoop() = ThreadUtils.loop(600_000) {
        runCatching {
            val data = WebUtils.getAs<JsonObject>("${BASE_URL}/mayor").getOrThrow()
            val mayor = data["mayor"]?.jsonObject !!
            val minister = mayor["minister"]?.jsonObject
            val perks = mayor["perks"]?.jsonArray
                ?.map { it.jsonObject["name"]?.jsonPrimitive?.content to it.jsonObject["description"]?.jsonPrimitive?.content?.removeFormatting() }
                ?.map { ElectionData.Perk(it.first !!, it.second !!) }
                ?: return@runCatching

            electionData = ElectionData(
                ElectionData.Mayor(
                    mayor["name"]?.jsonPrimitive?.content !!,
                    perks
                ),
                ElectionData.Minister(
                    minister?.get("name")?.jsonPrimitive?.content.orEmpty(),
                    ElectionData.Perk(minister?.get("perk")?.jsonObject["name"]?.jsonPrimitive?.content.orEmpty(), minister?.get("perk")?.jsonObject["description"]?.jsonPrimitive?.content?.removeFormatting().orEmpty())
                )
            )
        }.onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }

        runCatching {
            priceData.putAll(WebUtils.getAs<Map<String, Long>>("${BASE_URL}/lowestbin").getOrThrow())
        }.onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }

        runCatching {
            val data = WebUtils.getAs<JsonObject>("${BASE_URL}/bazaar").getOrThrow()
            data["products"]?.jsonObject?.forEach { (key, element) ->
                val product = element.jsonObject
                val productId = product["product_id"]?.jsonPrimitive?.content ?: key
                val buyPrice = product["buy_summary"]?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("pricePerUnit")?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L

                priceData[productId] = buyPrice
            }
        }.onFailure {
            logger.error("Error while making a web request", it)
            it.printStackTrace()
        }
    }
}