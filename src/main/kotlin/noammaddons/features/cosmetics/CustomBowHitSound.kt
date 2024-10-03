package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.mc
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config

object CustomBowHitSound {
    @SubscribeEvent
    fun onBowSound(event: PlaySoundEvent) {
        if (!config.CustomBowHitSound) return
        if (event.name != "random.successful_hit") return

        event.result = null

        // Play custom sound instead
        val player = mc.thePlayer ?: return
        player.playSound("note.harp", 1f, 1f)
        player.playSound("note.harp", 1f, 1f)
        player.playSound("note.harp", 1f, 1f)
    }
}
