package config

import com.github.noamm9.config.types.MapSetting
import com.google.gson.JsonParser
import kotlin.test.*

class MapSettingTest {
    @Test
    fun `writes integer keys and string values`() {
        val setting = MapSetting("Names", mutableMapOf(0 to "&aFarming", 26 to "Tools"))
        val json = setting.write().asJsonObject
        assertEquals("&aFarming", json.get("0").asString)
        assertEquals("Tools", json.get("26").asString)
    }

    @Test
    fun `supports nested values`() {
        val setting = MapSetting("Groups", mutableMapOf("slots" to listOf(1, 3, 5)))
        assertEquals(JsonParser.parseString("""{"slots":[1,3,5]}"""), setting.write())
    }

    @Test
    fun `edits entries without changing the supplied defaults`() {
        val defaults = mutableMapOf(0 to "&aFarming")
        val setting = MapSetting("Names", defaults)
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

    @Test
    fun `supports vararg constructor`() {
        val setting = MapSetting("Names", 0 to "First", 1 to "Second")
        assertEquals("First", setting[0])
        assertEquals("Second", setting[1])
        assertEquals(2, setting.value.size)
    }

    @Test
    fun `triggers changeListener on set`() {
        val setting = MapSetting("Names", 0 to "First")
        var called = false
        setting.changeListener = { called = true }
        setting[0] = "Second"
        assertTrue(called)
        assertEquals("Second", setting[0])
    }
}
