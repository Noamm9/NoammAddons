package noammaddons.features.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.Player

object CustomBowHitSound: Feature() {
    @SubscribeEvent
    fun onBowSound(event: SoundPlayEvent) {
        if (! config.CustomBowHitSound) return
        if (event.name != "random.successful_hit") return
        event.isCanceled = true

        Player?.playSound("note.harp", 1f, 1f)
    }
}
