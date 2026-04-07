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
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.network.ApiUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.ConcurrentHashMap

object PartyFinder: Feature() {
    private val showJoinStats by ToggleSetting("Join Stats", true).withDescription("Prints the dungeon stats of a player that joins your party.").section("Join Stats")

    private val showLevelReq by ToggleSetting("Show Level Req", true).withDescription("Shows the red level requirement number.").section("Menu")
    private val showMissingOverlay by ToggleSetting("Show Missing Classes", true).withDescription("Shows missing classes on the head.")

    private val showTooltipStats by ToggleSetting("Show Stats", true).withDescription("Shows player stats (Cata/Secrets/PB) in tooltip.").section("Tooltip")
    private val showSecrets by ToggleSetting("Show Secrets", true).withDescription("Shows total secrets and overall secrets per run.").showIf { showTooltipStats.value }
    private val showPB by ToggleSetting("Show PB", true).withDescription("Shows Personal Best for the current floor.").showIf { showTooltipStats.value }
    private val showMissingTooltip by ToggleSetting("Show Missing List", true).withDescription("Shows the list of missing classes at the bottom of the tooltip.")

    private val autoKick by ToggleSetting("Auto Kick", false).withDescription("Automatically kick players that don't meet requirements.").section("Auto Kick")
    private val autoKickFloor by DropdownSetting("Floor", 6, listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7")).showIf { autoKick.value }
    private val masterMode by ToggleSetting("Master Mode", true).showIf { autoKick.value }
    private val informKicked by ToggleSetting("Inform Kicked", false).withDescription("Send a party chat message before kicking.").showIf { autoKick.value }
    private val noDupe by ToggleSetting("No Dupe", false).withDescription("Automatically kicks players who join with a class that is already in your party.").showIf { autoKick.value }
    private val minimumPB by ToggleSetting("Minimum PB", true).withDescription("Require players to have an S+ PB at or under the configured minimum seconds.").showIf { autoKick.value }
    private val pbLimitSeconds by SliderSetting("Minimum Seconds", 400, 60, 480, 10, suffix = "s").withDescription("Players fail if their selected-floor S+ PB is missing or slower than this many seconds.").showIf { autoKick.value && minimumPB.value }
    private val minimumSecrets by SliderSetting("Minimum Secrets", 0, 0, 200, 1, suffix = "k").withDescription("Minimum secrets in thousands.").showIf { autoKick.value }
    private val minimumSprArcher by SliderSetting("Minimum SPR Archer", 0.0, 0.0, 20.0, 0.1).withDescription("Minimum overall dungeon secrets per run for Archers.").showIf { autoKick.value }
    private val minimumSprBerserk by SliderSetting("Minimum SPR Berserk", 0.0, 0.0, 20.0, 0.1).withDescription("Minimum overall dungeon secrets per run for Berserks.").showIf { autoKick.value }
    private val minimumSprHealer by SliderSetting("Minimum SPR Healer", 0.0, 0.0, 20.0, 0.1).withDescription("Minimum overall dungeon secrets per run for Healers.").showIf { autoKick.value }
    private val minimumSprMage by SliderSetting("Minimum SPR Mage", 0.0, 0.0, 20.0, 0.1).withDescription("Minimum overall dungeon secrets per run for Mages.").showIf { autoKick.value }
    private val minimumSprTank by SliderSetting("Minimum SPR Tank", 0.0, 0.0, 20.0, 0.1).withDescription("Minimum overall dungeon secrets per run for Tanks.").showIf { autoKick.value }

    private val joinedRegex = Regex("^§dParty Finder §f> (.+?) §ejoined the dungeon group! \\(§b(\\w+) Level (\\d+)§e\\)$")
    private val kickedPlayers = mutableSetOf<String>()
    private val pendingProfiles = ConcurrentHashMap<String, Deferred<Result<JsonObject>>>()
    private val partyMemberClasses = mutableMapOf<String, DungeonClass>()
    private const val prefix = "&9AutoKick &f>"

    private val partyMembersRegex = Regex("§5 (.+)§f: §e(.+)§b \\(..(\\d+)..\\)")
    private val levelRequiredRegex = Regex("§7Dungeon Level Required: §b(\\d+)")
    private val selectedClassRegex = Regex("Currently Selected: (.+)")
    private val selectDungeonClassRegex = Regex("§7View and select a dungeon class\\.")
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

        register<WorldChangeEvent> {
            kickedPlayers.clear()
            partyMemberClasses.clear()
        }

        register<ChatMessageEvent> {
            val joinedMatch = joinedRegex.find(event.formattedText)?.destructured ?: return@register
            val name = joinedMatch.component1().removeFormatting()
            val joinedClass = DungeonClass.fromName(joinedMatch.component2())
            partyMemberClasses[name.lowercase()] = joinedClass
            if (name == mc.user.name) return@register

            if (showJoinStats.value) {
                scope.launch { printPlayerStats(name) }
            }

            if (! autoKick.value || ! PartyUtils.isLeader()) return@register

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

    private suspend fun checkPlayer(name: String, joinedClass: DungeonClass): List<String> {
        val reasons = mutableListOf<String>()
        if (name.equalsOneOf("Noamm", mc.user.name)) return reasons

        if (noDupe.value) {
            duplicateClassReason(joinedClass, currentPartyClasses(name))?.let(reasons::add)
        }

        if (! requiresProfileCheck()) return reasons

        val floor = autoKickFloor.value + 1
        val summary = loadSummary(name, floor.takeIf { minimumPB.value }, masterMode.value) ?: return reasons

        reasons += evaluate(
            summary,
            PartyFinderRuleConfig(
                floor = floor,
                masterMode = masterMode.value,
                enforcePbLimit = minimumPB.value,
                pbLimitSeconds = pbLimitSeconds.value,
                minimumSecretsThousands = minimumSecrets.value,
                minimumSecretsPerRun = minimumSecretsPerRunThreshold(joinedClass)
            )
        )

        return reasons
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
                type.startsWith("BABY_") -> "Yeti"
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

    private fun getStats(name: String, floor: Int, type: Char): String {
        val summary = getSummaryOrRequest(name, floor.takeIf { it > 0 }, type == 'M')
            ?: return "§7(Loading...)"

        return buildString {
            append("§b(§6${summary.catacombsLevel ?: "?"}§b)§r")

            if (showSecrets.value) {
                val secrets = summary.totalSecrets?.toString() ?: "?"
                val avg = summary.secretsPerRun?.toFixed(2) ?: "?"
                append(" §8[§a$secrets§8/§b$avg§8]§r")
            }

            if (showPB.value) {
                val pb = summary.floorPbMilliseconds?.let(::formatTime) ?: "N/A"
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
        val totalSeconds = milliseconds.toLong() / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        }
        else {
            String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    private fun requiresProfileCheck() = minimumPB.value || minimumSecrets.value > 0 || hasMinimumSprRequirement()

    private fun hasMinimumSprRequirement() = listOf(
        minimumSprArcher.value,
        minimumSprBerserk.value,
        minimumSprHealer.value,
        minimumSprMage.value,
        minimumSprTank.value,
    ).any { it > 0 }

    private fun minimumSecretsPerRunThreshold(dungeonClass: DungeonClass) = when (dungeonClass) {
        DungeonClass.Archer -> minimumSprArcher.value
        DungeonClass.Berserk -> minimumSprBerserk.value
        DungeonClass.Healer -> minimumSprHealer.value
        DungeonClass.Mage -> minimumSprMage.value
        DungeonClass.Tank -> minimumSprTank.value
        else -> 0.0
    }

    private fun currentPartyClasses(excludeName: String): Set<DungeonClass> {
        val classes = PartyUtils.members.asSequence()
            .filter { ! it.equals(excludeName, ignoreCase = true) }
            .mapNotNull { partyMemberClasses[it.lowercase()]?.takeUnless { clazz -> clazz == DungeonClass.Empty } }
            .toMutableSet()

        selectedClass?.removeFormatting()
            ?.let(DungeonClass::fromName)
            ?.takeUnless { it == DungeonClass.Empty || mc.user.name.equals(excludeName, ignoreCase = true) }
            ?.let(classes::add)

        return classes
    }

    private fun duplicateClassReason(joiningClass: DungeonClass, currentClasses: Collection<DungeonClass>): String? {
        if (joiningClass == DungeonClass.Empty) return null
        return if (currentClasses.any { it == joiningClass }) "Dupe(${joiningClass.name})" else null
    }

    private fun evaluate(summary: DungeonProfileSummary, config: PartyFinderRuleConfig): List<String> {
        val reasons = mutableListOf<String>()

        if (config.enforcePbLimit) {
            val pbLimit = formatTime(config.pbLimitSeconds * 1000L)
            val pbMilliseconds = summary.floorPbMilliseconds

            if (pbMilliseconds == null) {
                reasons.add("PB(No S+/$pbLimit)")
            }
            else if (pbMilliseconds / 1000 > config.pbLimitSeconds) {
                val floorPrefix = if (config.masterMode) "M" else "F"
                reasons.add("$floorPrefix${config.floor}: PB(${formatTime(pbMilliseconds)}/$pbLimit)")
            }
        }

        if (config.minimumSecretsThousands > 0) {
            val secrets = summary.totalSecrets ?: 0
            if (secrets < config.minimumSecretsThousands * 1000) {
                reasons.add("Secrets(${secrets / 1000}k/${config.minimumSecretsThousands}k)")
            }
        }

        if (config.minimumSecretsPerRun > 0) {
            val secretsPerRun = summary.secretsPerRun ?: 0.0
            if (secretsPerRun < config.minimumSecretsPerRun) {
                reasons.add("SPR(${secretsPerRun.toFixed(2)}/${config.minimumSecretsPerRun.toFixed(2)})")
            }
        }

        return reasons
    }

    private fun getSummaryOrRequest(playerName: String, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary? {
        return getCachedSummary(playerName, floor, masterMode) ?: run {
            requestProfile(playerName)
            null
        }
    }

    private fun getCachedSummary(playerName: String, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary? {
        val key = cacheKey(playerName)
        val profile = ProfileCache.getFromCache(key) ?: return null
        return summarize(profile, floor, masterMode)
    }

    private suspend fun loadSummary(playerName: String, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary? {
        return loadProfile(playerName).getOrNull()?.let { summarize(it, floor, masterMode) }
    }

    private suspend fun loadProfile(playerName: String): Result<JsonObject> {
        val key = cacheKey(playerName)
        ProfileCache.getFromCache(key)?.let { return Result.success(it) }
        return pendingProfiles[key]?.await() ?: requestProfile(playerName).await()
    }

    private fun requestProfile(playerName: String): Deferred<Result<JsonObject>> {
        val cleanName = cleanName(playerName)
        val key = cleanName.lowercase()

        ProfileCache.getFromCache(key)?.let { return CompletableDeferred(Result.success(it)) }

        return pendingProfiles.computeIfAbsent(key) {
            scope.async {
                try {
                    ProfileUtils.getProfile(cleanName)
                }
                finally {
                    pendingProfiles.remove(key)
                }
            }
        }
    }

    private fun summarize(profile: JsonObject, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary {
        val dungeons = profile.getObj("dungeons")
        val totalSecrets = dungeons?.getInt("secrets")
        val totalRuns = extractTotalRuns(dungeons)
        val selectedMode = if (masterMode) dungeons?.getObj("master_catacombs") else dungeons?.getObj("catacombs")

        return DungeonProfileSummary(
            catacombsLevel = dungeons?.getDouble("catacombs_experience")?.let(ApiUtils::getCatacombsLevel),
            totalSecrets = totalSecrets,
            totalRuns = totalRuns,
            secretsPerRun = when {
                totalRuns == null -> null
                totalRuns == 0 -> 0.0
                totalSecrets == null -> null
                else -> totalSecrets.toDouble() / totalRuns.toDouble()
            },
            floorPbMilliseconds = floor?.takeIf { it > 0 }
                ?.let { selectedMode?.getObj("fastest_time_s_plus")?.getInt("$it") },
        )
    }

    private fun cacheKey(playerName: String) = cleanName(playerName).lowercase()

    private fun cleanName(playerName: String) = playerName.removeFormatting()

    private fun extractTotalRuns(dungeons: JsonObject?): Int? {
        if (dungeons == null) return null

        val normalRuns = extractTierCompletionTotal(dungeons.getObj("catacombs")?.getObj("tier_completions"))
        val masterRuns = extractTierCompletionTotal(dungeons.getObj("master_catacombs")?.getObj("tier_completions"))

        return if (normalRuns != null || masterRuns != null) {
            (normalRuns ?: 0) + (masterRuns ?: 0)
        }
        else dungeons.getInt("total_runs")
    }

    private fun extractTierCompletionTotal(completions: JsonObject?): Int? {
        if (completions == null) return null

        completions.getInt("total")?.let { return it }

        val perFloorValues = completions.entries
            .mapNotNull { (key, value) -> key.toIntOrNull()?.let { value.jsonPrimitive.content.toIntOrNull() } }

        return perFloorValues.takeIf { it.isNotEmpty() }?.sum()
    }
}

data class PartyFinderRuleConfig(
    val floor: Int,
    val masterMode: Boolean,
    val enforcePbLimit: Boolean = true,
    val pbLimitSeconds: Int,
    val minimumSecretsThousands: Int = 0,
    val minimumSecretsPerRun: Double = 0.0,
)

data class DungeonProfileSummary(
    val catacombsLevel: Int?,
    val totalSecrets: Int?,
    val totalRuns: Int?,
    val secretsPerRun: Double?,
    val floorPbMilliseconds: Int?,
)


