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
import com.github.noamm9.utils.JsonUtils.getDouble
import com.github.noamm9.utils.JsonUtils.getInt
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.network.ApiUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.util.*

object PartyFinder: Feature() {
    private val showLevelReq by ToggleSetting("Show Level Req", true).withDescription("Shows the red level requirement number.").section("Menu")
    private val showMissingOverlay by ToggleSetting("Show Missing Classes", true).withDescription("Shows missing classes on the head.")

    private val showTooltipStats by ToggleSetting("Show Stats", true).withDescription("Shows player stats (Cata/Secrets/PB) in tooltip.").section("Tooltip")
    private val showSecrets by ToggleSetting("Show Secrets", true).withDescription("Shows Total Secrets and Average.").showIf { showTooltipStats.value }
    private val showPB by ToggleSetting("Show PB", true).withDescription("Shows Personal Best for the current floor.").showIf { showTooltipStats.value }
    private val showMissingTooltip by ToggleSetting("Show Missing List", true).withDescription("Shows the list of missing classes at the bottom of the tooltip.")

    private val autoKick by ToggleSetting("Auto Kick", false).withDescription("Automatically kick players that don't meet requirements.").section("Auto Kick")
    private val autoKickFloor by DropdownSetting("Floor", 6, listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7")).showIf { autoKick.value }
    private val masterMode by ToggleSetting("Master Mode", true).showIf { autoKick.value }
    private val informKicked by ToggleSetting("Inform Kicked", false).withDescription("Send a party chat message before kicking.").showIf { autoKick.value }
    private val maximumSeconds by SliderSetting("Maximum Seconds", 400, 60, 480, 10, suffix = "s").withDescription("Maximum S+ PB time in seconds.").showIf { autoKick.value }
    private val minimumSecrets by SliderSetting("Minimum Secrets", 0, 0, 200, 1, suffix = "k").withDescription("Minimum secrets in thousands.").showIf { autoKick.value }

    private val joinedRegex = Regex("^§dParty Finder §f> (.+?) §ejoined the dungeon group! \\(§b(\\w+) Level (\\d+)§e\\)$")
    private val kickedPlayers = mutableSetOf<String>()
    private const val prefix = "&9AutoKick &f>"

    private val partyMembersRegex = Regex("§5 (.+)§f: §e(.+)§b \\(..(\\d+)..\\)")
    private val levelRequiredRegex = Regex("§7Dungeon Level Required: §b(\\d+)")
    private val selectedClassRegex = Regex("Currently Selected: (.+)")
    private val selectDungeonClassRegex = Regex("§7View and select a dungeon class\\.")
    private val classNames = listOf("&4&lArcher", "&a&lTank", "&6&lBerserk", "&5&lHealer", "&b&lMage")
    private var selectedClass: String? = null
    private var inPartyFinder = false

    private val pendingRequests = Collections.synchronizedSet(HashSet<String>())

    private val headSlots = setOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 44
    )

