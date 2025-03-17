package noammaddons.features.dungeons.esp

import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostRenderEntityModelEvent
import noammaddons.features.Feature
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.inDungeon

object HiddenMobs: Feature() {
    private var watcherMobs: List<String>? = null

    init {
        fetchJsonWithRetry<List<String>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/watcherMobs.json"
        ) { watcherMobs = it }
    }

    @SubscribeEvent
    fun onRenderEntity(event: PostRenderEntityModelEvent) {
        if (watcherMobs == null) return
        if (! inDungeon) return
        if (event.entity.isInvisible && when (val entity = event.entity) {
                is EntityEnderman -> config.showFels && entity.name == "Dinnerbone"
                is EntityPlayer -> config.showShadowAssassin && entity.name.contains("Shadow Assassin") ||
                        config.showStealthy && watcherMobs !!.any { entity.name.trim() == it }

                else -> false
            }
        ) event.entity.isInvisible = false
    }
}
