package com.github.noamm9.ui.clickgui.modmenu

import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> ClickGuiScreen }
    }
}