package noammaddons.utils

import com.google.gson.JsonParser
import gg.essential.api.EssentialAPI
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.JsonUtils.makeWebRequest
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

    fun getUUID(name: String): String {
        uuidCache[name]?.let { return it }
        return EssentialAPI.getMojangAPI().getUUID(name)?.get().toString().also {
            if (it != "null") {
                uuidCache[name] = it
            }
        }
    }

    fun getSecrets(name: String): Int {
        secretCache[name]?.let { (cachedSecrets, lastFetch) ->
            if (System.currentTimeMillis() - lastFetch > TWO_MINUTES) return@let
            return cachedSecrets
        }

        val response = readUrl("https://api.skyblockextras.com/hypixel/player?uuid=${getUUID(name)}")
        val jsonObject = JsonParser().parse(response)?.asJsonObject ?: return 0
        if (jsonObject.getAsJsonPrimitive("success")?.asBoolean != true) return 0
        val secrets = jsonObject.getAsJsonObject("player")?.getAsJsonObject("achievements")
            ?.getAsJsonPrimitive("skyblock_treasure_hunter")?.asInt ?: return 0

        secretCache[name] = secrets to System.currentTimeMillis()
        return secrets
    }

    fun getSpiritPet(name: String) = readUrl("https://api.tenios.dev/spiritPet/${getUUID(name)}").toBoolean()

    private fun readUrl(url: String): String {
        val connection = makeWebRequest(url)
        (connection as HttpsURLConnection).requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }
}