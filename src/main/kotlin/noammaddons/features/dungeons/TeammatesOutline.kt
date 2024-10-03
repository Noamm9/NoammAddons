package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderLivingEntityEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.OutlineUtils.outlineESP


object TeammatesOutline {
	@SubscribeEvent
	fun preRenderOutline(event: RenderLivingEntityEvent) {
		if (!inSkyblock || !config.dungeonTeammatesOutline || !inDungeons) return
		dungeonTeammatesNoSelf.forEach {
			if (event.entity == it.entity) {
				outlineESP(event, it.clazz.color)
			}
		}
	}
}


