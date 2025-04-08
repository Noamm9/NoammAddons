package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.KeyBinds
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils.useDungeonClassAbility

object AbilityKeybinds: Feature() {
    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! config.DungeonAbilityKeybinds) return
        if (! inDungeon) return
        if (! dungeonStarted) return

        when {
            KeyBinds.DungeonClassUltimate.isPressed -> useDungeonClassAbility(true)
            KeyBinds.DungeonClassAbility.isPressed -> useDungeonClassAbility(false)
        }
    }
}
