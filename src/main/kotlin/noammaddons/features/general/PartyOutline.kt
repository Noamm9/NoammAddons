package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PartyUtils.partyMembersNoSelf
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderUtils.drawEntityBox


object PartyOutline: Feature() {
    @SubscribeEvent
    fun EspPartyMembers(event: RenderEntityModelEvent) {
        if (! config.partyOutline) return
        if (config.espType == 1) return
        if (inDungeons) return
        if (partyMembersNoSelf.none { event.entity.entityId == it.second?.entityId }) return

        EspMob(event, getRainbowColor(1f))
    }

    @SubscribeEvent
    fun BoxPartyMembers(event: RenderWorld) {
        if (! config.partyOutline) return
        if (config.espType != 1) return
        if (inDungeons) return

        mc.theWorld?.loadedEntityList?.forEach { entity ->
            if (partyMembersNoSelf.none { entity.entityId == it.second?.entityId }) return@forEach

            drawEntityBox(entity, getRainbowColor(1f))
        }
    }
}