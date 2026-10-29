package com.github.noamm9.features.impl.general

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.IFeatureScreen
import com.github.noamm9.ui.gui.CommandKeybindsScreen
import com.github.noamm9.utils.ChatUtils
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft

object CommandKeybinds: Feature("Create your own command keybinds"), IFeatureScreen {
    val keybinds = PogObject("commandKeybinds", linkedMapOf<String, String>())
    override fun createScreen() = CommandKeybindsScreen()
    override fun init() {
        register<KeyboardEvent.KeyPressed> {
            if(event.action != InputConstants.PRESS) return@register
            if(Minecraft.getInstance().gui.screen() != null) return@register
            val key = InputConstants.getKey(event.keyEvent).name
            val binds = keybinds.get()
            if(!binds.containsKey(key)) return@register
            ChatUtils.sendCommand(binds[key]!!);
        }
    }
}