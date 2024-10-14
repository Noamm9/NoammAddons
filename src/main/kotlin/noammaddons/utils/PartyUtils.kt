package noammaddons.utils

import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.ChatUtils.removeFormatting
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PartyUtils {
	private val youJoinedPartyPattern = Regex("You have joined (?<name>.*)'s? party!")
	private val othersJoinedPartyPattern = Regex("(?<name>.*) joined the party\\.")
	private val othersInThePartyPattern = Regex("You'll be partying with: (?<names>.*)")
	private val disbandedPattern = Regex(".* has disbanded the party!")
	private val kickedPattern = Regex("You have been kicked from the party = .*")
	private val partyMembersStartPattern = Regex("Party Members \\(\\d+\\)")
	private val partyMemberListPattern = Regex("Party (?<kind>Leader|Moderators|Members): (?<names>.*)")
	private val partyChatMessagePattern = Regex("Party > (?<author>[^:]*): (?<message>.*)")
	private val memberLeftPatterns = listOf(
		Regex("(?<name>.*) has left the party\\."),
		Regex("(?<name>.*) has been removed from the party\\."),
		Regex("Kicked (?<name>.*) because they were offline\\."),
		Regex("(?<name>.*) was removed from your party because they disconnected\\.")
	)
	private val transferPatterns = listOf(
		Regex("The party was transferred to (?<newowner>.*) because (?<name>.*) left"),
		Regex("The party was transferred to (?<newowner>.*) = (?<name>.*)")
	)
	private val finderJoinPatterns = listOf(
		Regex("Party Finder > (?<name>.*?) joined the group! \\(Combat Level \\d+\\)"),
		Regex("Party Finder > (?<name>.*?) joined the dungeon group! \\(.* Level \\d+\\)")
	)
	
	var partyLeader: String? = null
	var prevPartyLeader: String? = null
	val partyMembers = mutableListOf<String>()
	
	private fun String.cleanPlayerName(): String {
		val split = trim().split(" ")
		return if (split.size > 1) split[1].removeFormatting() else split[0].removeFormatting()
	}
	
	
	@SubscribeEvent
	fun onChat(event: Chat) {
		val message = event.component.unformattedText.removeFormatting()
		
		if (partyChatMessagePattern.matches(message)) {
			val name = partyChatMessagePattern.find(message)!!.destructured.component1()
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
			partyMembers.clear()
		}
		if (partyMemberListPattern.matches(message)) {
			val (kind, names) = partyMemberListPattern.find(message)!!.destructured
			val isPartyLeader = kind == "Leader"
			names.split(" ● ").forEach { name ->
				val playerName = name.replace(" ●", "").cleanPlayerName()
				addPlayer(playerName)
				if (isPartyLeader) {
					partyLeader = playerName
				}
			}
		}
	}
	
	private fun handleJoinPatterns(message: String) {
		when {
			youJoinedPartyPattern.matches(message) -> {
				val name = youJoinedPartyPattern.find(message)!!.destructured.component1().cleanPlayerName()
				partyLeader = name
				addPlayer(name)
			}
			othersJoinedPartyPattern.matches(message) -> {
				val name = othersJoinedPartyPattern.find(message)!!.destructured.component1().cleanPlayerName()
				if (partyMembers.isEmpty()) partyLeader = mc.session.username
				addPlayer(name)
			}
			othersInThePartyPattern.matches(message) -> {
				othersInThePartyPattern.find(message)!!.destructured.component1().split(", ").forEach {
					addPlayer(it.cleanPlayerName())
				}
			}
			finderJoinPatterns.any { it.matches(message) } -> {
				val name = finderJoinPatterns.first { it.matches(message) }.find(message)!!.destructured.component1().cleanPlayerName()
				addPlayer(name)
			}
		}
	}
	
	private fun handleLeavePatterns(message: String) {
		memberLeftPatterns.forEach { pattern ->
			if (pattern.matches(message)) {
				val name = pattern.find(message)!!.destructured.component1().cleanPlayerName()
				removeWithLeader(name)
			}
		}
		transferPatterns.forEach { pattern ->
			if (pattern.matches(message)) {
				val (newLeader, prevLeader) = pattern.find(message)!!.destructured
				partyLeader = newLeader.cleanPlayerName()
				prevPartyLeader = prevLeader.cleanPlayerName()
			}
		}
	}
	
	private fun removeWithLeader(name: String) {
		partyMembers.remove(name)
		if (name == prevPartyLeader) prevPartyLeader = null
	}
	
	private fun addPlayer(playerName: String) {
		if (!partyMembers.contains(playerName) && playerName != mc.session.username) {
			partyMembers.add(playerName)
		}
	}
	
	private fun partyLeft() {
		partyMembers.clear()
		partyLeader = null
		prevPartyLeader = null
	}
}
