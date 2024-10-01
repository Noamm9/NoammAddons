package noammaddons.sounds

import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object potispow {
    fun play() {
        mc.thePlayer.playSound("$MOD_ID:potispow", 1f, 1f)
    }
}