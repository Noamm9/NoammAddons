package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.utils.*
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import kotlin.math.roundToInt

object SpringBootsDisplay: Feature() {
    private object SpringBootsElement: GuiElement(hudData.getData().springBootsDisplay) {
        override val enabled get() = SpringBootsDisplay.enabled
        var text = ""
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale(), color)
        override fun exampleDraw() = drawText("&a&l33%", getX(), getY(), getScale(), color)

        val color get() = RenderHelper.colorByPresent(progress, 42, false)
    }

    private val pitchList = listOf(
        0.6984127163887024,
        0.8253968358039856,
        0.8888888955116272,
        0.9365079402923584,
        1.047619104385376,
        1.1746032238006592,
        1.317460298538208,
    )

    private var progress = 0

    @SubscribeEvent
    fun onSound(event: SoundPlayEvent) {
        if (event.name != "note.pling") return
        if (! ServerPlayer.player.sneaking) return
        if (ServerPlayer.player.onGround == false) return
        if (progress >= 42) return
        if (event.pitch.toDouble() !in pitchList) return  // 42 - fill to 100%
        if (! IsWearingSpringBoots()) return
        progress += 1
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! ServerPlayer.player.sneaking) progress = 0
        if (ServerPlayer.player.onGround == false) progress = 0
        if (! IsWearingSpringBoots()) progress = 0
        if (progress <= 0) return

        SpringBootsElement.text = "${((progress / 42.0) * 100.0).roundToInt()}%"
        SpringBootsElement.draw()
    }

    private fun IsWearingSpringBoots() = (PlayerUtils.getBoots()?.skyblockID ?: "") == "SPRING_BOOTS"
}
