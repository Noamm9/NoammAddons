package com.github.noamm9.features.impl.floor7

//#if CHEAT

import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.AABB
import org.lwjgl.glfw.GLFW

object DebuffHelper: Feature(description = "Automatically pulls and fires bows based on Server Ticks (Lag Proof).") {
    private val semiAuto by ToggleSetting("Semi-Auto", true).withDescription("Automatically releases and re-draws the bow.").section("Options")
    private val soundEnabled by ToggleSetting("Play Sound", true).withDescription("Plays a sound when fully charged.")
    private val sound = createSoundSettings("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { soundEnabled.value }

    private val defaultTicks by SliderSetting("Default Ticks", 8, 0, 20, 1).withDescription("How many ticks should the bow be charged before it shoots. &e(Set to 0 to disable)").section("Ticks")
    private val p1Ticks by SliderSetting("P1 Ticks", 8, 0, 20, 1)
    private val p2Ticks by SliderSetting("P2 Ticks", 8, 0, 20, 1)
    private val p3Ticks by SliderSetting("P3 Ticks", 8, 0, 20, 1)
    private val p4Ticks by SliderSetting("P4 Ticks", 8, 0, 20, 1)
    private val purpleTicks by SliderSetting("Purple Dragon", 8, 0, 20, 1)
    private val greenTicks by SliderSetting("Green Dragon", 8, 0, 20, 1)
    private val redTicks by SliderSetting("Red Dragon", 8, 0, 20, 1)
    private val orangeTicks by SliderSetting("Orange Dragon", 8, 0, 20, 1)
    private val blueTicks by SliderSetting("Blue Dragon", 8, 0, 20, 1)

    private var isCharging = false
    private var ticksHeld = 0
    private var lastSequence = - 1
    private var holdingRC = false

    override fun init() {
        register<MouseClickEvent> {
            if (mc.screen != null) return@register
            if (event.button != 1) return@register
            holdingRC = event.action == GLFW.GLFW_PRESS
            if (holdingRC) return@register
            resetCharge()
        }

        register<PacketEvent.Sent> {
            if (event.packet !is ServerboundUseItemPacket) return@register
            val item = mc.player?.mainHandItem ?: return@register
            if (! item.skyblockId.contains("LAST_BREATH")) return@register
            lastSequence = event.packet.sequence
        }

        register<PacketEvent.Received> {
            if (event.packet !is ClientboundBlockChangedAckPacket) return@register
            if (event.packet.sequence != lastSequence) return@register

            isCharging = true
            ticksHeld = 0
        }

        register<TickEvent.Server> {
            if (mc.screen != null) return@register resetCharge()
            if (! isCharging || ! holdingRC) return@register
            val item = mc.player?.mainHandItem ?: return@register resetCharge()
            if (! item.skyblockId.contains("LAST_BREATH")) return@register resetCharge()

            ticksHeld ++

            val ticks = getTicks().takeIf { it > 0 } ?: return@register
            if (ticksHeld >= ticks) {
                ThreadUtils.scheduledTask(0, ::fire)
            }
        }
    }

    private fun fire() {
        if (soundEnabled.value) {
            sound.play.action.invoke()
        }

        if (! semiAuto.value) resetCharge()
        else {
            mc.options.keyUse.isDown = false

            resetCharge()

            ThreadUtils.scheduledTask(2) {
                if (holdingRC && mc.screen == null) {
                    mc.options.keyUse.isDown = true
                }
            }
        }
    }

    private fun resetCharge() {
        isCharging = false
        ticksHeld = 0
        lastSequence = - 1
    }

    private fun getTicks(): Int {
        val player = mc.player?.position() ?: return defaultTicks.value
        val phase = LocationUtils.F7Phase ?: return defaultTicks.value

        return when (phase) {
            1 -> p1Ticks.value
            2 -> p2Ticks.value
            3 -> p3Ticks.value
            4 -> p4Ticks.value
            5 -> when {
                AABB(47.0, 8.0, 113.0, 64.0, 28.0, 135.0).contains(player) -> purpleTicks.value
                AABB(13.0, 5.0, 85.0, 40.0, 27.0, 103.0).contains(player) -> greenTicks.value
                AABB(13.0, 4.0, 47.0, 40.0, 20.0, 68.0).contains(player) -> redTicks.value
                AABB(72.0, 3.0, 47.0, 97.0, 31.0, 65.0).contains(player) -> orangeTicks.value
                AABB(72.0, 3.0, 85.0, 97.0, 31.0, 107.0).contains(player) -> blueTicks.value
                else -> defaultTicks.value
            }

            else -> defaultTicks.value
        }
    }
}
//#endif