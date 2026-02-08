package com.github.noamm9.features.impl.tweaks

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.*
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting

object Camera: Feature() {
    private val fullBright by ToggleSetting("Full Bright")

    @JvmStatic
    val legacySneakHeight by ToggleSetting("1.8 Sneak height").withDescription("Changes the sneak height back to its 1.8 height while maitaining all vanilla behevior (visual only)").section("Camera")

    @JvmStatic
    val noFrontCamera by ToggleSetting("Disable Front Camera").withDescription("Removes the front camera perspective.")

    @JvmStatic
    val noCameraClip by ToggleSetting("Camera Clip").withDescription("Allows ur camera to clip in walls")

    @JvmStatic
    val customCameraDistance by ToggleSetting("Custom Camera Distance").withDescription("Sets the distance of the camera from ur player")

    @JvmStatic
    val cameraDistance by SliderSetting("Camera Distance", 4, 1, 10, 0.1).withDescription("The distance of the camera from ur player").showIf { customCameraDistance.value }

    @JvmStatic
    val hideFireOverlay by ToggleSetting("Hide Fire Overlay").withDescription("Hides the fire overlay on ur screen").section("Hide Overlays")

    @JvmStatic
    val hidePortalOverlay by ToggleSetting("Hide Portal Overlay").withDescription("Hides the portal overlay on ur screen when you enter a portal")

    @JvmStatic
    val hideWaterOverlay by ToggleSetting("Hide Water Overlay").withDescription("Hides the under water overlay on ur screen")

    @JvmStatic
    val hideBlockOverlay by ToggleSetting("Hide Block Overlay").withDescription("Hides the Block that render on your screen when u are stuck inside a block")

    @JvmStatic
    val disableBlindness by ToggleSetting("Disable Blindness").withDescription("Disables the Blindness potion effect").section("Hide Effects")

    @JvmStatic
    val disableDarkness by ToggleSetting("Disable Darkness").withDescription("Disables the Darkness potion effect")

    @JvmStatic
    val disableNausea by ToggleSetting("Disable Nausea").withDescription("Disables the Nausea potion effect")

    @JvmStatic
    val isFullBright get() = enabled && fullBright.value

    @JvmStatic
    val customFOV by ToggleSetting("Custom FOV").section("Custom FOV")

    @JvmStatic
    val customFOVSlider by SliderSetting("Custom FOV", 70, 30, 200, 5).hideIf { !customFOV.value }
}