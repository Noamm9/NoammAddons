package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting

object NameTagTweaks: Feature(name = "Nametag Tweaks") {
    @JvmStatic
    val disableNametagBackground by ToggleSetting("Hide Nametag Background").withDescription("Disable Nametag's black background.")

    @JvmStatic
    val addNameTagTextShadow by ToggleSetting("Shadowed Nametag").withDescription("Adds a text shadow to the nametag label.")
}