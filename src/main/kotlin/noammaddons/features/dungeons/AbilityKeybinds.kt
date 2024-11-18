package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.KeyBinds
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.features.dungeons.GhostPick.featureState
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.useDungeonClassAbility
import noammaddons.utils.ThreadUtils.setTimeout

object AbilityKeybinds : Feature() {
    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! inDungeons || ! config.DungeonAbilityKeybinds) return
        if (KeyBinds.DungeonClassUltimate.isPressed) {
            if (featureState) {
                featureState = false
                useDungeonClassAbility(true)
                setTimeout(50) { featureState = true }
            } else useDungeonClassAbility(true)
        }
        if (KeyBinds.DungeonClassAbility.isPressed) {
            if (featureState) {
                featureState = false
                useDungeonClassAbility(false)
                setTimeout(50) { featureState = true }
            } else useDungeonClassAbility(false)
        }
    }
}
