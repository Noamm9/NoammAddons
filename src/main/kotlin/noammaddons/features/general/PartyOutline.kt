package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostRenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PartyUtils.partyMembersNoSelf
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderUtils.drawEntityBox


object PartyOutline: Feature() {
    @SubscribeEvent
    fun EspPartyMembers(event: PostRenderEntityModelEvent) {
        if (! config.partyOutline) return
        if (config.espType == 1) return
        if (inDungeon) return
        if (partyMembersNoSelf.none { event.entity.entityId == it.second?.entityId }) return

        EspMob(event, getRainbowColor(1f))
    }

    @SubscribeEvent
    fun BoxPartyMembers(event: RenderWorld) {
        if (! config.partyOutline) return
        if (config.espType != 1) return
        if (inDungeon) return

        mc.theWorld?.loadedEntityList?.forEach { entity ->
            if (partyMembersNoSelf.none { entity.entityId == it.second?.entityId }) return@forEach

            drawEntityBox(entity, getRainbowColor(1f))
        }
    }
}