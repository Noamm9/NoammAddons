package com.github.noamm9.features.impl.floor7.devices

//#if CHEAT

import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.floor7.devices.I4Helper.getPredictionTarget
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.utils.Animation.Companion.easeInOutCubic
import com.github.noamm9.utils.ActionUtils
import com.github.noamm9.utils.ActionUtils.queue
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.MathUtils.calcYawPitch
import com.github.noamm9.utils.MathUtils.interpolateYaw
import com.github.noamm9.utils.MathUtils.lerp
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.PlayerUtils.leapAction
import com.github.noamm9.utils.PlayerUtils.rotate
import com.github.noamm9.utils.ThreadUtils.setTimeout
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import kotlinx.coroutines.delay
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

object AutoI4: Feature("Fully Automated I4") {
    private val rotationTime by SliderSetting<Long>("Rotation Time", 170, 0, 250, 1).withDescription("Time (ms) to interpolate rotations when aiming at dev block targets. &eSet to 0 to disable the auto rotation.")
    private val predictSetting by ToggleSetting("Predictions", true).withDescription("Enables prediction logic to aim at the next target block.")

    private val rodSetting by ToggleSetting("Auto Rod", true)
    private val maskSetting by ToggleSetting("Auto Mask", true)
    private val leapSetting by ToggleSetting("Auto Leap", true)
    private val leapPriorities = listOf(DungeonClass.Tank, DungeonClass.Mage, DungeonClass.Healer, DungeonClass.Archer)
    private val preferredLeapClass by DropdownSetting("Leap Priority", 0, leapPriorities.map { it.name })

    private const val STORM_DEATH_MESSAGE = "[BOSS] Storm: I should have known that I stood no chance."
    @Volatile private var state = PhaseState()

    override fun init() {
        register<ChatMessageEvent> {
            if (! LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 7) return@register
            val msg = event.unformattedText
            if (msg == STORM_DEATH_MESSAGE) state = PhaseState(tickTimer = 0).also {
                setTimeout(30_000L) { state = state.copy(tickTimer = - 1) }
            }
            else if (msg.contains("completed a device!")) {
                if (! (state.tickTimer >= 0 && I4Helper.isOnDev() && leapSetting.value)) return@register
                if (I4Helper.DEVICE_DONE_REGEX.find(msg)?.groupValues?.get(1) != mc.user.name) return@register
                onComplete()
            }
        }

        register<TickEvent.Server> {
            if (state.tickTimer == - 1) return@register
            state = state.copy(tickTimer = state.tickTimer + 1)
            if (! I4Helper.isOnDev()) return@register

            when (state.tickTimer) {
                307 if leapSetting.value -> queue(4, true, ::saveLeap)
                244 if maskSetting.value && ! state.hasChangedMask -> queue(3, true, PlayerUtils::changeMaskAction)
                174 if rodSetting.value -> queue(2, true, PlayerUtils::rodSwap)
                174 if maskSetting.value -> {
                    state = state.copy(hasChangedMask = true)
                    queue(3, true, PlayerUtils::changeMaskAction)
                }
            }
        }

        register<BlockChangeEvent> {
            if (! LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 7) return@register
            if (! I4Helper.isOnDev()) {
                state = PhaseState(tickTimer = state.tickTimer)
                return@register
            }
            if (! mc.player !!.mainHandItem.`is`(Items.BOW)) return@register
            if (event.pos !in I4Helper.devBlocks) return@register

            if (event.oldBlock == Blocks.EMERALD_BLOCK && event.newBlock == Blocks.BLUE_TERRACOTTA) {
                state = state.copy(doneCoords = state.doneCoords + event.pos)
                return@register
            }

            if (event.newBlock != Blocks.EMERALD_BLOCK) return@register

            if (rotationTime.value > 0) queue(1) {
                shootAtBlock(event.pos)
                state = state.copy(lastTarget = event.pos)

                if (predictSetting.value) {
                    val next = I4Helper.prediction ?: getPredictionTarget(event.pos, state.doneCoords) ?: return@queue
                    if (getEmerald(event.pos, next) != null) return@queue
                    shootAtBlock(next)
                }
            }
        }
    }

