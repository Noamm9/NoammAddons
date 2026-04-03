package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.PlayerUtils.findHotbarSlot
import com.github.noamm9.utils.PlayerUtils.swapToSlot
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object LivingSnake: Feature("Swaps to Frozen Water Pungi after each mined snake segment, then swaps back to your pickaxe.") {
    private const val PICKAXE_ID = "SELF_RECURSIVE_PICKAXE"
    private const val PUNGI_ID = "FROZEN_WATER_PUNGI"
    private const val TARGET_TIMEOUT_MS = 750L

    private val holdTime by SliderSetting("Pungi Hold Time", 300, 100, 1000, 25)
        .withDescription("How long to hold right click with the Frozen Water Pungi.")
        .section("Timing")

    private val triggerCooldown by SliderSetting("Trigger Cooldown", 250, 0, 1000, 25)
        .withDescription("Minimum time between snake stun cycles.")

    private var lastTargetedPos: net.minecraft.core.BlockPos? = null
    private var lastTargetedAt = 0L
    private var lastTriggerAt = 0L

    private var pendingPungiSlot: Int? = null
    private var returnSlot: Int? = null
    private var holdingUntil = 0L

    override fun onDisable() {
        releaseUse()
        resetState()
        super.onDisable()
    }

    override fun init() {
        register<WorldChangeEvent> {
            releaseUse()
            resetState()
        }

        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> {
            if (! shouldRun()) return@register
            if (event.item.skyblockId != PICKAXE_ID) return@register
            if (! isSnakeSegment(mc.level?.getBlockState(event.pos) ?: return@register)) return@register

            lastTargetedPos = event.pos
            lastTargetedAt = System.currentTimeMillis()
        }

        register<BlockChangeEvent> {
            if (! shouldRun()) return@register
            if (! event.newState.isAir) return@register
            if (! isSnakeSegment(event.oldState)) return@register

            val player = mc.player ?: return@register
            if (player.mainHandItem.skyblockId != PICKAXE_ID) return@register

            val now = System.currentTimeMillis()
            if (now - lastTargetedAt > TARGET_TIMEOUT_MS) return@register
            if (event.pos != lastTargetedPos) return@register
            if (now - lastTriggerAt < triggerCooldown.value.toLong()) return@register
            if (pendingPungiSlot != null || holdingUntil > now) return@register

            val pungiSlot = findHotbarSlot { it.skyblockId == PUNGI_ID } ?: return@register

            returnSlot = player.inventory.selectedSlot
            pendingPungiSlot = pungiSlot
            lastTriggerAt = now
        }

        register<TickEvent.Start> {
            val now = System.currentTimeMillis()

            pendingPungiSlot?.let { slot ->
                swapToSlot(slot)
                mc.options.keyUse.isDown = true
                PlayerUtils.rightClick()
                holdingUntil = now + holdTime.value
                pendingPungiSlot = null
                return@register
            }

            if (holdingUntil <= 0L) return@register

            if (now < holdingUntil) {
                mc.options.keyUse.isDown = true
                return@register
            }

            releaseUse()
            returnSlot?.let(::swapToSlot)
            resetState()
        }
    }

    private fun shouldRun(): Boolean {
        if (LocationUtils.world != WorldType.Rift) return false
        if (! enabled) return false
        return true
    }

    private fun isSnakeSegment(state: BlockState): Boolean {
        return state.`is`(Blocks.LAPIS_BLOCK) || state.`is`(Blocks.BLUE_STAINED_GLASS)
    }

    private fun releaseUse() {
        mc.options?.keyUse?.isDown = false
    }

    private fun resetState() {
        pendingPungiSlot = null
        returnSlot = null
        holdingUntil = 0L
        lastTargetedPos = null
        lastTargetedAt = 0L
    }
}
