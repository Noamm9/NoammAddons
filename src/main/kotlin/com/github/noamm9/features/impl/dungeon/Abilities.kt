package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.mojang.blaze3d.platform.InputConstants
import gg.essential.universal.UMinecraft

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
    private val autoUltDelay by SliderSetting(name = "Auto delay", 0, 0, 100, 1, suffix = "ticks").showIf { autoUlt.value }

    private class UltMessage(val msg: String, val classes: List<DungeonClass>, val floor: Int)

    private val ultMessages = listOf(
        UltMessage(
            msg = "⚠ Maxor is enraged! ⚠",
            classes = listOf(DungeonClass.Healer, DungeonClass.Tank),
            floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Goldor: You have done it, you destroyed the factory...",
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
            if (event.action != InputConstants.PRESS) return@register
            if (UMinecraft.currentScreenObj != null) return@register

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

            ThreadUtils.scheduledTaskServer(autoUltDelay.value) {
                PlayerUtils.useDungeonClassAbility(true)
                ChatUtils.modMessage("Used Ultimate!")
            }
        }
        //#endif
    }
}
