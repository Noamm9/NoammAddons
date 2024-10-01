package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object RemoveSelfieCam {
    @SubscribeEvent
    fun removeSelfieCamera(event: TickEvent.ClientTickEvent) {
        if (!config.removeSelfieCamera) return
        if (mc.gameSettings.keyBindTogglePerspective.isKeyDown || mc.gameSettings.keyBindTogglePerspective.isPressed) {
            if (mc.gameSettings.thirdPersonView == 2) mc.gameSettings.thirdPersonView = 0
        }
    }
}