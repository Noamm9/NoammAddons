package noammaddons.utils

import net.minecraft.client.audio.SoundCategory
import noammaddons.NoammAddons.Companion.MOD_ID
import noammaddons.NoammAddons.Companion.mc
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*
import kotlin.math.log10
import kotlin.math.max

object SoundUtils {
    private val audioDataCache = mutableMapOf<String, ByteArray>()
    private val clipPools = ConcurrentHashMap<String, MutableList<Clip>>()

    private fun loadAudioData(soundName: String): ByteArray {
        val stream = this::class.java.classLoader.getResourceAsStream("assets/$MOD_ID/sounds/$soundName.wav")
            ?: throw IllegalArgumentException("Sound not found: $soundName")
        return stream.use { it.readBytes() }
    }

    private fun getOrCreateClip(soundName: String): Clip {
        val pool = clipPools.getOrPut(soundName) { mutableListOf() }

        val clip = synchronized(pool) {
            pool.removeFirstOrNull()
        } ?: createNewClip(soundName)

        return clip
    }

    private fun createNewClip(soundName: String): Clip {
        val audioData = audioDataCache.getOrPut(soundName) { loadAudioData(soundName) }
        val inputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioData))
        val clip = AudioSystem.getClip()
        clip.open(inputStream)

        // Return clip to pool when done
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                clip.stop()
                clip.flush()
                clip.framePosition = 0
                clipPools.getOrPut(soundName) { mutableListOf() }.add(clip)
            }
        }

        return clip
    }

    fun playSound(soundName: String): Clip {
        val clip = getOrCreateClip(soundName)

        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val volume = mc.gameSettings.getSoundLevel(SoundCategory.MASTER)
        val adjusted = 20f * log10(max(volume, 0.001f))
        gainControl.value = adjusted.coerceIn(gainControl.minimum, gainControl.maximum)

        clip.start()
        return clip
    }

    fun chipiChapa() = playSound("chipi_chapa")
    fun ayaya() = playSound("AYAYA")
    fun click() = playSound("click")
    fun iHaveNothing() = playSound("ihavenothing")
    fun marioSound() = playSound("mariolikesound")
    fun notificationSound() = playSound("notificationsound")
    fun potisPow() = playSound("potispow")
    fun Pling() = playSound("Pling")
    fun harpNote() = playSound("HarpNote")
    fun buff() = playSound("buff")
    fun bigFart() = playSound("bigFart")
    fun smallFart() = playSound("smallFart")
}