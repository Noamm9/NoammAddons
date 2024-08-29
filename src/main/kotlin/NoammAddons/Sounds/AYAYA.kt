package NoammAddons.Sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object AYAYA {
    private val AYAYA = ResourceLocation(MOD_ID, "AYAYA")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(AYAYA))
    }
}
