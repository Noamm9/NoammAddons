package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.keybinds
import NoammAddons.utils.LocationUtils.inSkyblock
import NoammAddons.utils.PlayerUtils.useDungeonClassAbility
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object AbilityKeybinds {

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!inSkyblock || !config.DungeonAbilityKeybinds) return
        if (keybinds[2].isPressed) useDungeonClassAbility(true)
        if (keybinds[3].isPressed) useDungeonClassAbility(false)
    }
}
