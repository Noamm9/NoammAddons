package noammaddons.features.impl.dungeons

import gg.essential.universal.UChat
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.JsonUtils.getDouble
import noammaddons.utils.JsonUtils.getInt
import noammaddons.utils.JsonUtils.getObj
import noammaddons.utils.JsonUtils.getString
import noammaddons.utils.ProfileUtils.getCatacombsLevel
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.uppercaseFirst
import java.time.Duration


// todo pf join stats message and autokick?
object PartyFinder: Feature("A group of many features regarding the dungeon ape finder") {
    val customMenu = ToggleSetting("Custom Menu")
    private val partyFinderStats = ToggleSetting("Party Finder Stats")
    private val reformatPfMessages = ToggleSetting("Cleaner Messages")
    private val joinedSound = ToggleSetting("Join Sound")
    override fun init() = addSettings(customMenu, partyFinderStats, reformatPfMessages, joinedSound)

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
    fun onChat(event: Chat) {
        val msg = event.component.formattedText
        val playerFormatted = joinedRegex.find(msg)?.destructured?.component1() ?: return
        if (partyFinderStats.value && playerFormatted.removeFormatting() != mc.session.username) printPlayerStats(playerFormatted)
        if (joinedSound.value) SoundUtils.Pling()
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
        val profile = ProfileUtils.getSelectedProfile(name.removeFormatting()) ?: return@launch
        val dungeons = profile["dungeons"]?.jsonObject ?: return@launch
        val pets = profile["pets"]?.jsonArray?.map { it.jsonObject } ?: return@launch
        val accessoryBag = profile.getObj("accessory_bag_storage") ?: return@launch

        val selectedArrow = profile.getString("favorite_arrow")?.lowercase()?.remove("_arrow")?.uppercaseFirst() ?: "None"
        val powerStone = accessoryBag.getString("selected_power")?.uppercaseFirst() ?: "None"

        val catacombs = dungeons.getObj("dungeon_types")?.getObj("catacombs") ?: return@launch
        val master_catacombs = dungeons.getObj("dungeon_types")?.getObj("master_catacombs")

        val bloodMobsKilled = profile.getObj("player_stats")?.getObj("kills")?.run { (getInt("watcher_summon_undead") ?: 0) + (getInt("master_watcher_summon_undead") ?: 0) } ?: return@launch

        val catacombsRuns = catacombs.getObj("tier_completions")?.getInt("total") ?: return@launch
        val masterCatacombsRuns = master_catacombs?.getObj("tier_completions")?.getInt("total") ?: 0
        val totalDungeonRunsCount = (catacombsRuns + masterCatacombsRuns).toDouble()
        val totalSecrets = dungeons.getInt("secrets")?.toDouble() ?: return@launch
        val secretAvg = totalSecrets / totalDungeonRunsCount

        val cataLvl = catacombs.getDouble("experience")?.let { getCatacombsLevel(it) } ?: return@launch
        val classAvg = dungeons.getObj("player_classes")?.values?.let { it.sumOf { clazz -> getCatacombsLevel(clazz.jsonObject.getDouble("experience") !!).toDouble() } / it.size.toDouble() } ?: return@launch

        val petsList = pets.filter {
            it.getString("type").equalsOneOf("GOLDEN_DRAGON", "ENDER_DRAGON", "JELLYFISH", "SPIRIT", "BABY_YETI")
                    && it.getString("tier").equalsOneOf("LEGENDARY", "MYTHIC")
        }.map {
            ItemUtils.ItemRarity.valueOf(it.getString("tier") !!).baseColor to it.getString("type") !!.let { type ->
                if (type.endsWith("_DRAGON")) {
                    if (type.startsWith("GOLDEN")) "Gdrag" else "Edrag"
                }
                else if (type.startsWith("BABY_")) "Yeti"
                else type.lowercase().split("_").joinToString(" ") { word -> word.uppercaseFirst() }
            }
        }.toSet()

        //val equippmentInv = decodeBase64ItemList(profile.getObj("equippment_contents")?.getString("data") ?: return@launch)
        //val playerInv = decodeBase64ItemList(profile.getObj("inv_contents")?.getString("data") ?: return@launch)

        val talismenBag = ItemUtils.decodeBase64ItemList(profile.getObj("talisman_bag")?.getString("data") ?: return@launch)
        val magicalPower = ProfileUtils.getMagicalPower(talismenBag, profile)

        val armorInv = ItemUtils.decodeBase64ItemList(profile.getObj("inv_armor")?.getString("data") ?: return@launch)
        val armorList = armorInv.filterNotNull().reversed().map { armorPiece ->
            val lore = armorPiece.lore.toMutableList().apply { add(0, armorPiece.displayName) }
            ChatComponentText("  " + armorPiece.displayName).apply {
                chatStyle = ChatStyle().apply {
                    chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(lore.joinToString("\n")))
                }
            }
        }

        val completionsList = listOf(
            catacombs.getObj("fastest_time")?.keys?.mapNotNull { it.toIntOrNull() }?.maxOrNull()?.let { highestFloor ->
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
            master_catacombs?.getObj("fastest_time")?.keys?.mapNotNull { it.toIntOrNull() }?.maxOrNull()?.let { highestFloor ->
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
            completionsList.filterNotNull().forEach(UChat::chat)
            UChat.chat("§a§l------------------------------------§r")
        }
    }

    private fun formatPb(milliseconds: Comparable<*>): String {
        if (milliseconds is String) return milliseconds
        val duration = Duration.ofMillis((milliseconds as Number).toLong())
        val minutes = duration.toMinutes()
        val seconds = duration.seconds % 60
        val resultBuilder = StringBuilder()

        if (minutes > 0) resultBuilder.append("${minutes}m")

        if (seconds > 0) {
            if (resultBuilder.isNotEmpty()) resultBuilder.append(" ")
            resultBuilder.append("${seconds}s")
        }

        return resultBuilder.toString()
    }
}