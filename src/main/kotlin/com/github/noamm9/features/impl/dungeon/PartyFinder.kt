package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.ContainerFullyOpenedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.DungeonProfileSummary
import com.github.noamm9.utils.dungeons.DungeonProfileSummaryProvider
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

object PartyFinder: Feature() {
    private val showLevelReq by ToggleSetting("Show Level Req", true)
        .withDescription("Shows the red level requirement number.")
        .section("Menu")
    private val showMissingOverlay by ToggleSetting("Show Missing Classes", true)
        .withDescription("Shows missing classes on the head.")

    private val showTooltipStats by ToggleSetting("Show Stats", true)
        .withDescription("Shows player stats (Cata/Secrets/PB) in tooltip.")
        .section("Tooltip")
    private val showSecrets by ToggleSetting("Show Secrets", true)
        .withDescription("Shows total secrets and overall secrets per run.")
        .showIf { showTooltipStats.value }
    private val showPB by ToggleSetting("Show PB", true)
        .withDescription("Shows personal best for the current floor.")
        .showIf { showTooltipStats.value }
    private val showMissingTooltip by ToggleSetting("Show Missing List", true)
        .withDescription("Shows the list of missing classes at the bottom of the tooltip.")

    private val autoKick by ToggleSetting("Auto Kick", false)
        .withDescription("Automatically kick players that don't meet requirements.")
        .section("Auto Kick")
    private val autoKickFloor by DropdownSetting("Floor", 6, listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7"))
        .showIf { autoKick.value }
    private val masterMode by ToggleSetting("Master Mode", true).showIf { autoKick.value }
    private val informKicked by ToggleSetting("Inform Kicked", false)
        .withDescription("Send a party chat message before kicking.")
        .showIf { autoKick.value }
    private val noDupe by ToggleSetting("No Dupe", false)
        .withDescription("Automatically kicks players who join with a class that is already in your party.")
        .showIf { autoKick.value }
    private val minimumPB by ToggleSetting("Minimum PB", true)
        .withDescription("Require players to have an S+ PB at or under the configured minimum seconds.")
        .showIf { autoKick.value }
    private val pbLimitSeconds by SliderSetting("Minimum Seconds", 400, 60, 480, 10, suffix = "s")
        .withDescription("Players fail if their selected-floor S+ PB is missing or slower than this many seconds.")
        .showIf { autoKick.value && minimumPB.value }
    private val minimumSecrets by SliderSetting("Minimum Secrets", 0, 0, 200, 1, suffix = "k")
        .withDescription("Minimum secrets in thousands.")
        .showIf { autoKick.value }
    private val minimumSecretsPerRun by SliderSetting("Minimum Secrets / Run", 0.0, 0.0, 20.0, 0.1)
        .withDescription("Minimum overall dungeon secrets per run.")
        .showIf { autoKick.value }

    private val joinedRegex = Regex("^\\u00a7dParty Finder \\u00a7f> (.+?) \\u00a7ejoined the dungeon group! \\(\\u00a7b(\\w+) Level (\\d+)\\u00a7e\\)$")
    private val kickedPlayers = mutableSetOf<String>()
    private const val prefix = "&9AutoKick &f>"

    private val partyMembersRegex = Regex("\\u00a75 (.+)\\u00a7f: \\u00a7e(.+)\\u00a7b \\(..(\\d+)..\\)")
    private val levelRequiredRegex = Regex("\\u00a77Dungeon Level Required: \\u00a7b(\\d+)")
    private val selectedClassRegex = Regex("Currently Selected: (.+)")
    private val selectDungeonClassRegex = Regex("\\u00a77View and select a dungeon class\\.")
    private val classNames = listOf("&4&lArcher", "&a&lTank", "&6&lBerserk", "&5&lHealer", "&b&lMage")
    private var selectedClass: String? = null
    private var inPartyFinder = false

    private val headSlots = setOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 44
    )

