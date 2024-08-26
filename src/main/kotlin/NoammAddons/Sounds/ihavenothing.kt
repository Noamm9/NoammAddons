package NoammAddons.Sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation


object ihavenothing {
    private val ihavenothing = ResourceLocation(MOD_ID, "ihavenothing")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(ihavenothing))
    }
}