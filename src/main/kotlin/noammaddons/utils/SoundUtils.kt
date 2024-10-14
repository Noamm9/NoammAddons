package noammaddons.utils

import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

object SoundUtils {
	private fun wavToSound(SoundName: String): Clip {
		val resourceStream = this::class.java.classLoader.getResourceAsStream("assets/noammaddons/sounds/$SoundName.wav")
		val bufferedInput = BufferedInputStream(resourceStream!!)
		
		val audioInputStream = AudioSystem.getAudioInputStream(bufferedInput)
		val clip: Clip = AudioSystem.getClip()
		clip.open(audioInputStream)
		
		return clip
	}
	
	
	val chipiChapa get() = wavToSound("chipi_chapa")
	val ayaya get() = wavToSound("AYAYA")
	val click get() = wavToSound("click")
	val iHaveNothing get() = wavToSound("ihavenothing")
	val marioSound get() = wavToSound("mariolikesound")
	val notificationSound get() = wavToSound("notificationsound")
	val potisPow get() = wavToSound("potispow")
}


