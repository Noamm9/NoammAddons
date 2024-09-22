package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.utils.ChatUtils.equalsOneOf
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object PartyUtils {
	private val youJoinedPartyPattern = Regex("You have joined (?<name>.*)'s? party!")
	private val othersJoinedPartyPattern = Regex("(?<name>.*) joined the party\\.")
	private val othersInThePartyPattern = Regex("You'll be partying with: (?<names>.*)")
	private val otherLeftPattern = Regex("(?<name>.*) has left the party\\.")
	private val otherKickedPattern = Regex("(?<name>.*) has been removed from the party\\.")
	private val otherOfflineKickedPattern = Regex("Kicked (?<name>.*) because they were offline\\.")
	private val otherDisconnectedPattern = Regex("(?<name>.*) was removed from your party because they disconnected\\.")
	private val transferOnLeavePattern = Regex("The party was transferred to (?<newowner>.*) because (?<name>.*) left")
	private val transferVoluntaryPattern = Regex("The party was transferred to (?<newowner>.*) = (?<name>.*)")
	private val disbandedPattern = Regex(".* has disbanded the party!")
	private val kickedPattern = Regex("You have been kicked from the party = .*")
	private val partyMembersStartPattern = Regex("Party Members \\(\\d+\\)")
	private val partyMemberListPattern = Regex("Party (?<kind>Leader|Moderators|Members): (?<names>.*)")
	private val kuudraFinderJoinPattern = Regex("Party Finder > (?<name>.*?) joined the group! \\(Combat Level \\d+\\)")
	private val dungeonFinderJoinPattern = Regex("Party Finder > (?<name>.*?) joined the dungeon group! \\(.* Level \\d+\\)")
	private val PartyChatMessage = Regex("Party > (?<author>[^:]*): (?<message>.*)")

	
	val partyMembers = mutableListOf<String>()
	var partyLeader: String? = null
	var prevPartyLeader: String? = null
	
	
	private fun String.cleanPlayerName(): String {
		val split = trim().split(" ")
		return if (split.size > 1) {
			split[1].removeFormatting()
		} else {
			split[0].removeFormatting()
		}
	}
	
	
	
	fun listMembers() {
		val size = partyMembers.size
		if (size == 0) {
			ChatUtils.debugMessage("No tracked party members!")
			return
		}
		
		ChatUtils.debugMessage("Tracked party members §7($size) §f:")
		
		for (member in partyMembers) {
			ChatUtils.debugMessage(" §a- §7$member" + if (partyLeader == member) " §a(Leader)" else "")
		}
		
		if (partyLeader == mc.session.username) {
			ChatUtils.debugMessage("§aYou are leader")
		}
		
	}
	/*
	@SubscribeEvent
	fun test(event: MessageSentEvent) {
		if (event.message.equals("test")) {
			event.isCanceled = true
			listMembers()
		}
	}
	*/
	
	
	@SubscribeEvent
	fun onChat(event: Chat) {
		val message = event.component.unformattedText.removeFormatting()
		
		if (PartyChatMessage.matches(message)) {
			val name = PartyChatMessage.find(message)!!.destructured.component1()
			addPlayer(name)
		}
		
		// new member joined
		if (youJoinedPartyPattern.matches(message)) {
			youJoinedPartyPattern.find(message)!!.destructured
			val name = youJoinedPartyPattern.find(message)!!.destructured.component1().cleanPlayerName()
			partyLeader = name
			addPlayer(name)
		}
		if (othersJoinedPartyPattern.matches(message)) {
			val name = othersJoinedPartyPattern.find(message)!!.destructured.component1().cleanPlayerName()
			if (partyMembers.isEmpty()) partyLeader = mc.session.username
			
			addPlayer(name)
		}
		if (othersInThePartyPattern.matches(message)) {
			for (name in othersJoinedPartyPattern.find(message)!!.destructured.component1().split(", ")) {
				addPlayer(name.cleanPlayerName())
			}
		}
		if (kuudraFinderJoinPattern.matches(message)) {
			val name = kuudraFinderJoinPattern.find(message)!!.destructured.component1().cleanPlayerName()
			addPlayer(name)
		}
		if (dungeonFinderJoinPattern.matches(message)) {
			val name = dungeonFinderJoinPattern.find(message)!!.destructured.component1().cleanPlayerName()
			addPlayer(name)
		}
		
		// one member got removed
		if (otherLeftPattern.matches(message)) {
			val name = otherLeftPattern.find(message)!!.destructured.component1().cleanPlayerName()
			removeWithLeader(name)
		}
		if (otherKickedPattern.matches(message)) {
			val name = otherKickedPattern.find(message)!!.destructured.component1().cleanPlayerName()
			removeWithLeader(name)
		}
		if (otherOfflineKickedPattern.matches(message)) {
			val name = otherOfflineKickedPattern.find(message)!!.destructured.component1().cleanPlayerName()
			removeWithLeader(name)
		}
		if (otherDisconnectedPattern.matches(message)) {
			val name = otherDisconnectedPattern.find(message)!!.destructured.component1().cleanPlayerName()
			partyMembers.remove(name)
		}
		if (transferOnLeavePattern.matches(message)) {
			val name = transferOnLeavePattern.find(message)!!.destructured.component2().cleanPlayerName()
			partyLeader = transferOnLeavePattern.find(message)!!.destructured.component1().cleanPlayerName()
			partyMembers.remove(name)
		}
		if (transferVoluntaryPattern.matches(message)) {
			partyLeader = transferVoluntaryPattern.find(message)!!.destructured.component1().cleanPlayerName()
			prevPartyLeader = transferVoluntaryPattern.find(message)!!.destructured.component2().cleanPlayerName()
		}
		
		// party disbanded
		if (disbandedPattern.matches(message)) {
			partyLeft()
		}
		if (kickedPattern.matches(message)) {
			partyLeft()
		}
		if (message.equalsOneOf(
			"You left the party.",
			"The party was disbanded because all invites expired and the party was empty.",
			"You are not currently in a party.",
			"You are not in a party.")
		) { partyLeft() }
		
		// party list
		if (partyMembersStartPattern.matches(message)) {
			partyMembers.clear()
		}
		
		
		if (partyMemberListPattern.matches(message)) {
			val (kind, names) = partyMemberListPattern.find(message)!!.destructured
			val isPartyLeader = kind == "Leader"
			for (name in names.split(" ● ")) {
				val playerName = name.replace(" ●", "").cleanPlayerName()
				addPlayer(playerName)
				if (isPartyLeader) {
					partyLeader = playerName
				}
			}
		}
	}
	
	private fun removeWithLeader(name: String) {
		partyMembers.remove(name)
		if (name == prevPartyLeader) {
			prevPartyLeader = null
		}
	}
	
	private fun addPlayer(playerName: String) {
		if (partyMembers.contains(playerName)) return
		if (playerName == mc.session.username) return
		partyMembers.add(playerName)
	}
	
	private fun partyLeft() {
		partyMembers.clear()
		partyLeader = null
		prevPartyLeader = null
	}
}