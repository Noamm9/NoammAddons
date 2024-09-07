package NoammAddons.sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation


object chipi_chapa {
    private val chipi_chapa = ResourceLocation(MOD_ID, "chipi_chapa")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(chipi_chapa))
    }
}