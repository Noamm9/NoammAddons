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
import com.github.noamm9.utils.JsonUtils.getArray
import com.github.noamm9.utils.JsonUtils.getDouble
import com.github.noamm9.utils.JsonUtils.getInt
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.JsonUtils.getString
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.TabListUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.Utils.uppercaseFirst
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.network.ApiUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.util.*

object PartyFinder: Feature() {
    private val showJoinStats by ToggleSetting("Join Stats", true).withDescription("Prints the dungeon stats of a player that joins your party.").section("Join Stats")

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

    private val dungeonGroupJoinRegex = Regex("^Party Finder > (\\w{1,16}) joined the dungeon group! \\((\\w+) Level (\\d+)\\)$")
    private val kickedPlayers = mutableSetOf<String>()

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

                    val stats = if (showTooltipStats.value) getLoreStats(playerName, floor, type) else ""

                    event.lore[index] = Component.literal(" $pName: §e$className $color$level $stats".addColor())
                    remainingClasses.remove(className)
                }
            }

            if (selectedClass?.removeFormatting() in remainingClasses) {
                val idx = remainingClasses.indexOf(selectedClass?.removeFormatting())
                remainingClasses[idx] = "${selectedClass}§7"
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
            if (! autoKick.value && ! showJoinStats.value) return@register
            val name = dungeonGroupJoinRegex.find(event.unformattedText)?.groupValues?.getOrNull(1) ?: return@register
            if (name == mc.user.name) return@register

            scope.launch {
                if (autoKick.value && PartyUtils.isLeader()) autoKickPlayer(name)
                if (showJoinStats.value) printPlayerStats(name)
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("pfs").executes {
                    scope.launch { printPlayerStats(mc.user.name) }
                    1
                }.then(ClientCommandManager.argument("ign", StringArgumentType.word())
                    .suggests { _, builder ->
                        val players = TabListUtils.getTabList().mapNotNull { it.second.profile.name }.filterNot { it.matches("^![A-Z]-[a-z]$".toRegex()) }
                        players.forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }
                    .executes { context ->
                        val ign = StringArgumentType.getString(context, "ign")
                        scope.launch { printPlayerStats(ign) }
                        1
                    }
                )
            )
        }
    }

    private suspend fun printPlayerStats(name: String) {
        val cleanName = name.removeFormatting()

        val data = ProfileUtils.getProfile(cleanName).getOrNull() ?: return
        val dungeons = data.getObj("dungeons") ?: return
        val catacombs = dungeons.getObj("catacombs")
        val masterCatacombs = dungeons.getObj("master_catacombs")

        val selectedArrow = data.getString("favorite_arrow")?.lowercase()?.split("_")?.joinToString(" ") { it.uppercaseFirst() }
        val powerStone = data.getString("selected_power")?.lowercase()?.split("_")?.joinToString(" ") { it.uppercaseFirst() }

        val bloodMobsKilled = data.getInt("blood_mobs_killed") ?: 0
        val totalSecrets = dungeons.getInt("secrets")?.toDouble() ?: .0
        val totalRuns = dungeons.getInt("total_runs")?.toDouble() ?: .0
        val secretAvg = if (totalRuns > 0) totalSecrets / totalRuns else .0

        val cataLvl = dungeons.getDouble("catacombs_experience")?.let(ApiUtils::getCatacombsLevel) ?: return
        val classAvg = dungeons.getObj("player_classes")?.values
            ?.mapNotNull { element -> runCatching { ApiUtils.getCatacombsLevel(element.jsonPrimitive.double) }.getOrNull()?.toDouble() }
            ?.takeUnless(Collection<*>::isEmpty)
            ?.average()
            ?: return

        val pets = data.getArray("pets")?.mapNotNull { element ->
            val petObject = element.jsonObject
            val tier = petObject.getString("tier") ?: return@mapNotNull null
            val type = petObject.getString("type") ?: return@mapNotNull null
            val rarity = runCatching { ItemRarity.valueOf(tier) }.getOrNull() ?: return@mapNotNull null
            val formattedType = when {
                type.endsWith("_DRAGON") -> if (type.startsWith("GOLDEN")) "Gdrag" else "Edrag"
                else -> type.lowercase().split("_").joinToString(" ") { it.uppercaseFirst() }
            }
            rarity.baseColor.toString() to formattedType
        }?.toSet().orEmpty()

        val inventoryApiEnabled = ! data.getString("talisman_bag_data").isNullOrBlank()
        val talismanBag = data.getString("talisman_bag_data")?.let(ApiUtils::decodeBase64ItemList)
        val magicalPower = talismanBag?.let { ApiUtils.getMagicalPower(it, data) }
        val armorItems = data.getString("armor_data")?.let(ApiUtils::decodeBase64ItemList)
        val armorComponents = armorItems.orEmpty().reversed().map { item ->
            val displayName = item.hoverName.formattedText
            val hoverText = buildList {
                add(displayName)
                addAll(item.lore)
            }.joinToString("\n")

            Component.literal("  $displayName".addColor()).withStyle {
                it.withHoverEvent(HoverEvent.ShowText(Component.literal(hoverText.addColor())))
            }
        }

        val completionComponents = listOfNotNull(
            catacombs?.getObj("tier_completions")?.let { completionObj ->
                val highestFloor = completionObj.keys.mapNotNull(String::toIntOrNull).maxOrNull() ?: return@let null
                val header = "  &aFloor Completions &7(F$highestFloor): ${catacombs.getObj("fastest_time_s_plus")?.getInt("$highestFloor")?.let(::formatTime) ?: "N/A"}"
                val hover = (0 .. highestFloor).joinToString("\n") { floor ->
                    val completions = completionObj.getInt("$floor")
                    val pb = catacombs.getObj("fastest_time_s_plus")?.getInt("$floor")?.let(::formatTime) ?: "&cNo Comp"
                    val floorName = if (floor == 0) "Entrance" else floor.toString()
                    "&2&l*&a Floor $floorName: ${completions?.let { "&e$it &7(&6S+ &e$pb&7)" } ?: "&cDNF"}"
                }

                Component.literal(header.addColor()).withStyle {
                    it.withHoverEvent(HoverEvent.ShowText(Component.literal(hover.addColor())))
                }
            },
            masterCatacombs?.getObj("tier_completions")?.let { completionObj ->
                val highestFloor = completionObj.keys.mapNotNull(String::toIntOrNull).maxOrNull() ?: return@let null
                val header = "  &l&4Master Completions &7(M$highestFloor): ${masterCatacombs.getObj("fastest_time_s_plus")?.getInt("$highestFloor")?.let(::formatTime) ?: "N/A"}"
                val hover = (1 .. highestFloor).joinToString("\n") { floor ->
                    val completions = completionObj.getInt("$floor")
                    val pb = masterCatacombs.getObj("fastest_time_s_plus")?.getInt("$floor")?.let(::formatTime) ?: "&cNo Comp"
                    "&c&l*&4 Floor $floor: ${completions?.let { "&e$it &7(&6S+ &e$pb&7)" } ?: "&cDNF"}"
                }

                Component.literal(header.addColor()).withStyle {
                    it.withHoverEvent(HoverEvent.ShowText(Component.literal(hover.addColor())))
                }
            }
        )

        ChatUtils.chat("&a&l--- &r&b$cleanName&b's Dungeon Stats &a&l---&r")
        ChatUtils.chat(
            "  &4Cata Level: &b$cataLvl &f| &dClass Avg: &b${classAvg.toFixed(1)} &f| &dMP: ${
                if (! inventoryApiEnabled || magicalPower == null) "&cAPI off" else "&e$magicalPower"
            }"
        )
        ChatUtils.chat("  &bSecrets: &d${totalSecrets.toInt()} &b(&d${secretAvg.toFixed(2)}&b) &f| &cBlood Mobs: &f$bloodMobsKilled")
        ChatUtils.chat("  Pets: ${if (pets.isEmpty()) "&cNone" else pets.joinToString("&7,&r ") { it.first + it.second }}")
        ChatUtils.chat("  &9Power Stone: ${powerStone?.let { "&e$it" } ?: "&cUnknown"} &f| &6Arrows: ${selectedArrow?.let { "&e$it" } ?: "&cUnknown"}")
        ChatUtils.chat(" ")

        when {
            ! inventoryApiEnabled -> ChatUtils.chat("  &cInventory API is Off")
            armorComponents.isEmpty() -> ChatUtils.chat("  &cArmor data unavailable")
            else -> armorComponents.forEach(ChatUtils::chat)
        }

        if (completionComponents.isNotEmpty()) {
            ChatUtils.chat(" ")
            completionComponents.forEach(ChatUtils::chat)
        }

        ChatUtils.chat("&a&l------------------------------------&r")
    }

    private suspend fun autoKickPlayer(name: String) {
        if (name in kickedPlayers) {
            ChatUtils.modMessage("&9AutoKick &f> &cAuto-kicking &e$name &c(previously kicked)")
            ThreadUtils.scheduledTask(6) { ChatUtils.sendCommand("party kick $name") }
            return
        }

        val reasons = mutableListOf<String>().apply {
            if (name.equalsOneOf("Noamm", mc.user.name)) return@apply
            val profile = ProfileUtils.getProfile(name).getOrNull() ?: return@apply

            val dungeons = profile.getObj("dungeons") ?: return@apply
            val floor = autoKickFloor.value + 1
            val dungeonType = if (masterMode.value) dungeons.getObj("master_catacombs") else dungeons.getObj("catacombs")

            val floorPrefix = if (masterMode.value) "M" else "F"
            val pbReq = formatTime(maximumSeconds.value * 1000)
            val pb = dungeonType?.getObj("fastest_time_s_plus")?.getInt("$floor")?.div(1000)
            if (pb == null) add("PB(No S+/$pbReq)")
            else if (pb > maximumSeconds.value) {
                add("$floorPrefix$floor: PB(${formatTime(pb)}/$pbReq)")
            }

            if (minimumSecrets.value > 0) {
                val secrets = dungeons.getInt("secrets") ?: 0
                if (secrets < minimumSecrets.value * 1000) {
                    add("Secrets(${secrets / 1000}k/${minimumSecrets.value}k)")
                }
            }

            if (isEmpty()) return
        }

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

    private fun getLoreStats(name: String, floor: Int, type: Char): String {
        val key = name.removeFormatting().uppercase()
        val cachedData = ProfileCache.getFromCache(key)

        if (cachedData == null) {
            if (key !in pendingRequests && pendingRequests.size < 5) {
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
                val showSecrets = totalSecrets != .0
                val secrets = if (showSecrets) totalSecrets.toInt() else "?"
                val avg = if (showSecrets) secretAvg else "?"

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
        val m = (totalSecs % 3600) / 60
        val s = (totalSecs % 60).toString().padStart(2, '0')
        return "$m:$s"
    }
}