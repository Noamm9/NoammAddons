package com.github.noamm9.init

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.items.ItemUtils.idToNameMap
import com.github.noamm9.utils.items.ItemUtils.nameToIdMap
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.utils.network.data.ElectionData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import java.util.concurrent.*

object NetworkLoop {
    private const val ELECTION_URL = "https://api.hypixel.net/v2/resources/skyblock/election"
    private const val ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items"
    private const val BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar"
    private const val LOWESTBINS_URL = "https://lb.tricked.dev/lowestbins"

    @JvmField val priceData = ConcurrentHashMap<String, Long>()
    @JvmField var electionData = ElectionData.empty

    fun init() = ThreadUtils.loop(TimeUnit.MINUTES.toMillis(10)) {
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
        priceData.putAll(data.mapValues { it.value.toLong() })
    }.onFailure { logError("lowest bins", it) }

    private suspend fun updateBazaarPrices() = runCatching {
        val data = WebUtils.getAs<JsonObject>(BAZAAR_URL).getOrThrow()
        data["products"]?.jsonObject?.forEach { (key, element) ->
            val product = element.jsonObject
            val productId = product["product_id"]?.jsonPrimitive?.content ?: key
            val buyPrice = product["buy_summary"]?.jsonArray?.getOrNull(0)
                ?.jsonObject?.get("pricePerUnit")?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L

            priceData[productId] = buyPrice
        }
    }.onFailure { logError("bazaar prices", it) }

    private suspend fun updateSkyblockItems() = runCatching {
        val data = WebUtils.getAs<JsonObject>(ITEMS_URL).getOrThrow()
        val itemsArray = data["items"]?.jsonArray ?: return@runCatching
        for (element in itemsArray) {
            val item = element.jsonObject
            val id = item["id"]?.jsonPrimitive?.content ?: continue
            val name = item["name"]?.jsonPrimitive?.content ?: continue

            idToNameMap[id] = name
            nameToIdMap[name] = id
        }
    }.onFailure { logError("Skyblock items", it) }

    private fun logError(context: String, throwable: Throwable) {
        logger.error("Error fetching $context", throwable)
        throwable.printStackTrace()
    }
}