package noammaddons.features.cosmetics

import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.PlayerUtils.Player

object CustomBowHitSound {
    @SubscribeEvent
    fun onBowSound(event: PlaySoundEvent) {
        if (!config.CustomBowHitSound) return
        if (event.name != "random.successful_hit") return

        event.result = null
	    
	    // note.harp
	    // random.pop
        Player!!.playSound("random.pop", 1f, 1f)
        Player!!.playSound("random.pop", 1f, 1f)
        Player!!.playSound("random.pop", 1f, 1f)
    }
}
