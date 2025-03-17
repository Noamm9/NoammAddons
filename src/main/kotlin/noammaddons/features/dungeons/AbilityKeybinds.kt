package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.KeyBinds
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.features.dungeons.GhostPick.featureState
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils.useDungeonClassAbility
import noammaddons.utils.ThreadUtils.setTimeout

object AbilityKeybinds: Feature() {
    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! config.DungeonAbilityKeybinds) return
        if (! inDungeon) return

        when {
            KeyBinds.DungeonClassUltimate.isPressed -> {
                if (! featureState) useDungeonClassAbility(true)
                else {
                    featureState = false
                    useDungeonClassAbility(true)
                    setTimeout(50) { featureState = true }
                }
            }

            KeyBinds.DungeonClassAbility.isPressed -> {
                if (! featureState) useDungeonClassAbility(false)
                else {
                    featureState = false
                    useDungeonClassAbility(false)
                    setTimeout(50) { featureState = true }
                }
            }
        }
    }
}
