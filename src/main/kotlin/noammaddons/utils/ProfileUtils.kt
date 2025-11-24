package noammaddons.utils

import kotlinx.serialization.json.*
import net.minecraft.item.ItemStack
import noammaddons.NoammAddons
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.JsonUtils.getArray
import noammaddons.utils.JsonUtils.getBoolean
import noammaddons.utils.JsonUtils.getInt
import noammaddons.utils.JsonUtils.getObj
import noammaddons.utils.JsonUtils.getString
import noammaddons.utils.Utils.remove
import noammaddons.utils.WebUtils.readUrl
import kotlin.math.floor


object ProfileUtils {
    private const val API = "https://api.noammaddons.workers.dev"
    val profileCache = mutableMapOf<String, JsonObject>()
    val uuidCache = mutableMapOf(mc.session.username to mc.session.playerID)

    private val mojangApiList = listOf(
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
        "https://api.mojang.com/users/profiles/minecraft/",
        "https://api.ashcon.app/mojang/v2/user/"
    )

    fun getUUID(_name: String): String? {
        val name = _name.uppercase()
        if (uuidCache.containsKey(name)) return uuidCache[name]

        uuidCache[name] = null
        for (api in mojangApiList) {
            val response = runCatching { readUrl(api + _name) }.getOrNull() ?: continue
            val json = JsonUtils.stringToJson(response)
            val uuid = json.getString("id") ?: json.getString("uuid")
            if (! uuid.isNullOrBlank()) {
                uuidCache[name] = uuid
                return uuid
            }
        }

        return null
    }

    fun getHypixelPlayer(name: String): JsonObject? {
        val uuid = getUUID(name) ?: return null
        val raw = runCatching { readUrl("$API/hypixel/player?uuid=$uuid") }.getOrNull() ?: return null
        return JsonUtils.stringToJson(raw).takeIf { it.getBoolean("success") == true }?.getObj("player")
    }

    fun getSelectedProfile(_name: String): JsonObject? {
        val name = _name.uppercase()
        if (profileCache.containsKey(name)) return profileCache[name]
        val uuid = getUUID(name) ?: return null

        profileCache[name] = JsonObject(mapOf())
        NoammAddons.Logger.info("Fetching Skyblock Data for $_name")
        val raw = readUrl("$API/hypixel/skyblock/profiles?uuid=$uuid")
        val jsonObject = JsonUtils.stringToJson(raw).takeIf { it.getValue("success").jsonPrimitive.boolean } ?: return null
        val selectedProfile = jsonObject.getArray("profiles")?.find {
            it.jsonObject.getBoolean("selected") == true
        }?.jsonObject?.getObj("members")?.entries?.find {
            it.key == uuid.remove("-")
        }?.value?.jsonObject

        return if (selectedProfile != null) {
            profileCache[name] = selectedProfile
            ThreadUtils.setTimeout(60 * 10 * 1000) { profileCache.remove(name) }
            selectedProfile
        }
        else null
    }

    fun getStatus(name: String): Boolean {
        val raw = readUrl("$API/hypixel/status?uuid=${getUUID(name) ?: return false}")
        val json = JsonUtils.stringToJson(raw).takeIf { it.getValue("success").jsonPrimitive.boolean }
        val isOnline = json?.get("session")?.jsonObject?.get("online")?.jsonPrimitive?.boolean ?: false
        return isOnline
    }

    fun getSecrets(name: String): Int {
        return getHypixelPlayer(name)?.getObj("achievements")?.getInt("skyblock_treasure_hunter") ?: 0
    }

    private val xpRequirements = listOf(
        50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385, 6275, 8940, 12700, 17960, 25340, 35640, 50040, 70040,
        97640, 135640, 188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1683640, 2284640, 3084640, 4149640,
        5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640, 51559640, 66559640, 85559640,
        109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640,
    )

    fun getCatacombsLevel(totalXp: Double): Int {
        if (totalXp < 0) return 0
        for (i in xpRequirements.indices) {
            if (totalXp < xpRequirements[i].toDouble()) {
                return i
            }
        }

        val lastLevelInList = xpRequirements.size
        val xpRequiredForLastLevelInList = xpRequirements.last().toDouble()
        val xpBeyondLastLevelInList = totalXp - xpRequiredForLastLevelInList
        val levelsAboveLastLevel = (xpBeyondLastLevelInList / 200_000_000.0).toInt()
        return lastLevelInList + levelsAboveLastLevel
    }

    private val requiredRegex = Regex("§7§4☠ §cRequires §5.+§c.")
    fun getMagicalPower(talismanBag: MutableList<ItemStack?>, profileInfo: JsonObject): Int {
        return talismanBag.filterNotNull().map {
            val itemId = it.skyblockID?.let { id -> if (id.startsWith("PARTY_HAT_")) "PARTY_HAT" else id }
            val unusable = it.lore.any { line -> requiredRegex.matches(line) }
            val rarity = ItemUtils.getRarity(it)

            val mp = if (unusable) 0
            else when (rarity) {
                ItemUtils.ItemRarity.MYTHIC -> 22
                ItemUtils.ItemRarity.LEGENDARY -> 16
                ItemUtils.ItemRarity.EPIC -> 12
                ItemUtils.ItemRarity.RARE -> 8
                ItemUtils.ItemRarity.UNCOMMON -> 5
                ItemUtils.ItemRarity.COMMON -> 3
                ItemUtils.ItemRarity.SPECIAL -> 3
                ItemUtils.ItemRarity.VERY_SPECIAL -> 5
                else -> 0
            }

            val bonus = when (itemId) {
                "HEGEMONY_ARTIFACT" -> mp
                "ABICASE" -> {
                    val contacts = profileInfo.getObj("nether_island_player_data")?.getObj("abiphone")?.getArray("active_contacts")?.size ?: 0
                    floor(contacts / 2.0).toInt()
                }

                else -> 0
            }

            Pair(itemId, mp + bonus)
        }.groupBy { it.first }.mapValues { entry ->
            entry.value.maxBy { it.second }
        }.values.fold(0) { acc, pair ->
            acc + pair.second
        }.let {
            when {
                profileInfo.getObj("rift")?.getObj("access")?.get("consumed_prism") != null -> it + 11
                else -> it
            }
        }
    }
}