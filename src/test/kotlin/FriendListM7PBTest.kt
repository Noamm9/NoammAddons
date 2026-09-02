import com.github.noamm9.features.impl.general.FriendListM7PB
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FriendListM7PBTest {
    @Test
    fun `extracts ranked friend entries`() {
        val friend = FriendListM7PB.extractFriend("[MVP++] Some_Player is currently offline")

        assertEquals("Some_Player", friend?.displayName)
        assertEquals(listOf("Some_Player"), friend?.lookupNames)
    }

    @Test
    fun `keeps nickname fallback for profile lookup`() {
        val friend = FriendListM7PB.extractFriend("[VIP] Nickname (Real_Player) is in a SkyBlock Lobby")

        assertEquals("Nickname", friend?.displayName)
        assertEquals(listOf("Nickname", "Real_Player"), friend?.lookupNames)
    }

    @Test
    fun `does not treat the friend list header as a player`() {
        assertNull(FriendListM7PB.extractFriend("Friends (Page 1 of 3)"))
    }

    @Test
    fun `formats normal and hour-long personal bests`() {
        assertEquals("5:03", FriendListM7PB.formatPB(303_999))
        assertEquals("1:02:09", FriendListM7PB.formatPB(3_729_000))
    }

    @Test
    fun `compacts master mode locations and converts the floor`() {
        val original = "Eden240 is in SkyBlock - Master Mode The Catacombs - [MM] Floor VII"

        assertEquals("Eden240 is in SkyBlock - In MM7", FriendListM7PB.compactLocationText(original))
        assertEquals(
            "Player is in SkyBlock - In MM6",
            FriendListM7PB.compactLocationText("Player is in SkyBlock - Master Mode The Catacombs - [MM] Floor VI")
        )
    }

    @Test
    fun `leaves already short friend locations unchanged`() {
        val original = "Flotsams is in SkyBlock - Private Island"

        assertEquals(original, FriendListM7PB.compactLocationText(original))
    }

    @Test
    fun `recognizes hypixel multiline friend list packets`() {
        val message = """
            -----------------------------------------------------
                                      Friends (Page 1 of 15) >>
            5staraiko is in Limbo
            Eden240 is in SkyBlock - Master Mode The Catacombs - [MM] Floor VII
            -----------------------------------------------------
        """.trimIndent()

        assertEquals(true, FriendListM7PB.isFriendList(message))
    }

    @Test
    fun `does not capture unrelated multiline chat`() {
        val message = """
            -----------------------------------------------------
            Some unrelated server announcement
            -----------------------------------------------------
        """.trimIndent()

        assertEquals(false, FriendListM7PB.isFriendList(message))
    }

    @Test
    fun `recognizes friend pages with previous and next arrows`() {
        val middlePage = """
            -----------------------------------------------------
                          << Friends (Page 2 of 15) >>
            formulafire4810 is in SkyBlock - Master Mode The Catacombs - [MM] Floor VII
            -----------------------------------------------------
        """.trimIndent()
        val lastPage = """
            -----------------------------------------------------
                          << Friends (Page 15 of 15)
            SomePlayer is currently offline
            -----------------------------------------------------
        """.trimIndent()

        assertEquals(true, FriendListM7PB.isFriendList(middlePage))
        assertEquals(true, FriendListM7PB.isFriendList(lastPage))
    }

    @Test
    fun `splits the real multiline component into styled lines`() {
        val component = Component.literal("-----------------------------------------------------\n")
            .append(Component.literal("Friends (Page 1 of 15) >>\n").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("Eden240 ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("is in SkyBlock - Master Mode The Catacombs - ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("[MM] ").withStyle(ChatFormatting.DARK_RED))
            .append(Component.literal("Floor VII\n-----------------------------------------------------").withStyle(ChatFormatting.YELLOW))

        val lines = FriendListM7PB.splitStyledLines(component).map { it.unformattedText }

        assertEquals(4, lines.size)
        assertEquals("Friends (Page 1 of 15) >>", lines[1])
        assertEquals(
            "Eden240 is in SkyBlock - Master Mode The Catacombs - [MM] Floor VII",
            lines[2]
        )
    }

    @Test
    fun `inserts pb before status in legacy colored hypixel text`() {
        val original = Component.literal("§b5staraiko §eis in Limbo")

        val formatted = FriendListM7PB.formatFriendLine(original, "5staraiko", 301_000).unformattedText

        assertEquals("5staraiko [M7: 5:01] is in Limbo", formatted)
    }

    @Test
    fun `inserts pb and compacts legacy colored mm location`() {
        val original = Component.literal(
            "§bEden240 §eis in SkyBlock - Master Mode The Catacombs - §4[MM] §eFloor VII"
        )

        val formatted = FriendListM7PB.formatFriendLine(original, "Eden240", 301_000).unformattedText

        assertEquals("Eden240 [M7: 5:01] is in SkyBlock - In MM7", formatted)
    }

    @Test
    fun `colors compact mm location red`() {
        val original = Component.literal(
            "§bEden240 §eis in SkyBlock - Master Mode The Catacombs - §4[MM] §eFloor VII"
        )

        val formatted = FriendListM7PB.formatFriendLine(original, "Eden240", 301_000).formattedText

        assertEquals(true, formatted.contains("§cIn MM7"))
    }

    @Test
    fun `keeps full location when shortener is disabled`() {
        val original = Component.literal(
            "§bEden240 §eis in SkyBlock - Master Mode The Catacombs - §4[MM] §eFloor VII"
        )

        val formatted = FriendListM7PB.formatFriendLine(
            original,
            "Eden240",
            301_000,
            shortenLocations = false
        ).unformattedText

        assertEquals(
            "Eden240 [M7: 5:01] is in SkyBlock - Master Mode The Catacombs - [MM] Floor VII",
            formatted
        )
    }

    @Test
    fun `preserves name hover and click events`() {
        val hover = HoverEvent.ShowText(Component.literal("Friends for a month and 2 days"))
        val click = ClickEvent.RunCommand("/socialoptions Eden240")
        val original = Component.empty()
            .append(Component.literal("§bEden240").withStyle { it.withHoverEvent(hover).withClickEvent(click) })
            .append(Component.literal(" §eis in SkyBlock - Hub"))

        val formatted = FriendListM7PB.formatFriendLine(original, "Eden240", 301_000)
        var nameStyle = Style.EMPTY
        formatted.visit({ style, text ->
            if (text.removeFormatting().contains("Eden240")) nameStyle = style
            Optional.empty<Unit>()
        }, Style.EMPTY)

        assertEquals(hover, nameStyle.hoverEvent)
        assertEquals(click, nameStyle.clickEvent)
    }

    @Test
    fun `keeps clickable names as direct siblings for chat modifiers`() {
        val hover = HoverEvent.ShowText(Component.literal("Friends for a month and 2 days\nClick to open social options"))
        val click = ClickEvent.RunCommand("/socialoptions Eden240")
        val original = Component.empty()
            .append(Component.literal("§bEden240").withStyle { it.withHoverEvent(hover).withClickEvent(click) })
            .append(Component.literal(" §eis in SkyBlock - Hub"))
        val line = FriendListM7PB.formatFriendLine(original, "Eden240", 301_000)

        val output = FriendListM7PB.joinLines(listOf(line))
        val pvVisibleName = output.siblings.firstOrNull { component ->
            val command = (component.style.clickEvent as? ClickEvent.RunCommand)?.command
            command?.startsWith("/socialoptions ") == true || command?.startsWith("/viewprofile ") == true
        }

        assertEquals("Eden240", pvVisibleName?.unformattedText)
        assertEquals(hover, pvVisibleName?.style?.hoverEvent)
    }

}
