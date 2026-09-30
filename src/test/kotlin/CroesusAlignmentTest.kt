import com.github.noamm9.utils.dungeons.alignNewestFirst
import kotlin.test.*

class CroesusAlignmentTest {
    private data class Run(val floor: String, val grade: String)

    private fun align(croesus: List<String>, tracked: List<Run>) =
        alignNewestFirst(croesus, tracked) { floor, run -> floor == run.floor }.mapValues { it.value.grade }

    @Test
    fun `matches by position when nothing is missing`() {
        val tracked = listOf(Run("M7", "S+"), Run("M7", "S"), Run("F7", "S+"))
        assertEquals(mapOf(0 to "S+", 1 to "S", 2 to "S+"), align(listOf("M7", "M7", "F7"), tracked))
    }

    @Test
    fun `leaves runs expired from croesus unmatched at the end`() {
        val tracked = listOf(Run("M7", "S+"), Run("M7", "S"), Run("M6", "A"))
        assertEquals(mapOf(0 to "S+", 1 to "S"), align(listOf("M7", "M7"), tracked))
    }

    @Test
    fun `skips untracked runs of a different floor without shifting`() {
        // A F5 run done without the mod sits between two tracked runs.
        val tracked = listOf(Run("M7", "S+"), Run("M6", "S"))
        assertEquals(mapOf(0 to "S+", 2 to "S"), align(listOf("M7", "F5", "M6"), tracked))
    }

    @Test
    fun `skips tracked runs that croesus does not have`() {
        val tracked = listOf(Run("M7", "S+"), Run("F3", "S"), Run("M6", "A"))
        assertEquals(mapOf(0 to "S+", 1 to "A"), align(listOf("M7", "M6"), tracked))
    }

    @Test
    fun `leaves everything unknown when nothing is compatible`() {
        assertTrue(align(listOf("M7", "M7"), listOf(Run("F7", "S+"))).isEmpty())
        assertTrue(align(emptyList(), listOf(Run("F7", "S+"))).isEmpty())
        assertTrue(align(listOf("M7"), emptyList()).isEmpty())
    }
}
