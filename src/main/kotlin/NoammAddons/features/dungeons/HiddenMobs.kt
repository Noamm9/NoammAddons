package NoammAddons.features.dungeons

import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.events.RenderLivingEntityEvent
import NoammAddons.utils.LocationUtils.inDungeons

object HiddenMobs {

    private val watcherMobs = listOf(
        "Revoker",
        "Psycho",
        "Reaper",
        "Cannibal",
        "Mute",
        "Ooze",
        "Putrid",
        "Freak",
        "Leech",
        "Tear",
        "Parasite",
        "Flamer",
        "Skull",
        "Mr. Dead",
        "Vader",
        "Frost",
        "Walker",
        "Wandering Soul",
        "Bonzo",
        "Scarf",
        "Livid"
    )

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEntityEvent) {
        if (!inDungeons) return
        if (event.entity.isInvisible && when (event.entity) {
                is EntityEnderman -> config.showFels && event.entity.name == "Dinnerbone"
                is EntityPlayer -> config.showShadowAssassin && event.entity.name.contains("Shadow Assassin") ||
                        config.showStealthy && watcherMobs.any { event.entity.name.trim() == it }
                else -> false
            }
        ) {
            event.entity.isInvisible = false
        }
    }
}
