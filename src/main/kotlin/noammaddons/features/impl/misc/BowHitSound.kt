package noammaddons.features.impl.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.removeFormatting

object BowHitSound: Feature() {
    private val soundName by TextInputSetting("Sound Name", "note.harp")
    private val volume by SliderSetting("Volume", 0f, 1f, 0.1f, 0.5f)
    private val pitch by SliderSetting("Pitch", 0f, 2f, 0.1f, 1f)
    private val playSound by ButtonSetting("Play Sound") {
        repeat(5) { mc.thePlayer?.playSound(soundName.lowercase().removeFormatting(), volume, pitch) }
    }

    @SubscribeEvent
    fun onBowSound(event: SoundPlayEvent) {
        if (event.name != "random.successful_hit") return
        if (soundName.lowercase().removeFormatting() == "random.successful_hit") return
        event.isCanceled = true
        playSound.run()
    }
}
