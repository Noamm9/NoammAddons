import org.gradle.api.GradleException
import java.io.File
import kotlin.test.*

class PreprocessingTest {
    private fun process(variant: String, source: String, stripLineComments: Boolean = false): List<String> {
        val file = File.createTempFile("preprocess", ".kt").also(File::deleteOnExit)
        file.writeText(source.trimIndent())
        return processDirectives(file, variant, stripLineComments)
    }

    @Test
    fun cheatBlock() {
        val source = """
            a
            //#if CHEAT
            cheat
            //#endif
            b
        """
        assertEquals(listOf("a", "cheat", "b"), process("cheat", source))
        assertEquals(listOf("a", "b"), process("legit", source))
    }

    @Test
    fun legitBlock() {
        val source = """
            //#if LEGIT
            //!legit()
            //#endif
        """
        assertEquals(listOf("legit()"), process("legit", source))
        assertEquals(emptyList(), process("cheat", source))
    }

    @Test
    fun elseBlock() {
        val source = """
            //#if CHEAT
            cheat
            //#else
            legit
            //#endif
        """
        assertEquals(listOf("cheat"), process("cheat", source))
        assertEquals(listOf("legit"), process("legit", source))
    }

    @Test
    fun nestedBlocks() {
        val source = """
            //#if CHEAT
            //#if LEGIT
            never
            //#else
            cheat
            //#endif
            //#endif
        """
        assertEquals(listOf("cheat"), process("cheat", source))
        assertEquals(emptyList(), process("legit", source))
    }

    @Test
    fun inlineLegit() {
        val source = """
            object Foo(
                //#if LEGIT name = "Legit",
                id = "foo"
            )
        """
        assertEquals(listOf("object Foo(", "    name = \"Legit\",", "    id = \"foo\"", ")"), process("legit", source))
        assertEquals(listOf("object Foo(", "    id = \"foo\"", ")"), process("cheat", source))
    }

    @Test
    fun inlineLegitMarker() {
        assertEquals(listOf("import foo"), process("legit", "//#if LEGIT //!import foo"))
    }

    @Test
    fun inlineLegitInCheatBlock() {
        val source = """
            //#if CHEAT
            //#if LEGIT legit
            //#endif
        """
        assertEquals(emptyList(), process("legit", source))
    }

    @Test
    fun inlineCheat() {
        assertFailsWith<GradleException> { process("cheat", "//#if CHEAT cheat()") }
        assertFailsWith<GradleException> { process("legit", "    //#if CHEAT cheat()") }
    }

    @Test
    fun stripComments() {
        val source = """
            {
              // comment
              //!"key": "value"
            }
        """
        assertEquals(listOf("{", "  \"key\": \"value\"", "}"), process("legit", source, stripLineComments = true))
    }

    @Test
    fun unbalanced() {
        assertFailsWith<GradleException> { process("cheat", "//#else") }
        assertFailsWith<GradleException> { process("cheat", "//#endif") }
        assertFailsWith<GradleException> { process("cheat", "//#if CHEAT") }
    }

    @Test
    fun placeholders() {
        val properties = mapOf("mod_id" to "noammaddons")
        assertEquals("id = noammaddons, @UNKNOWN@", replaceProperties("id = @MOD_ID@, @UNKNOWN@", properties))
    }
}