    override fun init() {
        register<ContainerEvent.Render.Slot.Post> {
            if (! inPartyFinder) return@register
            if (event.screen.title.string != "Party Finder") return@register
            if (! showLevelReq.value && ! showMissingOverlay.value) return@register
            if (event.slot.index !in headSlots) return@register
            val item = event.slot.item.takeUnless { it.isEmpty || ! it.`is`(Blocks.PLAYER_HEAD.asItem()) } ?: return@register

            val classes = mutableListOf<String>()
            var levelRequired = 0

            for (line in item.lore) when {
                line.contains("Dungeon Level Required:") && showLevelReq.value -> levelRequired = levelRequiredRegex.find(line)?.groupValues?.get(1)?.toInt() ?: 0
                partyMembersRegex.matches(line) && showMissingOverlay.value -> classes.add(partyMembersRegex.matchEntire(line) !!.destructured.component2())
            }

            event.context.pose().translate(event.slot.x.toFloat(), event.slot.y.toFloat())

            if (levelRequired != 0) {
                val str = "&c$levelRequired"
                Render2D.drawString(event.context, str, 16f - str.width() * 0.6f, 0f, scale = 0.6f)
            }

            if (showMissingOverlay.value) {
                val missingClasses = classNames.filter { classes.indexOf(it.removeFormatting()) == - 1 }.map { it.take(5) }
                val missing = listOf(
                    missingClasses.take(2).joinToString(""),
                    missingClasses.drop(2).take(2).joinToString("")
                ).filter { it.isNotBlank() }

                for ((i, line) in missing.withIndex()) {
                    Render2D.drawString(
                        event.context,
                        line,
                        0,
                        10f - if (i == 1) 6f else 0f,
                        scale = 0.65
                    )
                }
            }

            event.context.pose().translate(- event.slot.x.toFloat(), - event.slot.y.toFloat())
        }

        register<ContainerEvent.Render.Tooltip> {
            if (! inPartyFinder) return@register
            if (event.screen.title.string != "Party Finder") return@register
            if (! event.stack.`is`(Items.PLAYER_HEAD)) return@register
            if (event.screen.menu.slots.find { it.item == event.stack }?.index !in headSlots) return@register

            var floor = 0
            var type = 'F'

            val remainingClasses = classNames.map { it.removeFormatting() }.toMutableList()

            event.lore.toList().forEachIndexed { index, comp ->
                val line = comp.formattedText

                if (line.removeFormatting().contains("Dungeon: Master Mode")) type = 'M'
                if (line.contains("§7Floor: §bFloor ")) floor = line.split(" ").last().let { it.toIntOrNull() ?: it.romanToDecimal() }

                partyMembersRegex.matchEntire(line)?.destructured?.let { (pName, cName, cLvl) ->
                    val playerName = pName.removeFormatting()
                    val className = cName.removeFormatting()
                    val level = cLvl.toInt()
                    val color = getColor(level)

                    val stats = if (showTooltipStats.value) getStats(playerName, floor, type) else ""

                    event.lore[index] = Component.literal(" $pName: §e$className $color$level $stats".addColor())
                    remainingClasses.remove(className)
                }
            }

            if (selectedClass?.removeFormatting() in remainingClasses) {
                val idx = remainingClasses.indexOf(selectedClass?.removeFormatting())
                remainingClasses[idx] = "$selectedClass§7"
            }

            if (showMissingTooltip.value) {
                event.lore.add(Component.literal("§cMissing: §7" + remainingClasses.joinToString(", ") { it.addColor() }))
            }
        }

        register<ContainerFullyOpenedEvent> {
            if (event.title.string == "Catacombs Gate") {
                event.items[45]?.lore?.takeIf { it.size > 3 && it[0].matches(selectDungeonClassRegex) }?.run {
                    selectedClassRegex.matchEntire(get(2).removeFormatting())?.destructured?.run {
                        selectedClass = classNames[classNames.map { it.removeFormatting() }.indexOf(component1())]
                    }
                }
            }
            else if (event.title.string == "Party Finder") {
                event.items[50]?.takeIf { it.`is`(Items.NETHER_STAR) }?.lore[5]?.let {
                    if (! it.contains("§aCombat Level: ")) {
                        inPartyFinder = true
                    }
                }
            }
        }

        register<ContainerEvent.Close> { inPartyFinder = false }

        register<WorldChangeEvent> { kickedPlayers.clear() }

        register<ChatMessageEvent> {
            if (! autoKick.value || ! PartyUtils.isLeader()) return@register
            val name = joinedRegex.find(event.formattedText)?.destructured?.component1() ?: return@register

            if (name in kickedPlayers) {
                ChatUtils.modMessage("$prefix &cAuto-kicking &e$name &c(previously kicked)")
                ThreadUtils.scheduledTask(6) { ChatUtils.sendCommand("party kick $name") }
                return@register
            }

            scope.launch {
                val reasons = checkPlayer(name.removeFormatting()).takeUnless { it.isEmpty() } ?: return@launch
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

    private suspend fun checkPlayer(name: String): List<String> {
        val reasons = mutableListOf<String>()
        if (name.equalsOneOf("Noamm", mc.user.name)) return reasons
        val profile = ProfileUtils.getProfile(name).getOrNull() ?: return reasons

        val dungeons = profile.getObj("dungeons") ?: return reasons
        val floor = autoKickFloor.value + 1
        val dungeonType = if (masterMode.value) dungeons.getObj("master_catacombs") else dungeons.getObj("catacombs")

        val floorPrefix = if (masterMode.value) "M" else "F"
        val pbReq = formatTime(maximumSeconds.value * 1000)
        val pb = dungeonType?.getObj("fastest_time_s_plus")?.getInt("$floor")?.div(1000)
        if (pb == null) reasons.add("PB(No S+/$pbReq)")
        else if (pb / 1000 > maximumSeconds.value) {
            reasons.add("$floorPrefix$floor: PB(${formatTime(pb)}/$pbReq)")
        }

        if (minimumSecrets.value > 0) {
            val secrets = dungeons.getInt("secrets") ?: 0
            if (secrets < minimumSecrets.value * 1000) {
                reasons.add("Secrets(${secrets / 1000}k/${minimumSecrets.value}k)")
            }
        }

        return reasons
    }

    private fun getStats(name: String, floor: Int, type: Char): String {
        val key = name.removeFormatting().uppercase()
        val cachedData = ProfileCache.getFromCache(key)

        if (cachedData == null) {
            if (! pendingRequests.contains(key) && pendingRequests.size < 5) {
                pendingRequests.add(key)

                scope.launch {
                    try {
                        ProfileUtils.getProfile(key).getOrThrow()
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                    finally {
                        pendingRequests.remove(key)
                    }
                }
            }
            return "§7(Loading...)"
        }

        val dungeons = cachedData.getObj("dungeons")
        val catacombs = dungeons?.getObj("catacombs")
        val masterCatacombs = dungeons?.getObj("master_catacombs")

        val cataLvl = dungeons?.getDouble("catacombs_experience")?.let { ApiUtils.getCatacombsLevel(it) } ?: "?"

        return buildString {
            append("§b(§6$cataLvl§b)§r")

            if (showSecrets.value) {
                val totalSecrets = dungeons?.getInt("secrets")?.toDouble() ?: .0
                val totalRuns = dungeons?.getInt("total_runs")?.toDouble() ?: .0
                val secretAvg = if (totalRuns > 0) (totalSecrets / totalRuns).toFixed(2) else "0.00"
                val showSecretsVal = totalSecrets != .0
                val secrets = if (showSecretsVal) totalSecrets.toInt() else "?"
                val avg = if (showSecretsVal) secretAvg else "?"

                append(" §8[§a$secrets§8/§b$avg§8]§r")
            }

            if (showPB.value) {
                val pbObj = if (type == 'F') catacombs else masterCatacombs
                val pbTime = pbObj?.getObj("fastest_time_s_plus")?.getInt("$floor")
                val pb = pbTime?.let(::formatTime) ?: "N/A"
                append(" §8[§9$pb§8]§r")
            }
        }
    }


    private fun getColor(level: Int) = when {
        level >= 50 -> "§c§l"
        level >= 45 -> "§c"
        level >= 40 -> "§6"
        level >= 35 -> "§d"
        level >= 30 -> "§9"
        level >= 25 -> "§b"
        level >= 20 -> "§2"
        level >= 15 -> "§a"
        level >= 10 -> "§e"
        level >= 5 -> "§f"
        else -> "§7"
    }

    private fun formatTime(milliseconds: Number): String {
        val totalSecs = milliseconds.toLong() / 1000
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60

        return buildList {
            if (h > 0) add(h)
            if (m > 0) add(m)
            if (s > 0) add(s)
        }.joinToString(":")
    }
}