    override fun init() {
        register<ContainerEvent.Render.Slot.Post> {
            if (!inPartyFinder) return@register
            if (event.screen.title.string != "Party Finder") return@register
            if (!showLevelReq.value && !showMissingOverlay.value) return@register
            if (event.slot.index !in headSlots) return@register

            val item = event.slot.item.takeUnless { it.isEmpty || !it.`is`(Blocks.PLAYER_HEAD.asItem()) } ?: return@register
            val classes = mutableListOf<String>()
            var levelRequired = 0

            for (line in item.lore) {
                when {
                    line.contains("Dungeon Level Required:") && showLevelReq.value -> {
                        levelRequired = levelRequiredRegex.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }

                    partyMembersRegex.matches(line) && showMissingOverlay.value -> {
                        classes.add(partyMembersRegex.matchEntire(line)?.destructured?.component2().orEmpty())
                    }
                }
            }

            event.context.pose().translate(event.slot.x.toFloat(), event.slot.y.toFloat())

            if (levelRequired != 0) {
                val text = "&c$levelRequired"
                Render2D.drawString(event.context, text, 16f - text.width() * 0.6f, 0f, scale = 0.6f)
            }

            if (showMissingOverlay.value) {
                val missingClasses = classNames
                    .filter { missingClass -> classes.none { it == missingClass.removeFormatting() } }
                    .map { it.take(5) }

                val missingLines = listOf(
                    missingClasses.take(2).joinToString(""),
                    missingClasses.drop(2).take(2).joinToString("")
                ).filter { it.isNotBlank() }

                for ((index, line) in missingLines.withIndex()) {
                    Render2D.drawString(
                        event.context,
                        line,
                        0,
                        10f - if (index == 1) 6f else 0f,
                        scale = 0.65
                    )
                }
            }

            event.context.pose().translate(-event.slot.x.toFloat(), -event.slot.y.toFloat())
        }

        register<ContainerEvent.Render.Tooltip> {
            if (!inPartyFinder) return@register
            if (event.screen.title.string != "Party Finder") return@register
            if (!event.stack.`is`(Items.PLAYER_HEAD)) return@register
            if (event.screen.menu.slots.find { it.item == event.stack }?.index !in headSlots) return@register

            var floor = 0
            var masterModeTooltip = false
            val remainingClasses = classNames.map { it.removeFormatting() }.toMutableList()

            event.lore.toList().forEachIndexed { index, component ->
                val line = component.formattedText

                if (line.removeFormatting().contains("Dungeon: Master Mode")) masterModeTooltip = true
                if (line.contains("\u00a77Floor: \u00a7bFloor ")) {
                    val floorText = line.split(" ").last()
                    floor = floorText.toIntOrNull() ?: floorText.romanToDecimal()
                }

                partyMembersRegex.matchEntire(line)?.destructured?.let { (playerNameText, classNameText, classLevelText) ->
                    val playerName = playerNameText.removeFormatting()
                    val className = classNameText.removeFormatting()
                    val classLevel = classLevelText.toInt()
                    val classLevelColor = getColor(classLevel)
                    val stats = if (showTooltipStats.value) getStats(playerName, floor, masterModeTooltip) else ""

                    event.lore[index] = Component.literal(
                        " $playerNameText: &e$className $classLevelColor$classLevel $stats".addColor()
                    )
                    remainingClasses.remove(className)
                }
            }

            val selectedClassName = selectedClass?.removeFormatting()
            if (selectedClassName != null && selectedClassName in remainingClasses) {
                val selectedIndex = remainingClasses.indexOf(selectedClassName)
                remainingClasses[selectedIndex] = "${selectedClass}&7"
            }

            if (showMissingTooltip.value) {
                event.lore.add(
                    Component.literal(("&cMissing: &7" + remainingClasses.joinToString(", ")).addColor())
                )
            }
        }

        register<ContainerFullyOpenedEvent> {
            if (event.title.string == "Catacombs Gate") {
                event.items[45]?.lore
                    ?.takeIf { it.size > 3 && selectDungeonClassRegex.matches(it[0]) }
                    ?.let { lore ->
                        val selectedName = selectedClassRegex.find(lore[2].removeFormatting())?.groupValues?.getOrNull(1)
                            ?: return@let
                        val selectedIndex = classNames.indexOfFirst { it.removeFormatting() == selectedName }
                        if (selectedIndex != -1) {
                            selectedClass = classNames[selectedIndex]
                        }
                    }
            }
            else if (event.title.string == "Party Finder") {
                event.items[50]
                    ?.takeIf { it.`is`(Items.NETHER_STAR) }
                    ?.lore
                    ?.getOrNull(5)
                    ?.let {
                        if (!it.contains("\u00a7aCombat Level: ")) {
                            inPartyFinder = true
                        }
                    }
            }
        }

        register<ContainerEvent.Close> { inPartyFinder = false }
        register<WorldChangeEvent> { kickedPlayers.clear() }

        register<ChatMessageEvent> {
            if (!autoKick.value || !PartyUtils.isLeader()) return@register

            val joinMatch = joinedRegex.find(event.formattedText) ?: return@register
            val (joinedName, joinedClassName, _) = joinMatch.destructured
            val name = joinedName.removeFormatting()
            val joinedClass = DungeonClass.fromName(joinedClassName)

            if (name in kickedPlayers) {
                ChatUtils.modMessage("$prefix &cAuto-kicking &e$name &c(previously kicked)")
                ThreadUtils.scheduledTask(6) { ChatUtils.sendCommand("party kick $name") }
                return@register
            }

            scope.launch {
                val reasons = checkPlayer(name, joinedClass).takeUnless { it.isEmpty() } ?: return@launch
                kickedPlayers.add(name)

                if (informKicked.value) {
                    ChatUtils.sendCommand("pc Kicking $name: ${reasons.joinToString(", ")}")
                    ThreadUtils.scheduledTask(6) { ChatUtils.sendCommand("party kick $name") }
                }
                else {
                    ChatUtils.modMessage("&cKicking &e$name:&r ${reasons.joinToString(", ")}")
                    ChatUtils.sendCommand("party kick $name")
                }
            }
        }
    }

