package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import org.lwjgl.glfw.GLFW

//#if CHEAT
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.dungeons.enums.DungeonClass
//#endif

object Abilities: Feature(
    //#if CHEAT
    "Allows you to use dungeon class abilities with keybinds and automatically trigger ultimates when needed."
    //#else
    //$"Allows you to use dungeon class abilities with keybinds."
    //#endif
) {
    private val ultKeybind by KeybindSetting("Ultimate Keybind").section("Keybinds")
    private val abilityKeybind by KeybindSetting("Ability Keybind")

    //#if CHEAT
    private val autoUlt by ToggleSetting("Auto Use Ultimate").section("Auto Ultimate")

    private class UltMessage(val msg: String, val classes: List<DungeonClass>, val floor: Int)

    private val ultMessages = listOf(
        UltMessage(
            msg = "⚠ Maxor is enraged! ⚠",
            classes = listOf(DungeonClass.Healer, DungeonClass.Tank),
            floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Goldor: You have done it, you destroyed the factory…",
            classes = listOf(DungeonClass.Healer, DungeonClass.Tank),
            floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Sadan: My giants! Unleashed!",
            classes = listOf(DungeonClass.Healer, DungeonClass.Tank, DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Mage),
            floor = 6
        ),
        UltMessage(
            msg = "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.",
            classes = listOf(DungeonClass.Healer, DungeonClass.Tank),
            floor = 5
        )
    )
    //#endif

    override fun init() {
        register<KeyboardEvent.KeyPressed> {
            if (! LocationUtils.inDungeon || ! DungeonListener.dungeonStarted) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register

            if (ultKeybind.isPressed()) {
                PlayerUtils.useDungeonClassAbility(true)
                event.isCanceled = true
                return@register
            }

            if (abilityKeybind.isPressed()) {
                PlayerUtils.useDungeonClassAbility(false)
                event.isCanceled = true
                return@register
            }
        }

        //#if CHEAT
        register<ChatMessageEvent> {
            if (! autoUlt.value || ! LocationUtils.inBoss) return@register
            val msg = event.unformattedText
            val matchingMessage = ultMessages.find {
                it.msg == msg && it.floor == LocationUtils.dungeonFloorNumber
            } ?: return@register

            if (DungeonListener.thePlayer?.clazz !in matchingMessage.classes) return@register
            PlayerUtils.useDungeonClassAbility(true)
            ChatUtils.modMessage("Used Ultimate!")
        }
        //#endif
    }
}
