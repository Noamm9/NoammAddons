package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.world.entity.Entity

object NameTagTweaks: Feature(name = "Nametag Tweaks") {
    private val showOwnNametag by ToggleSetting("Show Own Nametag").withDescription("Renders your own nametag above your head.")
    private val forceNametag by ToggleSetting("Force Nametag").withDescription("Makes player nametags always visible, even while sneaking or invisible.")
    @JvmStatic val disableNametagBackground by ToggleSetting("Hide Nametag Background").withDescription("Disable Nametag's black background.")
    @JvmStatic val addNameTagTextShadow by ToggleSetting("Shadowed Nametag").withDescription("Adds a text shadow to the nametag label.")
    private val hideDinnerboneNametag by ToggleSetting("Hide Dinnerbone Nametag").withDescription("Hides the nametag of any entity named \"Dinnerbone\".")

    @JvmStatic
    fun isForceNametagActive() = enabled && forceNametag.value && LocationUtils.inSkyblock

    @JvmStatic
    fun shouldShowNametag(entity: Entity): Boolean {
        if (! enabled) return false
        if (! showOwnNametag.value) return false
        if (player != entity) return false
        return true
    }

    @JvmStatic
    fun shouldHideDinnerbone(entity: Entity): Boolean {
        if (! enabled) return false
        if (! hideDinnerboneNametag.value) return false
        return entity.customName?.string.equals("Dinnerbone", ignoreCase = true)
    }
}
