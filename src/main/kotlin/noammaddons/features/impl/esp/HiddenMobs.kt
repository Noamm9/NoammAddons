package noammaddons.features.impl.esp

import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.DataDownloader
import noammaddons.utils.LocationUtils.inDungeon


object HiddenMobs: Feature("Reveals invisible mobs in dungeons") {
    private var watcherMobs = DataDownloader.loadJson<List<String>>("watcherMobs.json")

    private val showFels by ToggleSetting("Show Fels")
    private val showSa by ToggleSetting("Show Shadow Assassins")
    private val showStealthy by ToggleSetting("Show Stealthy")
    
    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (! inDungeon) return
        if (! event.entity.isInvisible) return
        val isFel = event.entity is EntityEnderman && showFels && event.entity.name == "Dinnerbone"
        val isSA = event.entity is EntityPlayer && showSa && event.entity.name.contains("Shadow Assassin")
        val isWatcherMob = event.entity is EntityPlayer && showStealthy && watcherMobs.any { event.entity.name.trim() == it }
        if (isFel || isSA || isWatcherMob) event.entity.isInvisible = false
    }
}