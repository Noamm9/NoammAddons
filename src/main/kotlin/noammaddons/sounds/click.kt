package noammaddons.sounds

import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc

object click {
	private val click = ResourceLocation(MOD_ID, "click")
	
	fun play() {
		mc.soundHandler.playSound(PositionedSoundRecord.create(click))
	}
}
