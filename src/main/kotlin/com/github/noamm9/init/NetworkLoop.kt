package com.github.noamm9.init

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.utils.network.data.ElectionData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import java.util.concurrent.*

object NetworkLoop: ISelfInit {
    private const val ELECTION_URL = "https://api.hypixel.net/v2/resources/skyblock/election"
    private const val ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items"
    private const val BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar"
    private const val LOWESTBINS_URL = "https://lb.tricked.dev/lowestbins"

    private val bazaarPrices = ConcurrentHashMap<String, BazaarPrice>()
    private val lowestBinPrices = ConcurrentHashMap<String, Long>()
    private val npcSellPrices = ConcurrentHashMap<String, Long>()

    @JvmField var electionData = ElectionData.empty
    @JvmField val nameToIdMap = ConcurrentHashMap<String, String>()

    fun getNpcSellPrice(itemId: String) = npcSellPrices[itemId]
    fun getLowestBin(itemId: String) = lowestBinPrices[itemId]
    fun getBazaarPrice(itemId: String) = bazaarPrices[itemId]
    fun getPrice(itemId: String) = bazaarPrices[itemId]?.sell ?: lowestBinPrices[itemId]

    override fun init() = ThreadUtils.loop(TimeUnit.MINUTES.toMillis(10)) {
        coroutineScope {
            val jobs = listOf(
                async { updateElectionData() },
                async { updateLowestBins() },
                async { updateBazaarPrices() },
                async { updateSkyblockItems() }
            )

            jobs.awaitAll()
        }
    }

    private suspend fun updateElectionData() = runCatching {
        val data = WebUtils.getAs<JsonObject>(ELECTION_URL).getOrThrow()
        val mayor = data["mayor"]?.jsonObject ?: return@runCatching
        val minister = mayor["minister"]?.jsonObject
        val perks = mayor["perks"]?.jsonArray?.mapNotNull { element ->
            val obj = element.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val desc = obj["description"]?.jsonPrimitive?.content?.removeFormatting() ?: return@mapNotNull null
            ElectionData.Perk(name, desc)
        } ?: return@runCatching

        electionData = ElectionData(
            ElectionData.Mayor(mayor["name"]?.jsonPrimitive?.content.orEmpty(), perks),
            ElectionData.Minister(
                minister?.get("name")?.jsonPrimitive?.content.orEmpty(),
                ElectionData.Perk(
                    minister?.get("perk")?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty(),
                    minister?.get("perk")?.jsonObject?.get("description")?.jsonPrimitive?.content?.removeFormatting().orEmpty()
                )
            )
        )
    }.onFailure { logError("election data", it) }

    private suspend fun updateLowestBins() = runCatching {
        val data = WebUtils.getAs<Map<String, Double>>(LOWESTBINS_URL).getOrThrow()
        lowestBinPrices.putAll(data.mapValues { it.value.toLong() })
    }.onFailure { logError("lowest bins", it) }

    private suspend fun updateBazaarPrices() = runCatching {
        WebUtils.getAs<JsonObject>(BAZAAR_URL).getOrThrow()["products"]?.jsonObject?.forEach { (key, element) ->
            val product = element.jsonObject
            val productId = product["product_id"]?.jsonPrimitive?.content ?: key
            val sellPrice = product["buy_summary"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("pricePerUnit")?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L
            val buyPrice = product["sell_summary"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("pricePerUnit")?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L

            bazaarPrices[productId] = BazaarPrice(buyPrice, sellPrice)
        }

    }.onFailure { logError("bazaar prices", it) }

    private suspend fun updateSkyblockItems() = runCatching {
        val data = WebUtils.getAs<JsonObject>(ITEMS_URL).getOrThrow()
        val itemsArray = data["items"]?.jsonArray ?: return@runCatching
        for (element in itemsArray) {
            val item = element.jsonObject
            val id = item["id"]?.jsonPrimitive?.content ?: continue
            val name = item["name"]?.jsonPrimitive?.content ?: continue
            val npcPrice = item["npc_sell_price"]?.jsonPrimitive?.longOrNull

            nameToIdMap[name] = id
            npcPrice?.let { npcSellPrices[id.replace(':', '-')] = it }
        }
    }.onFailure { logError("Skyblock items", it) }

    private fun logError(context: String, throwable: Throwable) {
        logger.error("Error fetching $context", throwable)
        throwable.printStackTrace()
    }

    data class BazaarPrice(val buy: Long, val sell: Long)
}