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
    private val clips = mutableMapOf<ByteArray, Clip>()

    private fun loadAudioData(soundName: String): ByteArray {
        val resourceStream = this::class.java.classLoader.getResourceAsStream("assets/$MOD_ID/sounds/$soundName.wav")
            ?: throw IllegalArgumentException("Sound not found: $soundName")

        return resourceStream.use { it.readBytes() }
    }

    private fun createClipFromData(data: ByteArray): Clip {
        clips[data]?.let {
            it.framePosition = 0
            return it
        }

        val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
        val clip: Clip = AudioSystem.getClip()
        clip.open(audioInputStream)
        return clip
    }

    fun playSound(soundName: String): Clip {
        val audioData = audioDataCache.getOrPut(soundName) { loadAudioData(soundName) }
        val newClip = createClipFromData(audioData)
        clips[audioData] = newClip

        // Adjust volume
        val gainControl = newClip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val adjustedVolume = 20f * log10(max(mc.gameSettings.getSoundLevel(SoundCategory.MASTER), 0.001f))
        gainControl.value = adjustedVolume.coerceIn(gainControl.minimum, gainControl.maximum)

        newClip.start()
        return newClip
    }

    fun initSounds() {
        val volume = mc.gameSettings.getSoundLevel(SoundCategory.MASTER)
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F)
        chipiChapa()
        ayaya()
        click()
        iHaveNothing()
        marioSound()
        notificationSound()
        potisPow()
        Pling()
        harpNote()
        buff()
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, volume)
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
}