package noammaddons.utils

import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.PacketEvent
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.ItemUtils.lore

object PartyUtils {
    data class PartyMember(var name: String, var rank: String, var rankFormated: String, var formattedName: String, var online: Boolean)

    private val disbandsRegexs = arrayOf(
        Regex("^.+ §r§ehas disbanded the party!§r$"),
        Regex("^§eYou have been kicked from the party by .+ §r§e§r$"),
        Regex("^§cThe party was disbanded because all invites expired and the party was empty\\.§r$"),
        Regex("^§cThe party was disbanded because the party leader disconnected\\.§r$"),
        Regex("^§eYou left the party\\.§r$"),
        Regex("^§6Party Members \\(\\d+\\)§r$"),
        Regex("^You are not currently in a party\\.$"),
    )

    private val playerRejoin = Regex("^((?:§.(?:\\[[^]]+])?)) *(\\w+) §r§ejoined the party.§r$")
    private val playerleft = Regex("^((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§ehas left the party.§r\$")
    private val playerRemove = Regex("^((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§ehas been removed from the party.§r\$")
    private val inviteRegex = Regex("^((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§einvited .+ §r§eto the party! They have §r§c60 §r§eseconds to accept.§r\$")
    private val partyMsg = Regex("^§r§9Party §8> ((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+)§f: §r.+§r\$")
    private val youJoined = Regex("^§eYou have joined §r((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+)'s §r§eparty!§r\$")
    private val partylist = Regex("^§eParty (?:Members|Moderators): §r(.+)\$")
    private val partyListEntry = "^((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+)§r§(.)\$".toRegex()
    private val partylistv2 = Regex("^§eParty Leader: (?:§r)?((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§(\\w)●§r\$")
    private val leaderDisconect = Regex("^§eThe party leader, §r((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§ehas disconnected, they have §r§c5 §r§eminutes to rejoin before the party is disbanded\\.§r\$")
    private val partyTransfer = Regex("^§eThe party was transferred to §r((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§eby §r((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+)§r\$")
    private val leaveTransfer = Regex("^§eThe party was transferred to §r((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§ebecause §r((?:§.(?:\\[[^\\]]+\\])?)) *(\\w+) §r§eleft§r\$")
    private val partyfinder = "^§dParty Finder §r§f> §r(§.)(\\w{1,16}) §r§ejoined the dungeon group! \\(§r§b(\\w+) Level (\\d+)§r§e\\)§r\$".toRegex()


    var inParty = false
    var leader: String? = null
    val members = mutableMapOf<String, PartyMember>()
    var size = 0

    val entities get() = HashMap(members).mapNotNull { mc.theWorld.getPlayerEntityByName(it.key) }

    private val cachedranks = mutableMapOf<String, String>()

    @SubscribeEvent
    fun onChat(event: Chat) {
        val message = event.component.formattedText

        playerRejoin.matchAndRun(message) { (rank, name) -> addMember(name, rank) }
        playerleft.matchAndRun(message) { (_, name) -> removeMember(name) }
        playerRemove.matchAndRun(message) { (_, name) -> removeMember(name) }
        inviteRegex.matchAndRun(message) { (rank, name) -> addMember(name, rank) }
        partyMsg.matchAndRun(message) { (rank, name) -> addMember(name, rank, true) }
        partyfinder.matchAndRun(message) { (color, name) ->
            addMember(name)
            storedNames.forEach { (n, t) -> addMember(n); if (t == "L") leader = n }
            storedNames.clear()
        }

        for (regex in disbandsRegexs) {
            if (regex.matches(message)) {
                disband()
            }
        }

        youJoined.matchAndRun(message) { (rank, name) ->
            leader = name
            addMember(name, rank)
            addMember(mc.session.username)
        }

        partylist.matchAndRun(message) { (rest) ->
            val segments = rest.split(" ● §r")
            for (segment in segments) {
                val match = partyListEntry.find(segment) ?: continue
                val (rank, name, dotColor) = match.destructured
                addMember(name, rank, dotColor == "a")
            }
        }

        partylistv2.matchAndRun(message) { (rank, name, dotColor) ->
            leader = name
            addMember(name, rank)

            if (dotColor == "a") this.members[name] !!.online = true
            else if (dotColor == "c") this.members[name] !!.online = false
        }

        leaderDisconect.matchAndRun(message) { (rank, name) ->
            addMember(name, rank, false)
            leader = name
        }

        partyTransfer.matchAndRun(message) { (rank, name, rank2, name2) ->
            addMember(name, rank)
            leader = name
            addMember(name2, rank2)
        }

        leaveTransfer.matchAndRun(message) { (rank1, name1, leaderRank, leaderName) ->
            addMember(name1, rank1)
            leader = name1
            removeMember(leaderName)
        }

    }

    private fun Regex.matchAndRun(text: String, func: (List<String>) -> Unit) {
        find(text)?.destructured?.let { match ->
            val values = match.toList()
            try {
                func(values)
            }
            catch (e: Exception) {
                modMessage("Errored on regex $this with $text")
            }
            return
        }
    }


    private fun addMember(player: String, _rank: String = "", online: Boolean = true) {
        inParty = true
        var rank = _rank

        if (rank == "" && player in cachedranks.keys) {
            rank = cachedranks[player] !!
        }

        // Cache ranks
        if (rank != "" && player !in cachedranks.keys) {
            cachedranks[player] = rank
        }

        var formattedName = rank
        if (! Regex("^(?:§.)*$").matches(rank)) {
            formattedName += " "
        }
        formattedName += player

        this.members[player] = PartyMember(
            name = player,
            rank = rank.removeFormatting(),
            rankFormated = rank,
            formattedName = formattedName,
            online = online
        )

        size = members.size
    }

    private fun removeMember(player: String) {
        members.remove(player)

        this.size = this.members.size

        if (this.size == 0) {
            disband()
        }
    }

    private fun disband() {
        members.clear()
        size = 0
        leader = null
        inParty = false
    }

    val storedNames = mutableListOf<Pair<String, String>>()

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        val packet = event.packet as? C0EPacketClickWindow? ?: return
        if (currentChestName.removeFormatting() != "Party Finder") return
        val itemstack = packet.clickedItem ?: return
        val lName = Regex("^(?:§.)+(\\w+)'s Party\$").find(itemstack.displayName)?.destructured?.component1() ?: return
        storedNames.add(lName to "L")

        val lore = itemstack.lore
        val memberStartInd = lore.indexOf("§5§o§f§7Members: ").takeIf { it != - 1 } ?: return

        val nameLines = lore.slice(memberStartInd + 1 .. memberStartInd + 6)

        for (line in nameLines) {
            val member = Regex("^§5§o §.(\\w{1,16})§f: §e\\w+§b \\(§e(\\d+)§b\\)\$").find(line)?.destructured?.component1() ?: continue
            if (storedNames.any { it.first == member }) continue
            storedNames.add(member to "M")
        }
    }
}
