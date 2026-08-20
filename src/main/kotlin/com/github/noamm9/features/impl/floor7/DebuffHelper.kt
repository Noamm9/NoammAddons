package com.github.noamm9.features.impl.floor7

//#if CHEAT

import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW

object DebuffHelper: Feature(description = "Automatically pulls and fires bows based on Server Ticks (Lag Proof).") {
    private val semiAuto by BooleanConfig("Semi-Auto", true).withDescription("Automatically releases and re-draws the bow.").section("Options")
    private val soundEnabled by BooleanConfig("Play Sound", true).withDescription("Plays a sound when fully charged.")
    private val sound = createSoundSettings("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { soundEnabled.value }

    private val p1Ticks by NumberConfig("P1 Ticks", 8, 0, 20, 1).section("Ticks")
    private val p2Ticks by NumberConfig("P2 Ticks", 8, 0, 20, 1)
    private val p3Ticks by NumberConfig("P3 Ticks", 8, 0, 20, 1)
    private val p4Ticks by NumberConfig("P4 Ticks", 8, 0, 20, 1)
    private val purpleTicks by NumberConfig("Purple Dragon", 8, 0, 20, 1)
    private val greenTicks by NumberConfig("Green Dragon", 8, 0, 20, 1)
    private val redTicks by NumberConfig("Red Dragon", 8, 0, 20, 1)
    private val orangeTicks by NumberConfig("Orange Dragon", 8, 0, 20, 1)
    private val blueTicks by NumberConfig("Blue Dragon", 8, 0, 20, 1)

    private var isCharging = false
    private var holdingRC = false
    private var lastSequence = - 1
    private var ticksHeld = 0

    override fun init() {
        configSettings.filterIsInstance<NumberConfig<Int>>().forEach {
            if (it.min == 0 && it.max == 20 && it.step == 1 && it.defaultValue == 8) {
                it.withDescription("How many ticks should the bow be charged before it shoots. &e(Set to 0 to disable)")
            }
        }

        register<MouseClickEvent> {
            if (mc.screen != null) return@register
            if (event.button != 1) return@register
            holdingRC = event.action == GLFW.GLFW_PRESS
            if (holdingRC) return@register
            resetCharge()
        }

        register<PacketEvent.Sent> {
            if (event.packet !is ServerboundUseItemPacket) return@register
            if (! player.mainHandItem.skyblockId.contains("LAST_BREATH")) return@register
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
            if (! player.mainHandItem.skyblockId.contains("LAST_BREATH")) return@register resetCharge()

            ticksHeld ++

            val ticks = getTicks().takeIf { it > 0 } ?: return@register
            if (ticksHeld >= ticks) ThreadUtils.scheduledTask(0, ::fire)
        }
    }

    private fun fire() {
        if (soundEnabled.value) {
            sound.action.invoke()
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
        val phase = LocationUtils.F7Phase ?: return 0
        val player = player.position()

        return when (phase) {
            1 -> p1Ticks.value
            2 -> p2Ticks.value
            3 -> p3Ticks.value
            4 -> p4Ticks.value
            5 -> when {
                aabb(47, 8, 113, 64, 28, 135).contains(player) -> purpleTicks.value
                aabb(13, 5, 85, 40, 27, 103).contains(player) -> greenTicks.value
                aabb(13, 4, 47, 40, 20, 68).contains(player) -> redTicks.value
                aabb(72, 3, 47, 97, 31, 65).contains(player) -> orangeTicks.value
                aabb(72, 3, 85, 97, 31, 107).contains(player) -> blueTicks.value
                else -> 0
            }

            else -> 0
        }
    }
}
//#endif