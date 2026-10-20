package utils

import com.github.noamm9.utils.pets.LoadoutPetSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadoutPetSyncTest {
    private data class Pet(val level: Int, val skin: String?, val item: String?)
    private val old = Pet(180, "skin", "Unalloyed Speed")
    private val actual = Pet(200, null, "Antique Remedies")
    private var time = 0L
    private val cache = mutableMapOf("a" to old)
    private var shown: Pet? = null
    private val sync = LoadoutPetSync(cache::get, { key, pet -> cache[key] = pet }, { shown = it }, { time })

    @Test fun `packet before confirmation wins over stale prediction`() {
        sync.select("a", "Terminator")
        sync.receive(actual)
        sync.confirm("Terminator")
        assertEquals(actual, shown)
        assertEquals(actual, cache["a"])
    }

    @Test fun `confirmation before packet predicts then replaces every field`() {
        sync.select("a", "Terminator")
        sync.confirm("Terminator")
        assertEquals(old, shown)
        sync.receive(actual)
        assertEquals(actual, shown)
        assertEquals(actual, cache["a"])
    }

    @Test fun `unknown loadout waits for authoritative packet`() {
        sync.select("b", "Mage")
        sync.confirm("Mage")
        assertNull(shown)
        sync.receive(actual)
        assertEquals(actual, cache["b"])
    }

    @Test fun `repeated equip accepts changed server pet`() {
        repeat(2) {
            sync.select("a", "Terminator")
            sync.receive(if (it == 0) old else actual)
            sync.confirm("Terminator")
        }
        assertEquals(actual, shown)
        assertEquals(actual, cache["a"])
    }

    @Test fun `opening a menu updates display without assigning a loadout`() {
        sync.receive(actual)
        assertEquals(actual, shown)
        assertEquals(old, cache["a"])
    }

    @Test fun `expired request cannot poison another loadout cache`() {
        sync.select("a", "Terminator")
        time = 5001
        sync.receive(actual)
        sync.confirm("Terminator")
        assertEquals(actual, shown)
        assertEquals(old, cache["a"])
    }

    @Test fun `unrelated confirmation does not predict`() {
        sync.select("a", "Terminator")
        sync.confirm("Mage")
        assertNull(shown)
    }

    @Test fun `world reset discards pending prediction`() {
        sync.select("a", "Terminator")
        sync.reset()
        sync.confirm("Terminator")
        assertNull(shown)
    }
}
