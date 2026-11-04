package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils

object PartyUtils: ISelfInit {
    enum class PartyRole { LEADER, MOD, MEMBER }
    enum class OnlineState { ONLINE, OFFLINE }

    data class PartyMember(
        val name: String,
        val role: PartyRole,
        val state: OnlineState,
        val rank: String? = null,
        val rankPrefix: String? = null,
    ) {
        val isOnline get() = state == OnlineState.ONLINE
        val displayName get() = rankPrefix?.let { "$it $name" } ?: name
    }

    private const val RANK = "(?:\\[[^]]+] )?"
    private const val NAME = "(\\w{1,16})"
    private fun line(pattern: String) = Regex("^$pattern$")

    private val joinedSelf = line("You have joined $RANK$NAME'?s? party!")
    private val partyingWith = line("You'll be partying with: (.+)")
    private val kickedSelf = line("You have been kicked from the party by $RANK$NAME")
    private val disbanded = line("$RANK$NAME has disbanded the party!")
    private val selfLeft = setOf(
        "You left the party.",
        "You are not in a party right now.",
        "You are not currently in a party.",
        "You are not in a party.",
    )
    private const val DISBANDED_PREFIX = "The party was disbanded because "

    private val joined = line("$RANK$NAME joined the party\\.")
    private val finderJoined = line("Party Finder > $RANK$NAME joined the (?:dungeon )?group! \\(.+\\)")
    private val removed = line("$RANK$NAME (?:has left the party|has been removed from the party|was removed from (?:the|your) party because they disconnected)\\.")
    private val kickedOffline = line("Kicked (.+) because they were offline\\.")

    private val transferred = line("The party was transferred to $RANK$NAME (by|because) $RANK$NAME(?: left)?")
    private val roleChanged = line("$RANK$NAME has (?:promoted|demoted) $RANK$NAME to Party (Leader|Moderator|Member)") // todo test
    private val warped = line("Party Leader, $RANK$NAME, summoned you to their server.")

    private val invited = line("$RANK$NAME invited $RANK$NAME to the party! They have \\d+ seconds to accept\\.")
    private val inviteExpired = line("The party invite to $RANK$NAME has expired\\.?")

    private val memberDisconnected = line("$RANK$NAME has disconnected, they have \\d+ minutes? to rejoin before they are removed from the party\\.")
    private val leaderDisconnected = line("The party leader,? $RANK$NAME has disconnected, they have \\d+ minutes? to rejoin before the party is disbanded\\.")
    private val rejoined = line("(?:The party leader,? )?$RANK$NAME has rejoined\\.")

    private val listHeader = line("Party Members \\((\\d+)\\)")
    private val listEntry = line("Party (Leader|Moderators|Members): (.+)")

    private val partyChat = line("Party > $RANK$NAME: .*")

    private val floorEntered = line("[^\\w\\s]*\\s*$RANK$NAME entered (?:MM )?The Catacombs, Floor [IVX]+!")

    private val named = line("$RANK$NAME")
    private val rankedName = Regex("\\[([^]]+)] (\\w{1,16})(?!\\w)")
    private val rankedNameFormatted = Regex("((?:§.)*\\[(?:[^]§]|§.)+](?:§.)*) (?:§.)*(\\w{1,16})(?!\\w)")

    private var lineRanks = emptyMap<String, Pair<String, String?>>()
    private val separator = Regex("^[-▬=\\s]+$")
    private var membersKnown = false
    private var listSize: Int? = null

    @Volatile var partyMembers: List<PartyMember> = emptyList()
        private set

    @Volatile var pendingInvites: Set<String> = emptySet()
        private set

    @Volatile var previousLeader: String? = null
        private set

    @Volatile var isInParty = false
        private set

    val members get() = partyMembers.map(PartyMember::name)
    val partyLeader get() = partyMembers.firstOrNull { it.role == PartyRole.LEADER }?.name
    val partySize get() = partyMembers.size

    private val self get() = mc.user.name

