package noammaddons.features.impl.dungeons

import gg.essential.universal.UChat
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.CHAT_PREFIX
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.gui.Menus.impl.CustomPartyFinderMenu
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.JsonUtils.getArray
import noammaddons.utils.JsonUtils.getDouble
import noammaddons.utils.JsonUtils.getInt
import noammaddons.utils.JsonUtils.getObj
import noammaddons.utils.JsonUtils.getString
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ProfileUtils.getCatacombsLevel
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.uppercaseFirst
import kotlin.math.abs


// todo pf join stats message and autokick?
object PartyFinder: Feature("A group of many features regarding the dungeon ape finder") {
    val customMenu = ToggleSetting("Enabled")
    val customMenuShowStats = ToggleSetting("Show Players Stats").addDependency(customMenu)
    private val partyFinderStats = ToggleSetting("Party Finder Stats")
    private val reformatPfMessages = ToggleSetting("Cleaner Messages")
    private val joinedSound = ToggleSetting("Join Sound")

    private val autoKick = ToggleSetting("Enabled ")
    private val autoKickFloors = listOf("F7", "M4", "M5", "M6", "M7")
    private val autoKickFloor = DropdownSetting("Selected Floor", autoKickFloors).addDependency(autoKick)
    private val autoKickReasons = MultiCheckboxSetting(
        "Kick Reasons", mapOf(
            "Personal Best" to false, "Secrets Average" to false, "Spirit Pet" to false
        )
    ).addDependency(autoKick)
    private val pbReq = TextInputSetting("PB Requirement", "5:30").addDependency(autoKick).hideIf { autoKickReasons.value["Personal Best"] != true }
    private val secretsReq = TextInputSetting("Secrets Average Requirement", "6.0").addDependency(autoKick).hideIf { autoKickReasons.value["Secrets Average"] != true }

    override fun init() = addSettings(
        SeperatorSetting("Custom Menu"),
        customMenu, customMenuShowStats,
        SeperatorSetting("Auto Kick"),
        autoKick, autoKickFloor, autoKickReasons, pbReq, secretsReq,
        SeperatorSetting("Misc"),
        partyFinderStats, reformatPfMessages, joinedSound
    )

    private val joinedRegex = Regex("^&dParty Finder &r&f> (.+?) &r&ejoined the dungeon group! \\(&r&b(\\w+) Level (\\d+)&r&e\\)&r\$".addColor())
    private val playerClassChangeRegex = Regex("^&dParty Finder &r&f> (.+?) &r&eset their class to &r&b(\\w+) Level (\\d+)&r&e!&r\$".addColor())
    private val messageReplacements = mapOf(
        "Party Finder > Your party has been queued in the dungeon finder!" to "&d&lPF > &aParty Queued.",
        "Party Finder > Your group has been de-listed!" to "&d&lPF > &aParty Delisted."
    )

    override fun onEnable() {
        super.onEnable()
        MinecraftForge.EVENT_BUS.register(CustomPartyFinderMenu)
    }

    override fun onDisable() {
        super.onDisable()
        MinecraftForge.EVENT_BUS.unregister(CustomPartyFinderMenu)
    }

    @SubscribeEvent
    fun onChat(event: Chat) = ThreadUtils.runOnNewThread {
        val msg = event.component.formattedText
        val playerFormatted = joinedRegex.find(msg)?.destructured?.component1() ?: return@runOnNewThread
        val playerUnformatted = playerFormatted.removeFormatting()
        if (joinedSound.value) SoundUtils.Pling()
        if (partyFinderStats.value && playerUnformatted != mc.session.username) printPlayerStats(playerFormatted)
        if (autoKick.value && ! playerUnformatted.equalsOneOf("Noamm", mc.session.username)) autoKickPlayer(playerUnformatted, playerFormatted)
    }

    @SubscribeEvent
    fun onNewChatMessage(event: AddMessageToChatEvent) {
        if (! reformatPfMessages.value) return
        val text = event.component.formattedText

        when {
            "joined the dungeon group!" in text -> {
                val (playerFormatted, clazz, level) = joinedRegex.find(text)?.destructured ?: return
                val unformattedPlayer = playerFormatted.removeFormatting()

                val baseComp = ChatComponentText("$CHAT_PREFIX &d&lPF > $playerFormatted &8| &b$clazz $level".addColor())

                if (unformattedPlayer != mc.session.username) listOf(
                    createComponent(" &8| &c[Kick]", "/p kick $unformattedPlayer", "&c/p kick $unformattedPlayer"),
                    createComponent(" &7[Block]", "/block add $unformattedPlayer", "&7/block add $unformattedPlayer"),
                    createComponent(" &d[PV]", "/pv $unformattedPlayer", "&d/pv $unformattedPlayer")
                ).forEach(baseComp::appendSibling)

                mc.thePlayer?.addChatMessage(baseComp)
                event.isCanceled = true
            }

            "set their class to" in text -> {
                val (player, clazz, level) = playerClassChangeRegex.find(text)?.destructured ?: return
                modMessage("&d&lPF > &r$player &echanged to &b$clazz $level&e!")
                event.isCanceled = true
            }

            else -> messageReplacements[text.removeFormatting()]?.let {
                event.isCanceled = true
                modMessage(it)
            }
        }
    }

