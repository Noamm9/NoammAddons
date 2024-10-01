package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent


object CustomFov {
    @SubscribeEvent
    fun onFOVModifier(event: EntityViewRenderEvent.FOVModifier) {
        if (!config.CustomFov) return
        mc.gameSettings.fovSetting = config.CustomFovValue
    }
}