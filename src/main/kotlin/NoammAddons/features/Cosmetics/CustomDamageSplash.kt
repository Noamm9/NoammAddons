package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.events.RenderLivingEntityEvent
import NoammAddons.utils.ChatUtils.addRandomColorCodes
import NoammAddons.utils.ChatUtils.formatNumber
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object CustomDamageSplash {
    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEntityEvent) {
        if (event.entity is EntityArmorStand && config.customDamageSplash) {
            val damageNumber = Regex("/(?:✯|✧)?(\\d{1,3}(?:,\\d{3})+)(?:♞|✧|✯)?/")
                .find(event.entity.customNameTag.removeFormatting())?.groupValues?.get(1)

            if (damageNumber != null)
                event.entity.customNameTag =
                    addRandomColorCodes("✯${formatNumber(damageNumber.replace(Regex("/,/g"), ""))}✯")
        }
    }
}