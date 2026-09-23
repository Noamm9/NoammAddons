package config

import com.github.noamm9.config.types.ListSetting
import com.google.gson.JsonParser
import kotlin.test.*

class ListSettingTest {
    @Test
    fun `writes values in order including duplicates`() {
        val setting = ListSetting("Names", mutableListOf("First", "Second", "First"))
        assertEquals(JsonParser.parseString("""["First","Second","First"]"""), setting.write())
    }

    @Test
    fun `supports nested values`() {
        val setting = ListSetting("Groups", mutableListOf(mapOf("slot" to 3)))
        assertEquals(JsonParser.parseString("""[{"slot":3}]"""), setting.write())
    }

    @Test
    fun `edits entries without changing the supplied defaults`() {
        val defaults = mutableListOf("First", "Second")
        val setting = ListSetting("Names", defaults)
        setting[0] = "Changed"
        setting.value.add("Third")
        setting.value.removeAt(1)
        assertEquals("Changed", setting[0])
        assertEquals("Third", setting[1])
        assertEquals(listOf("First", "Second"), defaults)
        assertFailsWith<IndexOutOfBoundsException> { setting[2] }
    }

    @Test
    fun `empty defaults are independent between settings`() {
        val first = ListSetting<String>("First")
        val second = ListSetting<String>("Second")
        first.value.add("Value")
        assertEquals(emptyList(), second.value)
        assertEquals(JsonParser.parseString("[]"), second.write())
    }

    @Test
    fun `supports vararg constructor`() {
        val setting = ListSetting("Names", "First", "Second")
        assertEquals("First", setting[0])
        assertEquals("Second", setting[1])
        assertEquals(2, setting.value.size)
    }

    @Test
    fun `triggers changeListener on set`() {
        val setting = ListSetting("Names", "First")
        var called = false
        setting.changeListener = { called = true }
        setting[0] = "Second"
        assertTrue(called)
        assertEquals("Second", setting[0])
    }
}