    override fun init() {
        register<ChatMessageEvent>(EventPriority.HIGHEST, receiveCancelled = true) {
            if (! LocationUtils.onHypixel) return@register

            val lines = event.unformattedText.split('\n')
            val formattedLines = event.formattedText.split('\n').takeIf { it.size == lines.size }

            lines.forEachIndexed { i, raw ->
                val text = raw.trim()
                if (text.isEmpty() || separator.matches(text)) return@forEachIndexed
                handleLine(text, formattedLines?.get(i) ?: raw)
                lineRanks = emptyMap()
            }
        }

        register<DungeonEvent.RunStartedEvent> {
            DungeonListener.dungeonTeammates.filterNot { it.isDead }.forEach { upsert(it.name, state = OnlineState.ONLINE) }
        }
    }

    fun getMember(name: String = self) = partyMembers.firstOrNull { it.name.same(name) }
    fun getRole(name: String = self) = getMember(name)?.role
    fun getState(name: String) = getMember(name)?.state
    fun membersWith(role: PartyRole) = partyMembers.filter { it.role == role }
    fun membersWith(state: OnlineState) = partyMembers.filter { it.state == state }

    fun isLeader() = getRole() == PartyRole.LEADER
    fun isLeader(name: String) = getRole(name) == PartyRole.LEADER
    fun isMod(name: String = self) = getRole(name) == PartyRole.MOD
    fun isMember(name: String) = getMember(name) != null
    fun isOnline(name: String) = getState(name) == OnlineState.ONLINE
    fun canInvite() = getRole() in setOf(PartyRole.LEADER, PartyRole.MOD)

    fun sync() = ChatUtils.sendCommand("pl")

    private fun handleLine(text: String, formatted: String) {
        val prefixes = rankedNameFormatted.findAll(formatted).associate { it.groupValues[2].lowercase() to it.groupValues[1] }
        lineRanks = rankedName.findAll(text).associate { it.groupValues[2].lowercase() to (it.groupValues[1] to prefixes[it.groupValues[2].lowercase()]) }

        if (text in selfLeft || text.startsWith(DISBANDED_PREFIX)) return reset()

        floorEntered.matchEntire(text)?.let { return setLeader(it.groupValues[1]) }
        if (disbanded.matches(text) || kickedSelf.matches(text)) return reset()

        joinedSelf.matchEntire(text)?.let {
            reset()
            setLeader(it.groupValues[1])
            membersKnown = true
            return
        }

        partyingWith.matchEntire(text)?.let { match ->
            return match.groupValues[1].split(", ").mapNotNull(::nameOf).forEach { upsert(it) }
        }

        joined.matchEntire(text)?.let { return upsert(it.groupValues[1], state = OnlineState.ONLINE) }

        finderJoined.matchEntire(text)?.let {
            val name = it.groupValues[1]
            if (name.same(self)) reset()
            return upsert(name, state = OnlineState.ONLINE)
        }

        removed.matchEntire(text)?.let { return removeMember(it.groupValues[1]) }

        kickedOffline.matchEntire(text)?.let { match ->
            return match.groupValues[1].split(", ").mapNotNull(::nameOf).forEach(::removeMember)
        }

        transferred.matchEntire(text)?.let {
            val (newLeader, reason, oldLeader) = it.destructured
            setLeader(newLeader)
            if (reason == "because") removeMember(oldLeader)
            else upsert(oldLeader, PartyRole.MOD)
            return
        }

        warped.matchEntire(text)?.let {
            val (leader) = it.destructured
            setLeader(leader)
        }

        roleChanged.matchEntire(text)?.let {
            val (actor, target, role) = it.destructured
            if (! isMember(actor) || ! isMember(target)) membersKnown = false
            upsert(actor, state = OnlineState.ONLINE)
            setLeader(actor)
            when (role) {
                "Leader" -> {
                    setLeader(target)
                    upsert(actor, PartyRole.MOD)
                }

                "Moderator" -> upsert(target, PartyRole.MOD)
                else -> upsert(target, PartyRole.MEMBER)
            }
            return
        }

        invited.matchEntire(text)?.let {
            val (inviter, invitee) = it.destructured
            if (! isMember(inviter)) membersKnown = false
            if (partyLeader == null) setLeader(inviter) else upsert(inviter, state = OnlineState.ONLINE)
            if (! pendingInvites.has(invitee)) pendingInvites = pendingInvites + invitee
            return
        }

        inviteExpired.matchEntire(text)?.let {
            pendingInvites = pendingInvites.without(it.groupValues[1])
            return
        }

        leaderDisconnected.matchEntire(text)?.let {
            setLeader(it.groupValues[1])
            return upsert(it.groupValues[1], state = OnlineState.OFFLINE)
        }

        memberDisconnected.matchEntire(text)?.let {
            val name = it.groupValues[1]
            if (! isMember(name)) membersKnown = false
            return upsert(name, state = OnlineState.OFFLINE)
        }

        rejoined.matchEntire(text)?.let {
            val name = it.groupValues[1]
            if (! isMember(name)) membersKnown = false
            if (text.startsWith("The party leader")) setLeader(name)
            return upsert(name, state = OnlineState.ONLINE)
        }

        listHeader.matchEntire(text)?.let {
            partyMembers = emptyList()
            isInParty = true
            membersKnown = false
            listSize = it.groupValues[1].toInt()
            return
        }

        listEntry.matchEntire(text)?.let { return parseListEntry(it.groupValues[1], formatted) }

        partyChat.matchEntire(text)?.let {
            val name = it.groupValues[1]
            if (! isMember(name)) membersKnown = false
            return upsert(name, state = OnlineState.ONLINE)
        }
    }

