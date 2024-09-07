package NoammAddons.sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object mariolikesound {
    private val mariolikesound = ResourceLocation(MOD_ID, "mariolikesound")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(mariolikesound))
    }
}