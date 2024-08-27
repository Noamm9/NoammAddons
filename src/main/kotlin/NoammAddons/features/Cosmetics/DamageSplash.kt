package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.utils.ChatUtils.addRandomColorCodes
import NoammAddons.utils.ChatUtils.formatNumber
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.LocationUtils.inSkyblock
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object DamageSplash {
    private val damagePattern = Regex("[✧✯]?(\\d{1,3}(?:,\\d{3})*[⚔+✧❤♞☄✷ﬗ✯]*)")

    @SubscribeEvent
    fun onRenderLiving(e: RenderLivingEvent.Pre<EntityLivingBase>) {
        if (!inSkyblock || !config.customDamageSplash) return
        val entity = e.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return
        val strippedName = entity.customNameTag.removeFormatting()
        val damageMatcher = damagePattern.matchEntire(strippedName) ?: return
        //val name = entity.customNameTag
        val damage = damageMatcher.groups[1]!!.value.run {

            // maybe I will use it later
            when {
                //name.startsWith("§0") -> "${this}☠"
                //name.startsWith("§f") && !name.contains("§e") -> "${this}❂"
                //name.startsWith("§6") && !(name.contains("§e") || name.contains('ﬗ')) -> "${this}火"
                //name.startsWith("§3") -> "${this}水"
                else -> this
            }
        }

        entity.customNameTag = addRandomColorCodes(formatNumber(damage))
    }
}
