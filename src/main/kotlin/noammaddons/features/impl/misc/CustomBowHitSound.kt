package noammaddons.features.impl.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*

object CustomBowHitSound: Feature() {
    private val soundName by TextInputSetting("Sound Name", "note.harp")
    private val volume by SliderSetting("Volume", 0, 1, 0.5)
    private val pitch by SliderSetting("Pitch", 0, 2, 1.0)
    private val playSound = ButtonSetting("Play Sound") {
        repeat(5) {
            mc.thePlayer?.playSound(
                soundName,
                volume.toFloat(),
                pitch.toFloat()
            )
        }
    }

    @SubscribeEvent
    fun onBowSound(event: SoundPlayEvent) {
        if (event.name != "random.successful_hit") return
        event.isCanceled = true
        playSound.invoke()
    }
}
