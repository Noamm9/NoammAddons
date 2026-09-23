package config

import com.github.noamm9.config.types.ListSetting
import com.google.common.reflect.TypeToken
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListSettingTest {
    @Test
    fun `writes values in order including duplicates`() {
        val setting = ListSetting<String>("Names", listOf("First", "Second", "First"), object: TypeToken<MutableList<String>>() {}.type)
        assertEquals(JsonParser.parseString("""["First","Second","First"]"""), setting.write())
    }

    @Test
    fun `supports nested values`() {
        val setting = ListSetting<Map<String, Int>>("Groups", listOf(mapOf("slot" to 3)), object: TypeToken<MutableList<Map<String, Int>>>() {}.type)
        assertEquals(JsonParser.parseString("""[{"slot":3}]"""), setting.write())
    }

    @Test
    fun `edits entries without changing the supplied defaults`() {
        val defaults = mutableListOf("First", "Second")
        val setting = ListSetting<String>("Names", defaults, object: TypeToken<MutableList<String>>() {}.type)
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
        val type = object: TypeToken<MutableList<String>>() {}.type
        val first = ListSetting<String>("First", type = type)
        val second = ListSetting<String>("Second", type = type)
        first.value.add("Value")
        assertEquals(emptyList(), second.value)
        assertEquals(JsonParser.parseString("[]"), second.write())
    }
}
