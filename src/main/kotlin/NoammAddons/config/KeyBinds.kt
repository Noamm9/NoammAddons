package NoammAddons.config

import NoammAddons.NoammAddons.Companion.MOD_NAME
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard

object KeyBinds {
    val GhostPick = KeyBinding("Ghost Blocks", Keyboard.KEY_Z, MOD_NAME)
    val Config = KeyBinding("Config", Keyboard.KEY_RSHIFT, MOD_NAME)
    val DungeonClassUltimate = KeyBinding("Dungeon class Ultimate", Keyboard.KEY_GRAVE, MOD_NAME)
    val DungeonClassAbility = KeyBinding("Dungeon class Ability", 56, MOD_NAME)

    val allBindings: List<KeyBinding> by lazy {
        listOf(GhostPick, Config, DungeonClassUltimate, DungeonClassAbility)
    }
}