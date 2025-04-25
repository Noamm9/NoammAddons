package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText


object PetDisplay: Feature() {

    private object PetDisplayElement: GuiElement(hudData.getData().petDisplay.hudData) {
        override val enabled get() = PetDisplay.enabled
        var text = hudData.getData().petDisplay.pet
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale())
        override fun exampleDraw() = drawText("&6Golden Dragon", getX(), getY(), getScale())
    }

    private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")
    private val chatSpawnRegex = Regex("§aYou summoned your §r(?<pet>.*)§r§a!")
    private val chatDespawnRegex = Regex("You despawned your §r(?<pet>.*)§r§a!")
    private val petMenuRegex = Regex("Pets( \\(\\d/\\d\\) )?") // todo

    @SubscribeEvent
    fun onChat(event: Chat) {
        event.component.run {
            val match1 = chatSpawnRegex.find(formattedText)?.destructured?.component1()
            val match2 = chatDespawnRegex.find(formattedText)?.destructured?.component1()
            val match3 = chatPetRuleRegex.find(formattedText)?.destructured?.component1()
            PetDisplayElement.text = match1 ?: match2 ?: match3 ?: return
            hudData.getData().petDisplay.pet = PetDisplayElement.text
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! inSkyblock) return
        PetDisplayElement.draw()
    }
}