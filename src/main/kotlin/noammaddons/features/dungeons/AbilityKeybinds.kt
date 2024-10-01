package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.config.KeyBinds
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.useDungeonClassAbility
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.features.dungeons.GhostPick.featureState

object AbilityKeybinds {

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!inDungeons || !config.DungeonAbilityKeybinds) return
	    val oldValue = featureState
        if (KeyBinds.DungeonClassUltimate.isPressed) {
	        featureState = false
	        useDungeonClassAbility(Ultimate = true)
	        featureState = oldValue
		}
	    if (KeyBinds.DungeonClassAbility.isPressed) {
		    featureState = false
		    useDungeonClassAbility(Ultimate = false)
		    featureState = oldValue
	    }
    }
}
