package NoammAddons.Sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object marioikesound {
    private val marioikesound = ResourceLocation(MOD_ID, "marioikesound")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(marioikesound))
    }
}