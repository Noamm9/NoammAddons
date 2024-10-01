package noammaddons.sounds

import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object AYAYA {
    fun play() {
		mc.thePlayer.playSound("$MOD_ID:AYAYA", 1F, 1F)
    }
}
