package noammaddons.sounds

import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation


object chipi_chapa {
    fun play() {
        mc.thePlayer.playSound("$MOD_ID:chipi_chapa", 1F, 1F)
    }
}