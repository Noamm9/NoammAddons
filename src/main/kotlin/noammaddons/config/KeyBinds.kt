package noammaddons.config

import net.minecraft.client.settings.KeyBinding
import noammaddons.noammaddons.Companion.MOD_NAME
import org.lwjgl.input.Keyboard

object KeyBinds {
    val GhostPick = KeyBinding("Ghost Pick", Keyboard.KEY_NONE, MOD_NAME)
    val Config = KeyBinding("Config", Keyboard.KEY_RSHIFT, MOD_NAME)
    val DungeonClassUltimate = KeyBinding("Dungeon class Ultimate", Keyboard.KEY_GRAVE, MOD_NAME)
    val DungeonClassAbility = KeyBinding("Dungeon class Ability", Keyboard.KEY_NONE, MOD_NAME)
    val SlotBindingAddBinding = KeyBinding("Slot Binding - Add Binding", Keyboard.KEY_NONE, MOD_NAME)
    val SlotBindingRemoveBinding = KeyBinding("Slot Binding - Remove Binding", Keyboard.KEY_NONE, MOD_NAME)


    val allBindings: List<KeyBinding> by lazy {
        listOf(
            Config,
            GhostPick,
            DungeonClassUltimate,
            DungeonClassAbility,
            SlotBindingAddBinding,
            SlotBindingRemoveBinding
        )
    }
}