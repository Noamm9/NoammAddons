package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.section
import com.github.noamm9.ui.clickgui.componnents.showIf
import com.github.noamm9.utils.PlayerUtils.useDungeonClassAbility
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import org.lwjgl.glfw.GLFW

object AbilityKeybinds: Feature("Allows you do use your dungeon class ult/ability with a keybind") {
    private val classUltimate by ToggleSetting("Class Ultimate", true)
    private val classAbility by ToggleSetting("Class Ability", true)
    private val ultKeybind by KeybindSetting("Ultimate Keybind").showIf { classUltimate.value }.section("keybinds")
    private val abilityKeybind by KeybindSetting("Ability Keybind").showIf { classAbility.value }

    override fun init() {
        register<KeyboardEvent> {
            if (! LocationUtils.inDungeon || ! DungeonListener.dungeonStarted) return@register
            if (event.scanCode != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register

            if (classUltimate.value && ultKeybind.isPressed()) {
                event.isCanceled = true
                return@register useDungeonClassAbility(true)
            }

            if (classAbility.value && abilityKeybind.isPressed()) {
                event.isCanceled = true
                mc.player?.eyeHeight
                return@register useDungeonClassAbility(false)
            }
        }
    }
}