package noammaddons.features.impl.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*

object CustomBowHitSound: Feature() {
    private val soundName = TextInputSetting("Sound Name", "note.harp")
    private val volume = SliderSetting("Volume", 0f, 1f, 0.1f, 0.5f)
    private val pitch = SliderSetting("Pitch", 0f, 2f, 0.1f, 1f)
    private val playSound = ButtonSetting("Play Sound") {
        repeat(5) { mc.thePlayer?.playSound(soundName.value, volume.value, pitch.value) }
    }

    override fun init() = addSettings(soundName, volume, pitch, playSound)

    @SubscribeEvent
    fun onBowSound(event: SoundPlayEvent) {
        if (event.name != "random.successful_hit") return
        event.isCanceled = true
        playSound.invoke()
    }
}