    private fun getEmerald(vararg exclude: BlockPos?) = I4Helper.devBlocks.find {
        if (it in exclude) return@find false
        if (it in state.doneCoords) return@find false
        WorldUtils.getBlockAt(it) == Blocks.EMERALD_BLOCK
    }

    private fun getTargetVector(pos: BlockPos): Vec3 {
        val i = I4Helper.devBlocks.indexOf(pos).coerceAtLeast(0)
        val col = i % 3
        val row = i / 3

        val isLeftDone = (col < 2) && (I4Helper.devBlocks[i + 1] in state.doneCoords)
        val isRightDone = (col > 0) && (I4Helper.devBlocks[i - 1] in state.doneCoords)

        val targetX = when (col) {
            0 -> 67.5
            2 -> 65.5
            else -> when {
                isRightDone && ! isLeftDone -> 65.5
                isLeftDone && ! isRightDone -> 67.5
                else -> if (Math.random() < 0.5) 65.5 else 67.5
            }
        }

        val targetY = 131 - 2.0 * row
        return Vec3(targetX, targetY, 50.0)
    }

    private suspend fun shootAtBlock(pos: BlockPos) {
        val player = mc.player ?: return
        val (yaw, pitch) = calcYawPitch(getTargetVector(pos))
        val block = suspend {
            delay(50)
            PlayerUtils.rightClick()
        }

        getEmerald(state.lastTarget, pos)?.let { newer ->
            state = state.copy(doneCoords = state.doneCoords + newer)
            return shootAtBlock(newer)
        }

        val currentYaw = MathUtils.normalizeYaw(player.yRot)
        val currentPitch = MathUtils.normalizePitch(player.xRot)
        val targetYaw = MathUtils.normalizeYaw(yaw)
        val targetPitch = MathUtils.normalizePitch(pitch)
        val tolerance = 1f

        if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) return block()

        val startTime = System.currentTimeMillis()
        val duration = rotationTime.value.toDouble() * (0.9 + Math.random() * 0.2)
        while (true) {
            val newerDuring = getEmerald(state.lastTarget, pos)
            if (newerDuring != null) {
                state = state.copy(doneCoords = state.doneCoords + newerDuring)
                return shootAtBlock(newerDuring)
            }

            val elapsed = System.currentTimeMillis() - startTime
            val progress = min(elapsed.toDouble() / duration, 1.0)

            if (progress >= 1) {
                rotate(targetYaw, targetPitch)
                block()
                break
            }

            val easedProgress = easeInOutCubic(progress).toFloat()
            val newYaw = interpolateYaw(currentYaw, targetYaw, easedProgress)
            val newPitch = lerp(currentPitch, targetPitch, easedProgress).toFloat()
            rotate(newYaw, newPitch)
            delay(1)
        }
    }

    private suspend fun saveLeap() {
        if (! leapSetting.value) return
        if (state.hasLeaped) return
        state = state.copy(hasLeaped = true, tickTimer = - 1)
        val aliveTeammates = DungeonListener.dungeonTeammatesNoSelf.filterNot { it.isDead }

        val preferredClass = leapPriorities[preferredLeapClass.value]
        val target = aliveTeammates.find { it.clazz == preferredClass }
            ?: leapPriorities.firstNotNullOfOrNull { priority ->
                aliveTeammates.find { it.clazz == priority }
            } ?: return

        leapAction(target)
        while (I4Helper.isOnDev()) delay(50)
        ActionUtils.reset()
    }

    private fun onComplete() {
        if (state.hasAlerted) return
        state = state.copy(hasAlerted = true, tickTimer = - 1)
        queue(4, true, ::saveLeap)
    }

    private data class PhaseState(
        val tickTimer: Int = - 1,
        val doneCoords: Set<BlockPos> = emptySet(),
        val lastTarget: BlockPos? = null,
        val hasChangedMask: Boolean = false,
        val hasLeaped: Boolean = false,
        val hasAlerted: Boolean = false
    )
}
//#endif