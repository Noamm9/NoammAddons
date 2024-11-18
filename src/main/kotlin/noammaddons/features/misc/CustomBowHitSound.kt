package noammaddons.features.misc

import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.Player

object CustomBowHitSound: Feature() {
    @SubscribeEvent
    fun onBowSound(event: PlaySoundEvent) {
        if (! config.CustomBowHitSound) return
        if (event.name != "random.successful_hit") return

        event.result = null

        Player !!.playSound("note.harp", 1f, 1f)
    }
}
