package NoammAddons.Sounds

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation


object notificationsound {
    private val notificationsound = ResourceLocation(MOD_ID, "notificationsound")

    fun play() {
        mc.soundHandler.playSound(PositionedSoundRecord.create(notificationsound))
    }
}