package com.github.noamm9.features.impl.misc

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket

object Camera: Feature() {
    @JvmField var flashFullFright = false

    @JvmStatic val fullBright by ToggleSetting("Full Bright").onChange {
        flashFullFright = true
    }

    @JvmStatic val noFrontCamera by ToggleSetting("Disable Front Camera").withDescription("Removes the front camera perspective.").section("Camera")
    @JvmStatic val noCameraClip by ToggleSetting("Camera Clip").withDescription("Allows your camera to clip in walls.").showIf { NoammAddons.isCheat }
    @JvmStatic val customCameraDistance by ToggleSetting("Custom Camera Distance").withDescription("Sets the distance of the camera from your player.").showIf { NoammAddons.isCheat }
    @JvmStatic val cameraDistance by SliderSetting("Camera Distance", 4, 1, 10, 0.1).withDescription("The distance of the camera from the player.").showIf { customCameraDistance.value && NoammAddons.isCheat }
    private val doubleSneakFix by ToggleSetting("Double Sneak Fix").withDescription("Prevents the server from setting your sneak state")
    @JvmStatic val inputFix by ToggleSetting("Riding Input Delay Fix").withDescription("Fixes high mouse input delay when riding an entity. (MC-206540)")

    @JvmStatic val hideFireOverlay by ToggleSetting("Hide Fire Overlay").withDescription("Hides the fire overlay on your screen.").section("Hide Overlays")
    @JvmStatic val hidePortalOverlay by ToggleSetting("Hide Portal Overlay").withDescription("Hides the portal overlay on your screen when you enter a portal.")
    @JvmStatic val hideWaterOverlay by ToggleSetting("Hide Water Overlay").withDescription("Hides the under water overlay on your screen.")
    @JvmStatic val hideBlockOverlay by ToggleSetting("Hide Block Overlay").withDescription("Hides the block that render on your screen when you are stuck inside a block.")

    @JvmStatic val disableBlindness by ToggleSetting("Disable Blindness").withDescription("Disables the Blindness effect.").section("Hide Effects")
    @JvmStatic val disableNausea by ToggleSetting("Disable Nausea").withDescription("Disables the Nausea effect")

    @JvmStatic val customFOV by ToggleSetting("Custom FOV").section("Custom FOV")
    @JvmStatic val customFOVSlider by SliderSetting("FOV", 110, 30, 179, 1).hideIf { ! customFOV.value }

    override fun init() {
        register<GameStartEvent> {
            if (NoammAddons.isCheat) return@register
            noCameraClip.value = false
            customCameraDistance.value = false
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! doubleSneakFix.value) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            if (player.id != packet.id) return@register
            packet.packedItems.removeIf { it.id == 6 }
        }
    }
}