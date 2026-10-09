package com.github.noamm9.features.impl.misc

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket

//#if LEGIT
//$import java.util.*
//#endif

object Camera: Feature() {
    @JvmField var flashFullFright = false

    @JvmStatic val fullBright by ToggleSetting("Full Bright").onChange {
        flashFullFright = true
    }

    @JvmStatic val noFrontCamera by ToggleSetting("Disable Front Camera").withDescription("Removes the front camera perspective.").section("Camera")
    //#if CHEAT
    @JvmStatic val noCameraClip by ToggleSetting("Camera Clip").withDescription("Allows your camera to clip in walls.").showIf { NoammAddons.isCheat }
    @JvmStatic val customCameraDistance by ToggleSetting("Custom Camera Distance").withDescription("Sets the distance of the camera from your player.").showIf { NoammAddons.isCheat }
    @JvmStatic val cameraDistance by SliderSetting("Camera Distance", 4, 1, 10, 0.1).withDescription("The distance of the camera from the player.").showIf { customCameraDistance.value && NoammAddons.isCheat }
    //#else
    //$@JvmStatic val noCameraClip by ToggleSetting("Camera Clip", false).hideIf { true }.jsonName(UUID.randomUUID().toString())
    //$@JvmStatic val customCameraDistance by ToggleSetting("Custom Camera Distance", false).hideIf { true }.jsonName(UUID.randomUUID().toString())
    //$@JvmStatic val cameraDistance by SliderSetting("Camera Distance", 4, 1, 10, 0.1).hideIf { true }.jsonName(UUID.randomUUID().toString())
    //#endif
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

    @JvmStatic val viewBobbing by ToggleSetting("View Bobbing", true).withDescription("Overrides the vanilla View Bobbing option.").section("View Bobbing")
    private val disableCameraBobbing by ToggleSetting("Disable Camera Bobbing").withDescription("Stops the camera from bobbing while walking.").hideIf { ! viewBobbing.value }
    private val cameraBobbing by SliderSetting("Camera Bobbing", 1.0, 0.0, 2.0, 0.05).hideIf { ! viewBobbing.value || disableCameraBobbing.value }
    private val disableHandBobbing by ToggleSetting("Disable Hand Bobbing").withDescription("Stops your hand from bobbing while walking.").hideIf { ! viewBobbing.value }
    private val handBobbing by SliderSetting("Hand Bobbing", 1.0, 0.0, 2.0, 0.05).hideIf { ! viewBobbing.value || disableHandBobbing.value }

    @JvmStatic
    fun getBobScale(hand: Boolean): Float = when {
        ! enabled -> 1f
        hand -> if (disableHandBobbing.value) 0f else handBobbing.value.toFloat()
        else -> if (disableCameraBobbing.value) 0f else cameraBobbing.value.toFloat()
    }

    override fun init() {
        //#if LEGIT
        register<GameStartEvent> {
            noCameraClip.value = false
            customCameraDistance.value = false
        }
        //#endif

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! doubleSneakFix.value) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            if (player.id != packet.id) return@register
            packet.packedItems.removeIf { it.id == 6 }
        }
    }
}