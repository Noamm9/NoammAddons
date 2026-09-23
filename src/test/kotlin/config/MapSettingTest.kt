package config

import com.github.noamm9.config.types.MapSetting
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MapSettingTest {
    @Test
    fun `writes integer keys and string values`() {
        val setting = MapSetting<Int, String>("Names", mapOf(0 to "&aFarming", 26 to "Tools"))
        val json = setting.write().asJsonObject
        assertEquals("&aFarming", json.get("0").asString)
        assertEquals("Tools", json.get("26").asString)
    }

    @Test
    fun `supports nested values`() {
        val setting = MapSetting<String, List<Int>>("Groups", mapOf("slots" to listOf(1, 3, 5)))
        assertEquals(JsonParser.parseString("""{"slots":[1,3,5]}"""), setting.write())
    }
}
