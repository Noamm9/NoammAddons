package noammaddons.utils

import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.isNull

object PartyUtils {
    private val youJoinedPartyPattern = Regex("You have joined (?<name>.*)'s? party!")
    private val othersJoinedPartyPattern = Regex("(?<name>.*) joined the party\\.")
    private val othersInThePartyPattern = Regex("You'll be partying with: (?<names>.*)")
    private val disbandedPattern = Regex(".* has disbanded the party!")
    private val kickedPattern = Regex("You have been kicked from the party .*")
    private val partyMembersStartPattern = Regex("Party Members \\(\\d+\\)")
    private val partyMemberListPattern = Regex("Party (?<kind>Leader|Moderators|Members): (?<names>.*)")
    private val partyChatMessagePattern = Regex("Party > (?<author>[^:]*): (?<message>.*)")
    private val promotePattern = Regex("(?<name>.*) has promoted (?<newowner>.*) to Party Leader")
    private val memberLeftPatterns = listOf(
        Regex("(?<name>.*) has left the party\\."),
        Regex("(?<name>.*) has been removed from the party\\."),
        Regex("Kicked (?<name>.*) because they were offline\\."),
        Regex("(?<name>.*) was removed from your party because they disconnected\\.")
    )
    private val transferPatterns = listOf(
        Regex("The party was transferred to (?<newowner>.*) because (?<name>.*) left"),
        Regex("The party was transferred to (?<newowner>.*) by (?<name>.*)")
    )
    private val finderJoinPatterns = listOf(
        Regex("Party Finder > (?<name>.*?) joined the group! \\(Combat Level \\d+\\)"),
        Regex("Party Finder > (?<name>.*?) joined the dungeon group! \\(.* Level \\d+\\)")
    )


    @set:Synchronized
    private var INTERNAL_partyLeader: String? = null

    @get:Synchronized
    private val INTERNAL_partyMembers = mutableListOf<String>()
    var prevPartyLeader: String? = null


    @get:Synchronized
    val partyLeader: String? get() = INTERNAL_partyLeader?.replace(Regex("\\[[^]]+] "), "")?.replace(" ", "")

    @get:Synchronized
    val partyMembers: List<Pair<String, EntityPlayer?>>
        get() = INTERNAL_partyMembers.map {
            it.replace(Regex("\\[[^]]+] "), "")
        }.toMutableSet().apply {
            removeIf { it.length < 3 }
        }.map { it to mc.theWorld.getPlayerEntityByName(it) }

    @get:Synchronized
    val partyMembersNoSelf get() = partyMembers.filterNot { it.second == mc.thePlayer }
    fun isPartyLeader(name: String = mc.session.username) = partyLeader == name


    init {
        loop(1000) {
            if (! (onHypixel && INTERNAL_partyMembers.isNotEmpty())) {
                partyLeft()
            }
        }
    }


    private fun String.cleanPlayerName(): String {
        val split = trim().split(" ")
        return (if (split.size > 1) split[1].removeFormatting() else split[0].removeFormatting())
    }


    @SubscribeEvent
    @Synchronized
    fun onChat(event: Chat) {
        val message = event.component.noFormatText

        if (partyChatMessagePattern.matches(message)) {
            val name = partyChatMessagePattern.find(message) !!.destructured.component1().cleanPlayerName()
            addPlayer(name)
        }

        handleJoinPatterns(message)
        handleLeavePatterns(message)

        if (disbandedPattern.matches(message) || kickedPattern.matches(message)) {
            partyLeft()
        }

        if (message.equalsOneOf(
                "You left the party.",
                "The party was disbanded because all invites expired and the party was empty.",
                "You are not currently in a party.",
                "You are not in a party."
            )
        ) {
            partyLeft()
        }

        if (partyMembersStartPattern.matches(message)) {
            INTERNAL_partyMembers.clear()
        }
        if (partyMemberListPattern.matches(message)) {
            val (kind, _names) = partyMemberListPattern.find(message) !!.destructured
            val names = _names.split(" ● ").toMutableList()

            names.forEach { name ->
                val playerName = name.replace(" ●", "").cleanPlayerName()
                addPlayer(playerName)

                if (kind == "Leader") {
                    INTERNAL_partyLeader = playerName
                }
            }
        }
    }

    @Synchronized
    private fun handleJoinPatterns(message: String) {
        when {
            youJoinedPartyPattern.matches(message) -> {
                youJoinedPartyPattern.find(message) !!.destructured.component1().cleanPlayerName().let {
                    INTERNAL_partyLeader = it
                    addPlayer(it)
                    addPlayer(mc.session.username)
                }
            }

            othersJoinedPartyPattern.matches(message) -> {
                val name = othersJoinedPartyPattern.find(message) !!.destructured.component1().cleanPlayerName()
                if (INTERNAL_partyMembers.isEmpty()) INTERNAL_partyLeader = mc.session.username
                addPlayer(name)
            }

            othersInThePartyPattern.matches(message) -> {
                othersInThePartyPattern.find(message) !!.destructured.component1().split(", ").forEach {
                    addPlayer(it.cleanPlayerName())
                }
            }

            finderJoinPatterns.any { it.matches(message) } -> {
                val name = finderJoinPatterns.first { it.matches(message) }.find(message) !!.destructured.component1().cleanPlayerName()
                addPlayer(name)
            }
        }
    }

    @Synchronized
    private fun handleLeavePatterns(message: String) {
        memberLeftPatterns.forEach { pattern ->
            if (pattern.matches(message)) {
                val name = pattern.find(message) !!.destructured.component1().cleanPlayerName()
                removeWithLeader(name)
            }
        }

        transferPatterns.forEach { pattern ->
            if (pattern.matches(message)) {
                val (newLeader, prevLeader) = pattern.find(message) !!.destructured
                INTERNAL_partyLeader = newLeader.cleanPlayerName()
                prevPartyLeader = prevLeader.cleanPlayerName()
            }
        }

        if (promotePattern.matches(message)) {
            val (prevLeader, newLeader) = promotePattern.find(message) !!.destructured
            INTERNAL_partyLeader = newLeader.cleanPlayerName()
            prevPartyLeader = prevLeader.cleanPlayerName()
        }
    }

    @Synchronized
    private fun removeWithLeader(name: String) {
        INTERNAL_partyMembers.remove(name)
        if (name == prevPartyLeader) prevPartyLeader = null
    }

    @Synchronized
    private fun addPlayer(playerName: String) {
        if ((! INTERNAL_partyMembers.contains(playerName))) {
            INTERNAL_partyMembers.add(playerName)
        }
    }

    @Synchronized
    private fun partyLeft() {
        INTERNAL_partyMembers.clear()
        INTERNAL_partyLeader = null
        prevPartyLeader = null
    }

    fun isInParty() = INTERNAL_partyMembers.isNotEmpty() && ! INTERNAL_partyLeader.isNull()
}
