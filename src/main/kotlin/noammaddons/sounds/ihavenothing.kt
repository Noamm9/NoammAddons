package noammaddons.sounds

import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation


object ihavenothing {
    fun play() {
        mc.thePlayer.playSound("$MOD_ID:ihavenothing", 1f, 1f)
    }
}