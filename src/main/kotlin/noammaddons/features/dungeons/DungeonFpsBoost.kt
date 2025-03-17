package noammaddons.features.dungeons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.Utils.containsOneOf

object DungeonFpsBoost: Feature() {
    private val dungeonMobRegex = Regex("^(?:§.)*(.+)§r.+§c❤\$")

    private fun cancel(event: RenderLivingEvent.Specials.Pre<*>) {
        event.entity.setDead()
        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderNameTag(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.removeNonStarMobsNametag) return
        if (! inDungeon) return
        if (event.entity !is EntityArmorStand) return
        if (! event.entity.hasCustomName()) return
        val name = event.entity.customNameTag
        if (! name.matches(dungeonMobRegex)) return
        if (name.containsOneOf("Mimic", "Blaze")) return
        val isStarred = event.entity.customNameTag.contains("§6✯")
        if (isStarred && config.removeStarMobsNametag) return cancel(event)
        else if (! isStarred && config.removeNonStarMobsNametag) return cancel(event)
    }
}
