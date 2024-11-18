package noammaddons.features.misc

import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.addRandomColorCodes
import noammaddons.utils.ChatUtils.formatNumber
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inSkyblock


object DamageSplash: Feature() {
    private val damageRegex = Regex("[✧✯]?(\\d{1,3}(?:,\\d{3})*[⚔+✧❤♞☄✷ﬗ✯]*)")

    @SubscribeEvent
    fun onRenderLiving(e: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.customDamageSplash) return
        if (! inSkyblock) return
        val entity = e.entity as? EntityArmorStand ?: return
        if (! entity.hasCustomName()) return
        val damageMatcher = damageRegex.matchEntire(entity.customNameTag.removeFormatting()) ?: return

        entity.customNameTag = "&f✧${addRandomColorCodes(formatNumber(damageMatcher.groups[1] !!.value))}&f✧".addColor()
    }
}
