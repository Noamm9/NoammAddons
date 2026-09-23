package config

import com.github.noamm9.config.types.MapSetting
import com.google.common.reflect.TypeToken
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapSettingTest {
    @Test
    fun `writes integer keys and string values`() {
        val setting = MapSetting<Int, String>("Names", mapOf(0 to "&aFarming", 26 to "Tools"), object: TypeToken<MutableMap<Int, String>>() {}.type)
        val json = setting.write().asJsonObject
        assertEquals("&aFarming", json.get("0").asString)
        assertEquals("Tools", json.get("26").asString)
    }

    @Test
    fun `supports nested values`() {
        val setting = MapSetting<String, List<Int>>("Groups", mapOf("slots" to listOf(1, 3, 5)), object: TypeToken<MutableMap<String, List<Int>>>() {}.type)
        assertEquals(JsonParser.parseString("""{"slots":[1,3,5]}"""), setting.write())
    }

    @Test
    fun `edits entries without changing the supplied defaults`() {
        val defaults = mutableMapOf(0 to "&aFarming")
        val setting = MapSetting<Int, String>("Names", defaults, object: TypeToken<MutableMap<Int, String>>() {}.type)
        assertNull(setting[1])
        setting[0] = "&6Mining"
        setting[1] = "Tools"
        assertEquals("&6Mining", setting[0])
        assertEquals("Tools", setting[1])
        setting.value.remove(1)
        assertNull(setting[1])
        assertEquals(mapOf(0 to "&aFarming"), defaults)
        assertEquals("&6Mining", setting.write().asJsonObject.get("0").asString)
    }
}
