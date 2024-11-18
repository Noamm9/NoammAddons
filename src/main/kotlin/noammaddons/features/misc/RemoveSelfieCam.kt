package noammaddons.features.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.isHoldingWitherImpact

object RemoveSelfieCam : Feature() {
    @SubscribeEvent
    fun removeSelfieCamera(event: TickEvent) {
        if (! config.removeSelfieCamera) return
        if (! isHoldingWitherImpact() && config.removeSelfieCameraOnlyWithHype) return
        if (mc.gameSettings.thirdPersonView != 2) return
        if (! mc.gameSettings.keyBindTogglePerspective.isKeyDown &&
            ! mc.gameSettings.keyBindTogglePerspective.isPressed
        ) return

        mc.gameSettings.thirdPersonView = 0
    }
}