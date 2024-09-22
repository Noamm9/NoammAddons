package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.EditGui.HudElement
import NoammAddons.events.RenderOverlay
import NoammAddons.utils.ItemUtils.SkyblockID
import NoammAddons.utils.PlayerUtils
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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
        if (mc.thePlayer == null) return
        if (!mc.thePlayer.isSneaking && IsWearingSpringBoots() != true) return
        if (event.name != "note.pling") return
        if (!PitchArray.contains(event.sound.pitch.toDouble()) || progress >= 42) return  // 42 - fill to 100%

        progress += 1
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (!config.SpringBootsDisplay) return
        if (mc.thePlayer == null) return
        if (IsWearingSpringBoots() != true || progress <= 0) return
        if (!mc.thePlayer.isSneaking || IsWearingSpringBoots() != true) progress = 0
	    
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