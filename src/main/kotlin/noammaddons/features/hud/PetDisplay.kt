package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText

object PetDisplay: Feature() {
    private object PetDisplayElement: GuiElement(hudData.getData().PetDisplay) {
        override val enabled get() = config.PetDisplay
        var text = "                     "
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale())
        override fun exampleDraw() = drawText("&6Golden Dragon", getX(), getY(), getScale())
    }

    private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")
    private val chatSpawnRegex = Regex("§aYou summoned your §r(?<pet>.*)§r§a!")
    private val chatDespawnRegex = Regex("You despawned your §r(?<pet>.*)§r§a!")


    @SubscribeEvent
    fun onChat(event: Chat) {
        event.component.run {
            val match1 = chatSpawnRegex.find(formattedText)?.destructured?.component1()
            val match2 = chatDespawnRegex.find(formattedText)?.destructured?.component1()
            val match3 = chatPetRuleRegex.find(formattedText)?.destructured?.component1()
            PetDisplayElement.text = match1 ?: match2 ?: match3 ?: return
        }

        if (config.DevMode) PetDisplayElement.text = "hi hi hi"
    }


    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! inSkyblock) return
        if (! config.PetDisplay) return
        PetDisplayElement.draw()
    }
}