package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.config.KeyBinds
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.PlayerUtils.useDungeonClassAbility
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object AbilityKeybinds {

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!inDungeons || !config.DungeonAbilityKeybinds) return
        if (KeyBinds.DungeonClassUltimate.isPressed) useDungeonClassAbility(Ultimate = true)
        else if (KeyBinds.DungeonClassAbility.isPressed) useDungeonClassAbility(Ultimate = false)
    }
}
