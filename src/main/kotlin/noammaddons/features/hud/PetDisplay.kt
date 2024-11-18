package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock

object PetDisplay: Feature() {
    private val PetDisplayElement = TextElement("petdisplay", dataObj = hudData.getData().PetDisplay)
    private val chatSpawnRegex = Regex("§aYou summoned your §r(?<pet>.*)§r§a!")
    private val chatDespawnRegex = Regex("You despawned your §r(?<pet>.*)§r§a!")
    private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")


    @SubscribeEvent
    fun onChat(event: Chat) {
        event.component.run {
            val match1 = chatSpawnRegex.find(formattedText)?.destructured?.component1()
            val match2 = chatDespawnRegex.find(formattedText)?.destructured?.component1()
            val match3 = chatPetRuleRegex.find(formattedText)?.destructured?.component1()

            PetDisplayElement.setText(
                when {
                    match1 != null -> match1
                    match2 != null -> match2
                    match3 != null -> match3
                    else -> return
                }
            )
        }
    }


    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! inSkyblock) return
        if (! config.PetDisplay) return
        if (PetDisplayElement.getText() == "petdisplay") return

        PetDisplayElement.draw()
    }
}