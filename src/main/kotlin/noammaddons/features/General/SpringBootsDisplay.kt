package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.noammaddons.Companion.mc
import noammaddons.config.EditGui.HudElement
import noammaddons.events.RenderOverlay
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.PlayerUtils
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.Utils.isNull
import java.awt.Color

object SpringBootsDisplay {
    private var progress = 0
    private val SpringBootsElement = HudElement("&a&l33%", dataObj = hudData.getData().SpringBootsDisplay)
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
    fun onSound(event: PlaySoundEvent) {
        if (!config.SpringBootsDisplay) return
        if (Player.isNull()) return
        if (!Player!!.isSneaking && !IsWearingSpringBoots()) return
        if (event.name != "note.pling") return
        if (!PitchArray.contains(event.sound.pitch.toDouble()) || progress >= 42) return  // 42 - fill to 100%

        progress += 1
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onRenderOverlay(event: RenderOverlay) {
        if (!config.SpringBootsDisplay) return
        if (Player.isNull()) return
        if (!IsWearingSpringBoots() || progress <= 0) return
        if (!Player!!.isSneaking || !IsWearingSpringBoots()) progress = 0
	    
        SpringBootsElement
            .setText("${((progress/42.0)*100.0).toInt()}%")
            .setColor(when (((progress/42.0)*100.0).toInt()) {
                in 0 until 25 -> Color.GREEN
                in 25 until 50 -> Color.YELLOW
                in 50..74 -> Color(255, 116 , 0)
                in 75..100 -> Color.RED
                else -> return
            }).draw()
    }


    private fun IsWearingSpringBoots(): Boolean = (PlayerUtils.getBoots()?.SkyblockID ?: "") == "SPRING_BOOTS"
}
