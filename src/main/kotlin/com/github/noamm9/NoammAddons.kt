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
import com.github.noamm9.utils.render.RoundedRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.*
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

    @JvmField
    var isLoaded = false

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())
    val debugFlags = mutableSetOf<String>()
    var screen: Screen? = null

    var electionData = ElectionData.empty
    var priceData = mutableMapOf<String, Long>()


    override fun onInitializeClient() {
        DataDownloader.downloadData()

        EventDispatcher.init()
        ThreadUtils.init()
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

        SpecialGuiElementRegistry.register { buffer -> RoundedRect(buffer.vertexConsumers()) }

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
        WebUtils.get<JsonObject>("https://api.hypixel.net/v2/resources/skyblock/election")
            .onSuccess { data ->
                val mayor = data["mayor"]?.jsonObject !!
                val minister = mayor["minister"]?.jsonObject !!

                electionData = ElectionData(
                    ElectionData.Mayor(
                        mayor["name"]?.jsonPrimitive?.content !!,
                        mayor["perks"]?.jsonArray?.map { it.jsonObject["name"]?.jsonPrimitive?.content to it.jsonObject["description"]?.jsonPrimitive?.content?.removeFormatting() }?.map { ElectionData.Perk(it.first !!, it.second !!) } ?: return@onSuccess
                    ),
                    ElectionData.Minister(
                        minister["name"]?.jsonPrimitive?.content !!,
                        ElectionData.Perk(minister["perk"]?.jsonObject["name"]?.jsonPrimitive?.content !!, minister["perk"]?.jsonObject["description"]?.jsonPrimitive?.content?.removeFormatting() !!)
                    )
                )
            }
            .onFailure { logger.error("Error while making a web request", it) }

        WebUtils.get<Map<String, Long>>("https://lb.tricked.dev/lowestbins")
            .onSuccess { priceData.putAll(it) }
            .onFailure { logger.error("Error while making a web request", it) }

        WebUtils.get<JsonObject>("https://api.hypixel.net/v2/skyblock/bazaar")
            .onSuccess { data ->
                data["products"]?.jsonObject?.forEach { (key, element) ->
                    val product = element.jsonObject
                    val productId = product["product_id"]?.jsonPrimitive?.content ?: key
                    val buyPrice = product["buy_summary"]?.jsonArray?.getOrNull(0)
                        ?.jsonObject?.get("pricePerUnit")?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L

                    priceData[productId] = buyPrice
                }
            }
            .onFailure { logger.error("Error while making a web request", it) }
    }
}