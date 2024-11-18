package noammaddons.utils

import net.minecraft.client.audio.SoundCategory
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.debugMessage
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

object SoundUtils {
    private fun wavToSound(SoundName: String): Clip {
        val resourceStream = this::class.java.classLoader.getResourceAsStream("assets/noammaddons/sounds/$SoundName.wav")
        val bufferedInput = BufferedInputStream(resourceStream !!)

        val audioInputStream = AudioSystem.getAudioInputStream(bufferedInput)
        val clip: Clip = AudioSystem.getClip()
        clip.open(audioInputStream)


        val masterVolume = mc.gameSettings.getSoundLevel(SoundCategory.MASTER)
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl

        gainControl.value = convertToDecibel(masterVolume).coerceIn(gainControl.minimum, gainControl.maximum)
        debugMessage("Gain set to: ${gainControl.value}")
        return clip
    }

    private fun convertToDecibel(volume: Float): Float {
        return (- 80f + (volume * 80f)) / 6
    }


    val chipiChapa get() = wavToSound("chipi_chapa")
    val ayaya get() = wavToSound("AYAYA")
    val click get() = wavToSound("click")
    val iHaveNothing get() = wavToSound("ihavenothing")
    val marioSound get() = wavToSound("mariolikesound")
    val notificationSound get() = wavToSound("notificationsound")
    val potisPow get() = wavToSound("potispow")
    val Pling get() = wavToSound("Pling")
    val HarpNote get() = wavToSound("HarpNote")
}