    private fun parseListEntry(kind: String, formatted: String) {
        formatted.substringAfter(": ").split('●').dropLast(1).forEach { segment ->
            val name = nameOf(segment.removeFormatting()) ?: return@forEach
            val dotColour = segment.lastIndexOf('§').takeIf { it in 0 until segment.lastIndex }?.let { segment[it + 1] }
            val state = if (dotColour == 'c') OnlineState.OFFLINE else OnlineState.ONLINE

            when (kind) {
                "Leader" -> setLeader(name)
                "Moderators" -> upsert(name, PartyRole.MOD)
                else -> upsert(name, PartyRole.MEMBER)
            }
            upsert(name, state = state)
        }

        if (listSize == partySize) {
            membersKnown = true
            listSize = null
        }
    }

    private fun upsert(name: String, role: PartyRole? = null, state: OnlineState? = null) {
        if (! isInParty) membersKnown = false
        isInParty = true
        val current = partyMembers.let { list ->
            if (list.any { it.name.same(self) }) list
            else list + PartyMember(self, PartyRole.MEMBER, OnlineState.ONLINE)
        }

        val existing = current.firstOrNull { it.name.same(name) }
        val seenRank = lineRanks[name.lowercase()]
        val updated = PartyMember(
            existing?.name ?: name,
            role ?: existing?.role ?: PartyRole.MEMBER,
            state ?: existing?.state ?: OnlineState.ONLINE,
            seenRank?.first ?: existing?.rank,
            seenRank?.second ?: existing?.rankPrefix,
        )

        partyMembers = if (existing == null) current + updated else current.map { if (it === existing) updated else it }
        if (pendingInvites.has(name)) pendingInvites = pendingInvites.without(name)
    }

    private fun removeMember(name: String) {
        if (name.same(self)) return reset()
        if (! isMember(name)) membersKnown = false
        partyMembers = partyMembers.filterNot { it.name.same(name) }
        if (membersKnown && partyMembers.all { it.name.same(self) } && pendingInvites.isEmpty()) reset()
    }

    private fun setLeader(name: String) {
        val current = partyLeader
        if (current != null && ! current.same(name)) {
            previousLeader = current
            upsert(current, PartyRole.MEMBER, OnlineState.ONLINE)
        }
        upsert(name, PartyRole.LEADER, OnlineState.ONLINE)
    }

    private fun reset() {
        partyMembers = emptyList()
        pendingInvites = emptySet()
        previousLeader = null
        isInParty = false
        membersKnown = false
        listSize = null
    }

    private fun nameOf(entry: String) = named.matchEntire(entry.trim())?.groupValues?.get(1)
    private fun String?.same(other: String?) = this != null && equals(other, ignoreCase = true)
    private fun Set<String>.has(name: String) = any { it.equals(name, ignoreCase = true) }
    private fun Set<String>.without(name: String) = filterNot { it.equals(name, ignoreCase = true) }.toSet()
}