    private fun createComponent(text: String, command: String, hoverText: String): ChatComponentText {
        return ChatComponentText(text.addColor()).apply {
            chatStyle = ChatStyle().apply {
                chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
                chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText.addColor()))
            }
        }
    }

    @SubscribeEvent
    fun cmd(event: MessageSentEvent) {
        if (event.message.startsWith("/pfs")) {
            event.isCanceled = true
            printPlayerStats(event.message.removePrefix("/pfs").trim().ifEmpty { mc.session.username })
        }
    }

    private fun printPlayerStats(name: String) = scope.launch {
        val data = ProfileUtils.getDungeonStats(name.removeFormatting()) ?: return@launch
        val dungeons = data["dungeons"]?.jsonObject ?: return@launch
        val catacombs = dungeons.getObj("catacombs")
        val master_catacombs = dungeons.getObj("master_catacombs")

        val selectedArrow = data.getString("favorite_arrow")?.uppercaseFirst() ?: return@launch
        val powerStone = data.getString("selected_power")?.uppercaseFirst() ?: return@launch

        val bloodMobsKilled = data.getInt("blood_mobs_killed") ?: return@launch

        val totalSecrets = dungeons.getInt("secrets")?.toDouble() ?: return@launch
        val totalRuns = dungeons.getInt("total_runs")?.toDouble() ?: .0
        val secretAvg = totalSecrets / totalRuns

        val cataLvl = dungeons.getDouble("catacombs_experience")?.let { getCatacombsLevel(it) } ?: return@launch
        val classAvg = dungeons.getObj("player_classes")?.values?.takeUnless(Collection<*>::isEmpty)?.let {
            it.sumOf { clazzElement -> getCatacombsLevel(clazzElement.jsonPrimitive.double) } / it.size.toDouble()
        } ?: return@launch

        val petsList = data.getArray("pets")?.mapNotNull { element ->
            val petObject = element.jsonObject
            val tier = petObject.getString("tier") ?: return@mapNotNull null
            val type = petObject.getString("type") ?: return@mapNotNull null

            val rarityColor = runCatching { ItemUtils.ItemRarity.valueOf(tier).baseColor }.getOrNull() ?: return@mapNotNull null

            val formattedType = when {
                type.endsWith("_DRAGON") -> if (type.startsWith("GOLDEN")) "Gdrag" else "Edrag"
                type.startsWith("BABY_") -> "Yeti"
                else -> type.lowercase().split("_").joinToString(" ") { word -> word.uppercaseFirst() }
            }

            rarityColor to formattedType
        }?.toSet() ?: return@launch

        val talismenBag = data.getString("talisman_bag_data")?.let(ItemUtils::decodeBase64ItemList) ?: return@launch
        val magicalPower = ProfileUtils.getMagicalPower(talismenBag, data)
        val armorInv = data.getString("armor_data")?.let(ItemUtils::decodeBase64ItemList) ?: return@launch

        val armorList = armorInv.filterNotNull().reversed().map { armorPiece ->
            val lore = armorPiece.lore.toMutableList().apply { add(0, armorPiece.displayName) }
            ChatComponentText("  " + armorPiece.displayName).apply {
                chatStyle = ChatStyle().apply {
                    chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(lore.joinToString("\n")))
                }
            }
        }

        val completionsList = listOfNotNull(
            catacombs?.getObj("tier_completions")?.keys?.mapNotNull(String::toIntOrNull)?.maxOrNull()?.let { highestFloor ->
                val completionObj = catacombs.getObj("tier_completions")
                val highestFloorPb = "§7(F$highestFloor): ${formatPb(catacombs.getObj("fastest_time_s_plus")?.getInt("$highestFloor") ?: "N/A")}"

                ChatComponentText("  §aFloor Completions $highestFloorPb").apply {
                    chatStyle = ChatStyle().apply {
                        chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText((0 .. highestFloor).joinToString("\n") { floor ->
                            "§2§l●§a Floor ${if (floor == 0) "Entrance" else floor}: §e${
                                completionObj?.getInt("$floor")?.let { completions ->
                                    "$completions §7(§6S+ §e${
                                        formatPb(catacombs.getObj("fastest_time_s_plus")?.getInt("$floor") ?: "§cNo Comp")
                                    }§7)"
                                } ?: "§cDNF"
                            }"
                        }))
                    }
                }
            },
            master_catacombs?.getObj("tier_completions")?.keys?.mapNotNull(String::toIntOrNull)?.maxOrNull()?.let { highestFloor ->
                val masterCompletionObj = master_catacombs.getObj("tier_completions")
                val highestFloorPb = "§7(M$highestFloor): ${formatPb(master_catacombs.getObj("fastest_time_s_plus")?.getInt("$highestFloor") ?: "N/A")}"

                ChatComponentText("  §l§4Master Completions $highestFloorPb").apply {
                    chatStyle = ChatStyle().apply {
                        chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText((1 .. highestFloor).joinToString("\n") { floor ->
                            "§c§l●§4 Floor $floor: §e${
                                masterCompletionObj?.getInt("$floor")?.let { completions ->
                                    "$completions §7(§6S+ §e${
                                        formatPb(master_catacombs.getObj("fastest_time_s_plus")?.getInt("$floor") ?: "§cNo Comp")
                                    }§7)"
                                } ?: "§cDNF"
                            }"
                        }))
                    }
                }
            }
        )

        ThreadUtils.scheduledTask {
            UChat.chat("§a§l--- §r§b$name§b's Dungeon Stats §a§l---§r")
            UChat.chat("  §4Cata Level: §b$cataLvl §f| §dClass Avg: §b${"%.1f".format(classAvg)} §f| §dMP: §e$magicalPower")
            UChat.chat("  §bSecrets: §d${totalSecrets.toInt()} §b(§d${"%.2f".format(secretAvg)}§b) §f| §cBlood Mobs: §f$bloodMobsKilled")
            UChat.chat("  Pets: ${petsList.joinToString("§7,§r ") { it.first + it.second }}")
            UChat.chat("  §9Power Stone: &e$powerStone §f| §6Arrows: §e$selectedArrow")
            UChat.chat(" ")
            armorList.forEach(UChat::chat)
            UChat.chat(" ")
            completionsList.forEach(UChat::chat)
            UChat.chat("§a§l------------------------------------§r")
        }
    }

    private fun autoKickPlayer(name: String, playerFormatted: String) {
        val data = ProfileUtils.getDungeonStats(name) ?: return
        val dungeons = data.getObj("dungeons")
        val catacombs = dungeons?.getObj("catacombs")
        val master_catacombs = dungeons?.getObj("master_catacombs")

        val secretAvg = dungeons.let {
            val totalRuns = it?.getInt("total_runs")?.toDouble() !!
            val totalSecrets = it.getInt("secrets")?.toDouble() !!
            totalSecrets / totalRuns
        }

        val spirit = data.getArray("pets")?.any { it.jsonObject.getString("type") == "SPIRIT" } ?: false

        val pb = autoKickFloors[autoKickFloor.value].run { (if (startsWith('F')) catacombs else master_catacombs)?.getObj("fastest_time_s_plus")?.getInt("${last()}") }
        val prefix = "&9AutoKick &f>"

        fun kickPlayer(reason: String) {
            modMessage("$prefix Kicking $playerFormatted ($reason)")
            sendChatMessage("/p kick $name")
        }

        if (autoKickReasons.value["Personal Best"] == true) {
            val requiredPb = pbReq.value.split(":").let { parts ->
                when (parts.size) {
                    1 -> parts[0].toIntOrNull()?.times(1000) ?: Int.MAX_VALUE
                    2 -> {
                        val minutes = parts[0].toIntOrNull() ?: return@let Int.MAX_VALUE
                        val seconds = parts[1].toIntOrNull() ?: return@let Int.MAX_VALUE
                        (minutes * 60 * 1000) + (seconds * 1000)
                    }

                    else -> {
                        modMessage("$prefix &cError: Invalid PB time format '${pbReq.value}'. Expected 'MM:SS' or 'SS'.")
                        null
                    }
                }
            }

            if (pb == null) return kickPlayer("PB: No S+ | Req: ${pbReq.value}")
            if (requiredPb != null && pb > requiredPb && abs(pb - requiredPb) > 1000) return kickPlayer("PB: ${formatPb(pb)} | Req: ${pbReq.value}")
        }

        if (autoKickReasons.value["Secrets Average"] == true) {
            val requiredSecretsAvg = secretsReq.value.toDoubleOrNull()
            if (requiredSecretsAvg == null) modMessage("$prefix &cError: Secrets Average input is not a valid number \"${secretsReq.value}\".")
            else if (secretAvg < requiredSecretsAvg) return kickPlayer("SecretsAvg: ${secretAvg.toFixed(2)} | Req: $requiredSecretsAvg")
        }

        if (autoKickReasons.value["Spirit Pet"] == true && ! spirit) return kickPlayer("Missing Spirit Pet")
    }

    private fun formatPb(milliseconds: Any): String {
        return if (milliseconds is String) milliseconds
        else NumbersUtils.formatTime(milliseconds as Number)
    }
}