package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature

object EtherwarpSound: Feature() {
    @SubscribeEvent
    fun onSound(event: SoundPlayEvent) {
        if (! config.EtherwarpSound) return
        if (event.name != "mob.enderdragon.hit") return
        if (event.pitch != 0.53968257f) return
        event.isCanceled = true

        mc.thePlayer.playSound("note.pling", 1f, 0.3f)
    }
}
