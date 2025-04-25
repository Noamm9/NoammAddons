package noammaddons.features.impl.general.teleport

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*

object EtherwarpSound: Feature() {
    private val soundName = TextInputSetting("Sound Name", "random.orb")
    private val volume = SliderSetting("Volume", 0, 1, 0.5)
    private val pitch = SliderSetting("Pitch", 0, 2, 1.0)
    private val playSound = ButtonSetting("Play Sound") {
        repeat(5) {
            mc.thePlayer?.playSound(
                soundName.value,
                volume.value.toFloat(),
                pitch.value.toFloat()
            )
        }
    }

    init {
        addSettings(soundName, volume, pitch, playSound)
    }

    @SubscribeEvent
    fun onSound(event: SoundPlayEvent) {
        if (event.name != "mob.enderdragon.hit") return
        if (event.pitch != 0.53968257f) return
        event.isCanceled = true
        playSound.invoke()
    }
}
