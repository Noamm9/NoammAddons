package com.github.noamm9.features.impl.general

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.network.ProfileUtils
import gg.essential.universal.UChat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.util.*

object FriendListM7PB: Feature(
    name = "Friend List M7 PB",
    description = "Shows M7 PBs in the friends list."
) {
    private val shortenDungeonLocations by ToggleSetting("Shorten Dungeon Locations", true)
        .withDescription("Shortens Master Mode friend locations to In MM1-7.")

    private val delimiter = Regex("^-{10,}$")
    private val friendListHeader = Regex("(?:^|\\s)Friends \\(Page \\d+ of \\d+\\)", RegexOption.IGNORE_CASE)
    private val friendLine = Regex(
        "^(?:\\[[^]]+]\\s+)?(?<name>[A-Za-z0-9_]{1,16})(?:\\s+\\((?<other>[A-Za-z0-9_]{1,16})\\))?\\s+is\\b",
        RegexOption.IGNORE_CASE
    )
    private val masterModeLocation = Regex(
        " is in SkyBlock - Master Mode The Catacombs - \\[MM] Floor (?<floor>[IVXLCDM]+)",
        RegexOption.IGNORE_CASE
    )

    private var requestId = 0

    override fun init() {
        register<ChatMessageEvent> {
            if (! isFriendList(event.component.unformattedText)) return@register

            event.isCanceled = true
            enrichFriendList(splitStyledLines(event.component))
        }
    }

    private fun enrichFriendList(lines: List<Component>) {
        val thisRequest = ++ requestId

        ThreadUtils.async {
            val friends = lines.mapNotNull { extractFriend(it.unformattedText) }
                .distinctBy { it.displayName.lowercase() }

            if (friends.isEmpty()) return@async showLines(lines)

            val semaphore = Semaphore(MAX_CONCURRENT_LOOKUPS)
            val personalBests = coroutineScope {
                friends.map { friend ->
                    async {
                        friend.displayName.lowercase() to semaphore.withPermit { getM7PB(friend) }
                    }
                }.awaitAll().toMap()
            }

            val enriched = lines.map { line ->
                val friend = extractFriend(line.unformattedText) ?: return@map line
                formatFriendLine(line, friend.displayName, personalBests[friend.displayName.lowercase()] ?: PersonalBest.Unavailable)
            }
            if (thisRequest != requestId) return@async
            showLines(enriched)
        }
    }

    internal fun splitStyledLines(component: Component): List<Component> {
        val lines = mutableListOf(Component.empty())

        component.visit({ style, text ->
            var start = 0
            for (index in text.indices) {
                if (text[index] != '\n') continue
                if (index > start) lines.last().append(styledText(text.substring(start, index), style))
                lines.add(Component.empty())
                start = index + 1
            }
            if (start < text.length) lines.last().append(styledText(text.substring(start), style))
            Optional.empty<String>()
        }, Style.EMPTY)

        while (lines.lastOrNull()?.unformattedText?.isBlank() == true) lines.removeLast()
        return lines
    }

    private suspend fun getM7PB(friend: FriendEntry): PersonalBest {
        for (name in friend.lookupNames) {
            val profile = ProfileUtils.getProfile(name).getOrNull() ?: continue
            val milliseconds = profile.dungeons.masterCatacombs.fastestTimeSPlus[M7_FLOOR]
                ?: return PersonalBest.NoSPlus
            return PersonalBest.Time(milliseconds)
        }
        return PersonalBest.Unavailable
    }

    private fun formatFriendLine(original: Component, name: String, personalBest: PersonalBest): Component {
        return formatFriendLine(original, name, personalBest, shortenDungeonLocations.value)
    }

    private fun formatFriendLine(original: Component, name: String, personalBest: PersonalBest, shortenLocations: Boolean): Component {
        val visibleText = original.unformattedText
        val nameStart = findName(visibleText, name)
        if (nameStart == - 1) return original.copy().append(personalBestComponent(personalBest))

        val nameEnd = nameStart + name.length
        val locationMatch = masterModeLocation.find(visibleText).takeIf { shortenLocations }
        val result = Component.empty()

        appendStyledRange(original, 0, nameStart, result)
        appendStyledRange(original, nameStart, nameEnd, result)
        result.append(personalBestComponent(personalBest))

        if (locationMatch == null) {
            appendStyledRange(original, nameEnd, visibleText.length, result)
            return result
        }

        val floor = locationMatch.groups["floor"]?.value?.romanToDecimal()?.takeIf { it > 0 }
        if (floor == null) {
            appendStyledRange(original, nameEnd, visibleText.length, result)
            return result
        }

        appendStyledRange(original, nameEnd, locationMatch.range.first, result)
        result.append(Component.literal(" is in SkyBlock - ").withStyle(ChatFormatting.YELLOW))
        result.append(Component.literal("In MM$floor").withStyle(ChatFormatting.RED))
        appendStyledRange(original, locationMatch.range.last + 1, visibleText.length, result)
        return result
    }

    private fun personalBestComponent(personalBest: PersonalBest): MutableComponent {
        val value = when (personalBest) {
            is PersonalBest.Time -> Component.literal(formatPB(personalBest.milliseconds)).withStyle(ChatFormatting.AQUA)
            PersonalBest.NoSPlus -> Component.literal("No S+").withStyle(ChatFormatting.GRAY)
            PersonalBest.Unavailable -> Component.literal("N/A").withStyle(ChatFormatting.GRAY)
        }

        return Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal("M7").withStyle(ChatFormatting.RED))
            .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
            .append(value)
            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
    }

    private fun styledText(text: String, style: Style) = Component.literal(text).setStyle(style)

    private fun showLines(lines: List<Component>) {
        if (lines.isEmpty()) return
        val output = joinLines(lines)

        mc.execute {
            if (! ClientReceiveMessageEvents.ALLOW_GAME.invoker().allowReceiveGameMessage(output, false)) {
                ClientReceiveMessageEvents.GAME_CANCELED.invoker().onReceiveGameMessageCanceled(output, false)
                return@execute
            }

            val modified = ClientReceiveMessageEvents.MODIFY_GAME.invoker().modifyReceivedGameMessage(output, false)
            UChat.chat(modified)
            ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(modified, false)
        }
    }

    internal fun joinLines(lines: List<Component>): MutableComponent {
        val output = Component.empty()
        lines.forEachIndexed { index, line ->
            val root = line.copy()
            val siblings = root.siblings.toList()
            root.siblings.clear()
            if (root.string.isNotEmpty()) output.append(root)
            siblings.forEach(output::append)
            if (index != lines.lastIndex) output.append("\n")
        }
        return output
    }

    internal fun extractFriend(text: String): FriendEntry? {
        val match = friendLine.find(text.trim()) ?: return null
        val displayName = match.groups["name"]?.value ?: return null
        val alternateName = match.groups["other"]?.value
        return FriendEntry(displayName, listOfNotNull(displayName, alternateName).distinct())
    }

    internal fun formatPB(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = (totalSeconds % 60).toString().padStart(2, '0')
        return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
    }

    internal fun compactLocationText(text: String): String {
        return masterModeLocation.replace(text) { match ->
            val floor = match.groups["floor"]?.value?.romanToDecimal()?.takeIf { it > 0 } ?: return@replace match.value
            " is in SkyBlock - In MM$floor"
        }
    }

    internal fun isFriendList(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 3) return false
        if (! delimiter.matches(lines.first().trim()) || ! delimiter.matches(lines.last().trim())) return false
        return lines.any { friendListHeader.containsMatchIn(it) }
    }

    private fun findName(text: String, name: String): Int {
        var start = text.indexOf(name, ignoreCase = true)
        while (start != - 1) {
            val before = text.getOrNull(start - 1)
            val after = text.getOrNull(start + name.length)
            if (before?.isUsernameCharacter() != true && after?.isUsernameCharacter() != true) return start
            start = text.indexOf(name, start + 1, ignoreCase = true)
        }
        return - 1
    }

    private fun appendStyledRange(
        component: Component,
        start: Int,
        end: Int,
        output: MutableComponent,
        transformStyle: (Style) -> Style = { it }
    ) {
        if (start >= end) return
        var visibleOffset = 0

        component.visit({ style, text ->
            val visible = text.removeFormatting()
            val segmentStart = visibleOffset
            val segmentEnd = segmentStart + visible.length
            val overlapStart = maxOf(start, segmentStart)
            val overlapEnd = minOf(end, segmentEnd)

            if (overlapStart < overlapEnd) {
                val rawStart = rawIndexAtVisibleOffset(text, overlapStart - segmentStart)
                val rawEnd = rawIndexAtVisibleOffset(text, overlapEnd - segmentStart)
                val formatting = legacyFormattingCode.findAll(text.substring(0, rawStart)).joinToString("") { it.value }
                output.append(Component.literal(formatting + text.substring(rawStart, rawEnd)).setStyle(transformStyle(style)))
            }

            visibleOffset = segmentEnd
            Optional.empty<String>()
        }, Style.EMPTY)
    }

    private fun rawIndexAtVisibleOffset(text: String, visibleOffset: Int): Int {
        var rawIndex = 0
        var visibleIndex = 0

        while (rawIndex < text.length) {
            if ((text[rawIndex] == '§' || text[rawIndex] == '&') && rawIndex + 1 < text.length) {
                rawIndex += 2
                continue
            }
            if (visibleIndex == visibleOffset) return rawIndex
            visibleIndex ++
            rawIndex ++
        }

        return text.length
    }

    internal fun formatFriendLine(original: Component, name: String, milliseconds: Long, shortenLocations: Boolean = true): Component {
        return formatFriendLine(original, name, PersonalBest.Time(milliseconds), shortenLocations)
    }

    private fun Char.isUsernameCharacter() = isLetterOrDigit() || this == '_'

    internal data class FriendEntry(val displayName: String, val lookupNames: List<String>)

    private sealed interface PersonalBest {
        data class Time(val milliseconds: Long): PersonalBest
        data object NoSPlus: PersonalBest
        data object Unavailable: PersonalBest
    }

    private const val M7_FLOOR = "7"
    private const val MAX_CONCURRENT_LOOKUPS = 4
    private val legacyFormattingCode = Regex("[§&][0-9a-fk-or]", RegexOption.IGNORE_CASE)
}
