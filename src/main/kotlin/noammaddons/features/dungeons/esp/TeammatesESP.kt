package noammaddons.features.dungeons.esp

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostRenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.Utils.equalsOneOf


object TeammatesESP: Feature() {
    @SubscribeEvent
    fun RenderOutline(event: PostRenderEntityModelEvent) {
        if (! config.dungeonTeammatesEsp) return
        if (! config.espType.equalsOneOf(0, 2)) return
        if (! inDungeon) return

        dungeonTeammatesNoSelf.forEach {
            if (event.entity != it.entity) return@forEach

            EspMob(event, it.clazz.color)
        }
    }

    @SubscribeEvent
    fun RenderBox(event: RenderWorld) {
        if (! config.dungeonTeammatesEsp) return
        if (config.espType != 1) return
        if (! inDungeon) return

        for (entity in mc.theWorld?.loadedEntityList ?: return) {
            dungeonTeammatesNoSelf.forEach {
                if (entity != it.entity) return@forEach

                drawEntityBox(entity, it.clazz.color)
            }
        }
    }
}


