package noammaddons.utils

import net.minecraft.client.audio.SoundCategory
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import java.io.ByteArrayInputStream
import javax.sound.sampled.*
import kotlin.math.log10
import kotlin.math.max


object SoundUtils {
    private val audioDataCache = mutableMapOf<String, ByteArray>()

    private fun loadAudioData(soundName: String): ByteArray {
        val resourceStream = this::class.java.classLoader.getResourceAsStream("assets/$MOD_ID/sounds/$soundName.wav")
            ?: throw IllegalArgumentException("Sound not found: $soundName")

        return resourceStream.use { it.readBytes() }
    }

    private fun createClipFromData(data: ByteArray): Clip {
        val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
        val clip: Clip = AudioSystem.getClip()
        clip.open(audioInputStream)
        return clip
    }

    fun playSound(soundName: String): Clip {
        val audioData = audioDataCache.getOrPut(soundName) { loadAudioData(soundName) }
        val clip = createClipFromData(audioData)

        val adjustVol = {
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val adjustedVolume = convertToDecibel(mc.gameSettings.getSoundLevel(SoundCategory.MASTER))
            gainControl.value = adjustedVolume.coerceIn(gainControl.minimum, gainControl.maximum)
        }

        ThreadUtils.loop(100, { ! clip.isOpen }, adjustVol)

        clip.addLineListener { event ->
            if (event.type != LineEvent.Type.STOP) return@addLineListener
            clip.close() // Close to not cause mem leaks
        }


        return clip
    }

    private fun convertToDecibel(volume: Float): Float {
        return 20f * log10(max(volume, 0.001f))
    }

    fun clearCache() {
        audioDataCache.clear()
    }

    // Sound references
    val chipiChapa get() = playSound("chipi_chapa")
    val ayaya get() = playSound("AYAYA")
    val click get() = playSound("click")
    val iHaveNothing get() = playSound("ihavenothing")
    val marioSound get() = playSound("mariolikesound")
    val notificationSound get() = playSound("notificationsound")
    val potisPow get() = playSound("potispow")
    val Pling get() = playSound("Pling")
    val HarpNote get() = playSound("HarpNote")
    val buff get() = playSound("buff")
}
