package noammaddons.features.impl.esp

import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.WebUtils


object HiddenMobs: Feature("Reveals invisible mobs in dungeons") {
    private var watcherMobs: List<String>? = null

    private val showFels by ToggleSetting("Show Fels")
    private val showSa by ToggleSetting("Show Shadow Assasians")
    private val showStealthy by ToggleSetting("Show Stealthy")

    init {
        WebUtils.fetchJson<List<String>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/watcherMobs.json"
        ) { watcherMobs = it }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (watcherMobs == null) return
        if (! inDungeon) return
        if (event.entity.isInvisible && when (val entity = event.entity) {
                is EntityEnderman -> showFels && entity.name == "Dinnerbone"
                is EntityPlayer -> showSa && entity.name.contains("Shadow Assassin") ||
                        showStealthy && watcherMobs !!.any { entity.name.trim() == it }

                else -> false
            }
        ) event.entity.isInvisible = false
    }
}
