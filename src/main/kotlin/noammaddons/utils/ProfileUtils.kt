package noammaddons.utils

import com.google.gson.JsonParser
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.JsonUtils.getString
import java.io.BufferedReader
import javax.net.ssl.HttpsURLConnection

/*
  readUrl("https://api.tenios.dev/secrets/$uuid").toInt()
  readUrl("https://subat0mic.click/secrets/$uuid").toInt()
*/


object ProfileUtils {
    val uuidCache = mutableMapOf(mc.session.username to mc.session.playerID)
    val secretCache = mutableMapOf<String, Pair<Int, Long>>()
    const val TWO_MINUTES = 60 * 2 * 1000

    private val mojangApiList = listOf(
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
        "https://api.mojang.com/users/profiles/minecraft/",
        "https://api.ashcon.app/mojang/v2/user/"
    )

    fun getUUID(name: String): String {
        uuidCache[name]?.let { return it }

        for (api in mojangApiList) {
            val response = runCatching { readUrl(api + name) }.getOrNull() ?: continue
            val json = JsonUtils.stringToJson(response)
            val uuid = json.getString("id") ?: json.getString("uuid")
            if (uuid.isNullOrBlank()) return "null"

            uuidCache[name] = uuid
            return uuid
        }

        return "null"
    }

    fun getSecrets(name: String): Int {
        secretCache[name]?.let { (cachedSecrets, lastFetch) ->
            if (System.currentTimeMillis() - lastFetch > TWO_MINUTES) return@let
            return cachedSecrets
        }

        // Thx axle <3
        val response = readUrl("https://api.skyblockextras.com/hypixel/player?uuid=${getUUID(name)}")
        val jsonObject = JsonParser().parse(response)?.asJsonObject
        val secrets = jsonObject?.getAsJsonObject("player")?.getAsJsonObject("achievements")
            ?.getAsJsonPrimitive("skyblock_treasure_hunter")?.asInt ?: 0

        if (secrets > 0) secretCache[name] = secrets to System.currentTimeMillis()
        return secrets
    }

    // todo: Remove when mojang api is functional
    fun getSpiritPet(name: String) = /*readUrl("https://api.tenios.dev/spiritPet/${getUUID(name)}").toBoolean()*/true

    private fun readUrl(url: String): String {
        val connection = WebUtils.makeWebRequest(url)
        (connection as HttpsURLConnection).requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }
}