package noammaddons.features.dungeons.ESP

import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityModelEvent
import noammaddons.features.Feature
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.Utils.isNull

object HiddenMobs: Feature() {
    private var watcherMobs: List<String>? = null

    init {
        fetchJsonWithRetry<List<String>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/watcherMobs.json"
        ) { watcherMobs = it }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityModelEvent) {
        if (watcherMobs.isNull()) return
        if (! inDungeons) return
        if (event.entity.isInvisible && when (event.entity) {
                is EntityEnderman -> config.showFels && event.entity.name == "Dinnerbone"
                is EntityPlayer -> config.showShadowAssassin && event.entity.name.contains("Shadow Assassin") ||
                        config.showStealthy && watcherMobs !!.any { event.entity.name.trim() == it }

                else -> false
            }
        ) event.entity.isInvisible = false
    }
}
