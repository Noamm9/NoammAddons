package config

import com.github.noamm9.config.ConfigMigrator
import com.github.noamm9.config.migrators.ConfigMigratorV0ToV1
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfigMigratorV0ToV1Test {
    @Test
    fun `migrates configSettings array to object`() {
        val feature = oldFeature("Dungeon", true, listOf(
            "key" to JsonPrimitive(5),
            "name" to JsonPrimitive("test"),
            "enabled" to JsonPrimitive(false),
        ))

        val migrated = ConfigMigratorV0ToV1.migrate(oldRoot(feature))

        assertEquals(1, migrated.get("version").asInt)
        val settings = migrated.getAsJsonArray("config").get(0).asJsonObject.getAsJsonObject("configSettings")
        assertEquals(5, settings.get("key").asInt)
        assertEquals("test", settings.get("name").asString)
        assertEquals(false, settings.get("enabled").asBoolean)
    }

    @Test
    fun `preserves feature name and enabled`() {
        val migrated = ConfigMigratorV0ToV1.migrate(oldRoot(oldFeature("AutoGFS", true, emptyList())))

        val featureOut = migrated.getAsJsonArray("config").get(0).asJsonObject
        assertEquals("AutoGFS", featureOut.get("name").asString)
        assertEquals(true, featureOut.get("enabled").asBoolean)
    }

    @Test
    fun `migrates multiple features`() {
        val featureA = oldFeature("A", true, listOf("x" to JsonPrimitive(1)))
        val featureB = oldFeature("B", false, listOf("y" to JsonPrimitive(2)))

        val migrated = ConfigMigratorV0ToV1.migrate(oldRoot(featureA, featureB))

        val config = migrated.getAsJsonArray("config")
        assertEquals(2, config.size())
        assertEquals("A", config.get(0).asJsonObject.get("name").asString)
        assertEquals("B", config.get(1).asJsonObject.get("name").asString)
    }

    @Test
    fun `passes hud through unchanged`() {
        val hud = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("name", "Scoreboard")
                addProperty("x", 12.5f)
                addProperty("y", 3.0f)
                addProperty("scale", 1.25f)
            })
        }
        val root = JsonObject().apply {
            add("config", JsonArray())
            add("hud", hud)
        }

        val migrated = ConfigMigratorV0ToV1.migrate(root)

        val hudOut = migrated.getAsJsonArray("hud")
        assertEquals(1, hudOut.size())
        val hudObj = hudOut.get(0).asJsonObject
        assertEquals("Scoreboard", hudObj.get("name").asString)
        assertEquals(12.5f, hudObj.get("x").asFloat)
        assertEquals(3.0f, hudObj.get("y").asFloat)
        assertEquals(1.25f, hudObj.get("scale").asFloat)
    }

    @Test
    fun `handles empty config`() {
        val migrated = ConfigMigratorV0ToV1.migrate(JsonObject())

        assertEquals(1, migrated.get("version").asInt)
        assertTrue(migrated.getAsJsonArray("config").isEmpty)
    }

    @Test
    fun `handles missing config array`() {
        val migrated = ConfigMigratorV0ToV1.migrate(JsonObject())

        assertTrue(migrated.getAsJsonArray("config").isEmpty)
    }

    @Test
    fun `handles missing configSettings`() {
        val feature = JsonObject().apply {
            addProperty("name", "Some")
            addProperty("enabled", true)
        }

        val migrated = ConfigMigratorV0ToV1.migrate(oldRoot(feature))

        val settings = migrated.getAsJsonArray("config").get(0).asJsonObject.getAsJsonObject("configSettings")
        assertTrue(settings.isEmpty)
    }

    @Test
    fun `rejects backwards migration`() {
        assertFailsWith<IllegalArgumentException> {
            object: ConfigMigrator(2, 1) {
                override fun migrate(root: JsonObject) = root
            }
        }
    }

    @Test
    fun `rejects same version`() {
        assertFailsWith<IllegalArgumentException> {
            object: ConfigMigrator(1, 1) {
                override fun migrate(root: JsonObject) = root
            }
        }
    }

    private fun oldRoot(vararg features: JsonObject): JsonObject {
        val config = JsonArray()
        features.forEach(config::add)
        return JsonObject().apply { add("config", config) }
    }

    private fun oldFeature(name: String, enabled: Boolean, settings: List<Pair<String, JsonElement>>): JsonObject {
        val settingsArray = JsonArray()
        settings.forEach { (key, value) ->
            settingsArray.add(JsonObject().apply { add(key, value) })
        }
        return JsonObject().apply {
            addProperty("name", name)
            addProperty("enabled", enabled)
            add("configSettings", settingsArray)
        }
    }
}