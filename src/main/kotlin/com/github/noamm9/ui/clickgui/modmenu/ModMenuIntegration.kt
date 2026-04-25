package com.github.noamm9.ui.clickgui.modmenu

import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

// see fabric.mod.json5
@Suppress("unused")
class ModMenuIntegration: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { ClickGuiScreen }
    }
}