package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import net.minecraft.world.entity.Entity

object NameTagTweaks: Feature(name = "Nametag Tweaks") {
    @JvmStatic
    val forceNametag by ToggleSetting("Force Nametag").withDescription("Makes player nametags always visible, even while sneaking or invisible.")

    @JvmStatic
    val disableNametagBackground by ToggleSetting("Hide Nametag Background").withDescription("Disable Nametag's black background.")

    @JvmStatic
    val addNameTagTextShadow by ToggleSetting("Shadowed Nametag").withDescription("Adds a text shadow to the nametag label.")

    @JvmStatic
    val showOwnNametag by ToggleSetting("Show Own Nametag").withDescription("Renders your own nametag above your head.")

    @JvmStatic
    fun shouldShowNametag(entity: Entity): Boolean {
        if (! enabled) return false
        return if (mc.player == entity) showOwnNametag.value else forceNametag.value
    }
}