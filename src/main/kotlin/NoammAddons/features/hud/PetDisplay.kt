package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.HudElement
import NoammAddons.events.Chat
import NoammAddons.events.InventoryFullyOpenedEvent
import NoammAddons.events.RenderOverlay
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.ItemUtils.lore
import NoammAddons.utils.LocationUtils.inSkyblock
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PetDisplay {
	private val PetDisplayElement = HudElement("petdisplay", dataObj = hudData.getData().PetDisplay)
	private val inventorySelectedPetRegex = Regex("§7§7Selected pet: (?<pet>.*)")
	private val chatSpawnRegex = Regex("§aYou summoned your §r(?<pet>.*)§r§a!")
	private val chatDespawnRegex = Regex("§aYou despawned your §r.*§r§a!")
	private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")
	private val petMenuPattern = Regex("Pets(?: \\(\\d+/\\d+\\) )?",)

	val IsInPetMenu: Boolean = GuiUtils.currentChestName.matches(petMenuPattern)
	val currentPet get() = PetDisplayElement.getText()
	
	
	@SubscribeEvent
	fun onChat(event: Chat) {
		event.component.run {
			val match1 = chatSpawnRegex.find(formattedText)?.destructured?.component1()
			val match2 = chatDespawnRegex.find(formattedText)?.destructured?.component1()
			val match3 = chatPetRuleRegex.find(formattedText)?.destructured?.component1()
			
			PetDisplayElement.setText(when {
				match1!= null -> match1
				match2!= null -> match2
				match3!= null -> match3
				else -> return
			})
		}
	}
	
	
	@SubscribeEvent
	fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
		if (!IsInPetMenu) return
		
		try {
			event.inventory.items.asSequence()
				.flatMap { item -> item.value.lore.asSequence() }
				.mapNotNull { line -> inventorySelectedPetRegex.find(line)?.destructured?.component1() }
				.firstOrNull { it != "§cNone" }
			?.let { PetDisplayElement.setText(it) }
		} catch (_: Error) {}
	}
	
	@SubscribeEvent
	fun onRenderOverlay(event: RenderOverlay) {
		if (!inSkyblock) return
		if (!config.PetDisplay) return
		
		PetDisplayElement.draw()
	}
}