    private suspend fun checkPlayer(name: String, joinedClass: DungeonClass): List<String> {
        if (name.equalsOneOf("Noamm", mc.user.name)) return emptyList()

        val reasons = mutableListOf<String>()
        if (noDupe.value) {
            PartyFinderRules.duplicateClassReason(joinedClass, currentPartyClasses(name))?.let(reasons::add)
        }

        if (!requiresProfileCheck()) {
            return reasons
        }

        val floor = autoKickFloor.value + 1
        val pbFloor = floor.takeIf { minimumPB.value }
        val summary = DungeonProfileSummaryProvider.loadSummary(name, pbFloor, masterMode.value) ?: return reasons

        reasons += PartyFinderRules.evaluate(
            summary,
            PartyFinderRuleConfig(
                floor = floor,
                masterMode = masterMode.value,
                enforcePbLimit = minimumPB.value,
                pbLimitSeconds = pbLimitSeconds.value,
                minimumSecretsThousands = minimumSecrets.value,
                minimumSecretsPerRun = minimumSecretsPerRun.value,
            )
        )
        return reasons
    }

    private fun getStats(name: String, floor: Int, masterMode: Boolean): String {
        val summary = DungeonProfileSummaryProvider.getSummaryOrRequest(
            playerName = name,
            floor = floor.takeIf { it > 0 },
            masterMode = masterMode
        ) ?: return "&7(Loading...)"

        return formatStats(summary)
    }

    private fun formatStats(summary: DungeonProfileSummary): String {
        return buildString {
            append("&b(&6${summary.catacombsLevel ?: "?"}&b)&r")

            if (showSecrets.value) {
                val secrets = summary.totalSecrets?.toString() ?: "?"
                val secretsPerRun = summary.secretsPerRun?.toFixed(2) ?: "?"
                append(" &8[&a$secrets&8/&b$secretsPerRun&8]&r")
            }

            if (showPB.value) {
                val pb = summary.floorPbMilliseconds?.let(PartyFinderRules::formatDuration) ?: "N/A"
                append(" &8[&9$pb&8]&r")
            }
        }
    }

    private fun getColor(level: Int) = when {
        level >= 50 -> "&c&l"
        level >= 45 -> "&c"
        level >= 40 -> "&6"
        level >= 35 -> "&d"
        level >= 30 -> "&9"
        level >= 25 -> "&b"
        level >= 20 -> "&2"
        level >= 15 -> "&a"
        level >= 10 -> "&e"
        level >= 5 -> "&f"
        else -> "&7"
    }

    private fun requiresProfileCheck() = minimumPB.value || minimumSecrets.value > 0 || minimumSecretsPerRun.value > 0

    private fun currentPartyClasses(excludeName: String): Set<DungeonClass> {
        val classes = PartyUtils.members.asSequence()
            .filter { !it.equals(excludeName, ignoreCase = true) }
            .mapNotNull { PartyUtils.getDungeonMemberInfo(it)?.dungeonClass?.takeUnless { clazz -> clazz == DungeonClass.Empty } }
            .toMutableSet()

        selectedClass?.removeFormatting()
            ?.let(DungeonClass::fromName)
            ?.takeUnless { it == DungeonClass.Empty || mc.user.name.equals(excludeName, ignoreCase = true) }
            ?.let(classes::add)

        return classes
    }
}
