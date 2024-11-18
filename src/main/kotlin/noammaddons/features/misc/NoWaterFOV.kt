package noammaddons.features.misc

import net.minecraft.block.material.Material
import net.minecraftforge.client.event.EntityViewRenderEvent.FOVModifier
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock

object NoWaterFOV : Feature() {
    @SubscribeEvent
    fun onFOV(event: FOVModifier) {
        if (! config.antiWaterFOV || ! inSkyblock || event.block.material != Material.water) return
        event.fov = event.fov * 70F / 60F
    }
}
