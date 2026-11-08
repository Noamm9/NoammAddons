import kotlin.test.*

class CheatMarkersTest {
    private fun check(source: String) = findCheatMarkerViolations("Test.kt", source.trimIndent().lines())

    @Test
    fun taggedCheatSettingPasses() {
        val source = """
            object Foo: Feature("desc") {
                //#if CHEAT
                private val tb by ToggleSetting("Triggerbot").cheat()
                //#endif
            }
        """
        assertEquals(emptyList(), check(source))
    }

    @Test
    fun untaggedCheatSettingFails() {
        val source = """
            object Foo: Feature("desc") {
                //#if CHEAT
                private val tb by ToggleSetting("Triggerbot")
                //#endif
            }
        """
        val violations = check(source)
        assertEquals(1, violations.size)
        assertTrue(violations.single().startsWith("Test.kt:3:"))
        assertTrue("missing `.cheat()`" in violations.single())
    }

    @Test
    fun multiLineSettingWithTrailingTagPasses() {
        val source = """
            object Foo: Feature("desc") {
                //#if CHEAT
                private val opts by MultiCheckboxSetting(
                    "TB Options", mutableMapOf(
                        "Chest (big)" to false
                    )
                ).showIf { true }.cheat()
                //#endif
            }
        """
        assertEquals(emptyList(), check(source))
    }

    @Test
    fun chainedTagOnNextLinePasses() {
        val source = """
            object Foo: Feature("desc") {
                //#if CHEAT
                private val tb by ToggleSetting("Triggerbot")
                    .cheat()
                //#endif
            }
        """
        assertEquals(emptyList(), check(source))
    }

    @Test
    fun tagOutsideCheatBlockFails() {
        val source = """
            object Foo: Feature("desc") {
                private val tb by ToggleSetting("Legit thing").cheat()
            }
        """
        assertTrue("not inside an #if CHEAT block" in check(source).single())
    }

    @Test
    fun wholeFileCheatFeatureNeedsTagButItsSettingsDoNot() {
        val tagged = """
            //#if CHEAT
            object Auto: Feature("desc", cheat = true) {
                private val delay by SliderSetting("Delay", 1, 0, 10, 1)
            }
            //#endif
        """
        assertEquals(emptyList(), check(tagged))

        val untagged = """
            //#if CHEAT
            object Auto: Feature("desc") {
                private val delay by SliderSetting("Delay", 1, 0, 10, 1)
            }
            //#endif
        """
        assertTrue("missing `cheat = true`" in check(untagged).single())
    }

    @Test
    fun featureTagOutsideCheatBlockFails() {
        val source = """
            object Legit: Feature("desc", cheat = true) {
            }
        """
        assertTrue("not inside an #if CHEAT block" in check(source).single())
    }

    @Test
    fun legitElseBranchIsNotCheat() {
        val source = """
            object MapConfig: SettingProvider {
                //#if CHEAT
                val map by ToggleSetting("Cheater Map").cheat()
                //#else
                //!val map by ToggleSetting("Legit").hidden()
                //#endif
                val plain by ToggleSetting("Plain")
            }
        """
        assertEquals(emptyList(), check(source))
    }

    @Test
    fun ifLegitElseBranchIsCheat() {
        val source = """
            object Foo: Feature("desc") {
                //#if LEGIT
                //!val x by ToggleSetting("Legit")
                //#else
                val y by ToggleSetting("Cheat")
                //#endif
            }
        """
        assertTrue("missing `.cheat()`" in check(source).single())
    }

    @Test
    fun settingProviderInCheatFileStillNeedsTags() {
        val source = """
            object MapConfig: SettingProvider {
                //#if CHEAT
                val esp by ToggleSetting("Mimic ESP")
                //#endif
            }
        """
        assertEquals(1, check(source).size)
    }

    @Test
    fun multiLineFeatureHeaderIsRead() {
        val source = """
            object Abilities: Feature(
                //#if CHEAT
                "cheat description"
                //#else
                //!"legit description"
                //#endif
            ) {
                //#if CHEAT
                private val autoUlt by ToggleSetting("Auto Use Ultimate").cheat()
                //#endif
            }
        """
        assertEquals(emptyList(), check(source))
    }
}
