package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PreKeyInputEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils.useDungeonClassAbility

object AbilityKeybinds: Feature("Allows you do use your dungeon class ult/ability with a keybind") {
    val classUltimate = ToggleSetting("Class Ultimate", true)
    val classAbility = ToggleSetting("Class Ability", true)
    val ultKeybind = KeybindSetting("Ultimate Keybind")
    val abilityKeybind = KeybindSetting("Ability Keybind")

    override fun init() = addSettings(
        classUltimate, classAbility,
        SeperatorSetting("Keybinds"),
        ultKeybind, abilityKeybind
    )

    @SubscribeEvent
    fun onKeyInput(event: PreKeyInputEvent) {
        if (! inDungeon || ! dungeonStarted) return
        if (classUltimate.value && event.key == ultKeybind.value) useDungeonClassAbility(true)
        if (classAbility.value && event.key == abilityKeybind.value) useDungeonClassAbility(false)
    }
}
