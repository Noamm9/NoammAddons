package noammaddons.features.misc

import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature


object CustomFov : Feature() {
    @SubscribeEvent
    fun onFOVModifier(event: EntityViewRenderEvent.FOVModifier) {
        if (! config.CustomFov) return
        mc.gameSettings.fovSetting = config.CustomFovValue
    }
}