package noammaddons.features.general

import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.Player

object EtherwarpSound: Feature() {
    @SubscribeEvent
    fun onSound(event: PlaySoundEvent) {
        if (! config.EtherwarpSound) return
        if (event.name != "mob.enderdragon.hit") return
        if (event.sound.pitch != 0.53968257f) return

        event.result = null
        Player.playSound("note.pling", 1f, 0.3f)
    }
}
