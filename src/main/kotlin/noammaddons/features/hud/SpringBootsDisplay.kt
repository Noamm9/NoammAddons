package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.PlayerUtils
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText

object SpringBootsDisplay: Feature() {
    private object SpringBootsElement: GuiElement(hudData.getData().SpringBootsDisplay) {
        override val enabled get() = config.SpringBootsDisplay
        var text = ""
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale(), color)
        override fun exampleDraw() = drawText("&a&l33%", getX(), getY(), getScale(), color)

        val color get() = RenderHelper.colorByPresent(progress, 42, false)
    }

    private var progress = 0
    private val PitchArray = listOf(
        0.6984127163887024,
        0.8253968358039856,
        0.8888888955116272,
        0.9365079402923584,
        1.047619104385376,
        1.1746032238006592,
        1.317460298538208,
    )

    @SubscribeEvent
    fun onSound(event: SoundPlayEvent) {
        if (! config.SpringBootsDisplay) return
        if (mc.thePlayer == null) return
        if (! mc.thePlayer.isSneaking && ! IsWearingSpringBoots()) return
        if (event.name != "note.pling") return
        if (! PitchArray.contains(event.pitch.toDouble()) || progress >= 42) return  // 42 - fill to 100%

        progress += 1
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! SpringBootsElement.enabled) return
        if (mc.thePlayer == null) return
        if (! IsWearingSpringBoots() || progress <= 0) return
        if (! mc.thePlayer.isSneaking || ! IsWearingSpringBoots()) progress = 0

        SpringBootsElement.text = "${((progress / 42.0) * 100.0).toInt()}%"
        SpringBootsElement.draw()
    }

    private fun IsWearingSpringBoots(): Boolean = (PlayerUtils.getBoots()?.SkyblockID ?: "") == "SPRING_BOOTS"
}
