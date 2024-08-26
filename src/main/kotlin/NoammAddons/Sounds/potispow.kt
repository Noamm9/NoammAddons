package NoammAddons.Sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object potispow {
    private val potispow = ResourceLocation(MOD_ID, "potispow")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(potispow))
    }
}