package noammaddons.utils

import net.minecraft.client.audio.SoundCategory
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.*
import kotlin.math.log10

object SoundUtils {
    private val audioDataCache = mutableMapOf<String, ByteArray>()

    private fun loadAudioData(soundName: String): ByteArray {
        val resourceStream = this::class.java.classLoader.getResourceAsStream("assets/$MOD_ID/sounds/$soundName.wav")
            ?: throw IllegalArgumentException("Sound not found: $soundName")

        val buffer = ByteArray(1024)
        val outputStream = ByteArrayOutputStream()
        resourceStream.use { input ->
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == - 1) break
                outputStream.write(buffer, 0, bytesRead)
            }
        }
        return outputStream.toByteArray()
    }

    private fun createClipFromData(data: ByteArray): Clip {
        val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
        val clip: Clip = AudioSystem.getClip()
        clip.open(audioInputStream)
        return clip
    }

    private fun playSound(soundName: String): Clip {
        val audioData = audioDataCache.getOrPut(soundName) { loadAudioData(soundName) }
        val clip = createClipFromData(audioData)

        // Configure volume based on game settings, sadly not dynamic I am lazy
        val masterVolume = mc.gameSettings.getSoundLevel(SoundCategory.MASTER)
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        gainControl.value = convertToDecibel(masterVolume).coerceIn(gainControl.minimum, gainControl.maximum)

        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                clip.close() // Ensure resources are cleared to not cause mem leaks
            }
        }

        return clip
    }

    // this shit legit gave me a headache
    private fun convertToDecibel(volume: Float): Float {
        return 20f * log10(volume.coerceIn(0.001f, 1.0f))
    }


    fun clearCache() {
        audioDataCache.clear()
    }

    val chipiChapa get() = playSound("chipi_chapa")
    val ayaya get() = playSound("AYAYA")
    val click get() = playSound("click")
    val iHaveNothing get() = playSound("ihavenothing")
    val marioSound get() = playSound("mariolikesound")
    val notificationSound get() = playSound("notificationsound")
    val potisPow get() = playSound("potispow")
    val Pling get() = playSound("Pling")
    val HarpNote get() = playSound("HarpNote")
}




