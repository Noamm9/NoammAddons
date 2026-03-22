package com.github.noamm9.features.impl.floor7.devices

//#if CHEAT

import com.github.noamm9.event.EventBus
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
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

    private data class PhaseState(
        val tickTimer: Int = - 1,
        val lastEmeraldTick: Int = - 1,
        val doneCoords: Set<BlockPos> = emptySet(),
        val hasLeaped: Boolean = false,
        val hasChangedMask: Boolean = false,
        val hasAlerted: Boolean = false,
        val lastTarget: BlockPos? = null
    )

    private data class Action(val priority: Int, val block: suspend () -> Unit)

    private var state = PhaseState()
    private val deviceActionQueue = ArrayDeque<Action>()
    private var deviceJob: Job? = null

    override fun init() {
        register<TickEvent.Server> {
            if (state.tickTimer == - 1) return@register
            state = state.copy(tickTimer = state.tickTimer + 1)
            if (! I4Helper.isOnDev()) return@register

            when (state.tickTimer) {
                307 if leapSetting.value -> queue(4, ::saveLeap)
                244 if maskSetting.value && ! state.hasChangedMask -> queue(3, PlayerUtils::changeMaskAction)
                174 if rodSetting.value -> queue(2, PlayerUtils::rodSwap)
                174 if maskSetting.value -> {
                    state = state.copy(hasChangedMask = true)
                    queue(3, PlayerUtils::changeMaskAction)
                }
            }

            /*
            val ticksSinceLastEmerald = if (state.lastEmeraldTick < 0) Int.MAX_VALUE else (state.tickTimer - state.lastEmeraldTick)
            val shouldCheckForStall = rotationTime.value > 0 && state.tickTimer > 150 && ticksSinceLastEmerald > 30 && state.doneCoords.size > 4
            if (shouldCheckForStall) {
                val hasEmeraldBlock = devBlocks.any { WorldUtils.getBlockAt(it) == Blocks.EMERALD_BLOCK }
                if (hasEmeraldBlock) state = state.copy(lastEmeraldTick = state.tickTimer)
                else onComplete("Device stalled")
            }*/
        }

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

        register<BlockChangeEvent> {
            if (! LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 7) return@register
            if (! I4Helper.isOnDev()) {
                state = PhaseState(tickTimer = state.tickTimer)
                return@register
            }
            if (! mc.player !!.mainHandItem.`is`(Items.BOW)) return@register
            if (event.pos !in I4Helper.devBlocks) return@register

            state = state.copy(lastEmeraldTick = state.tickTimer)
            if (event.oldBlock == Blocks.EMERALD_BLOCK && event.newBlock == Blocks.BLUE_TERRACOTTA) {
                state = state.copy(doneCoords = state.doneCoords + event.pos)
                return@register
            }

            if (event.newBlock != Blocks.EMERALD_BLOCK) return@register

            if (rotationTime.value > 0) queue(1) {
                shootAtBlock(event.pos)
                state = state.copy(lastTarget = event.pos)

                if (predictSetting.value) {
                    getPredictionTarget(event.pos, state.doneCoords)?.let { nextTarget ->
                        val preCheckEmerald = findAnyEmeraldExcluding(event.pos, nextTarget)
                        if (preCheckEmerald != null) return@queue
                        shootAtBlock(nextTarget)
                    }
                }
            }
        }
    }

    private fun findAnyEmeraldExcluding(vararg exclude: BlockPos?): BlockPos? {
        return I4Helper.devBlocks.find { pos ->
            if (pos in exclude) return@find false
            if (pos in state.doneCoords) return@find false
            WorldUtils.getBlockAt(pos) == Blocks.EMERALD_BLOCK
        }
    }

    private suspend fun shootAtBlock(pos: BlockPos) {
        val player = mc.player ?: return
        val (yaw, pitch) = calcYawPitch(getTargetVector(pos))
        val block = suspend {
            delay(50)
            PlayerUtils.rightClick()
        }

        findAnyEmeraldExcluding(state.lastTarget, pos)?.let { newer ->
            state = state.copy(lastEmeraldTick = state.tickTimer, doneCoords = state.doneCoords + newer)
            return shootAtBlock(newer)
        }

        val currentYaw = MathUtils.normalizeYaw(player.yRot)
        val currentPitch = MathUtils.normalizePitch(player.xRot)
        val targetYaw = MathUtils.normalizeYaw(yaw)
        val targetPitch = MathUtils.normalizePitch(pitch)
        val tolerance = 1f

        if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) return block()

        val startTime = System.currentTimeMillis()
        while (true) {
            val newerDuring = findAnyEmeraldExcluding(state.lastTarget, pos)
            if (newerDuring != null) {
                state = state.copy(lastEmeraldTick = state.tickTimer, doneCoords = state.doneCoords + newerDuring)
                return shootAtBlock(newerDuring)
            }

            val elapsed = System.currentTimeMillis() - startTime
            val progress = if (rotationTime.value <= 0) 1.0 else min(elapsed.toDouble() / rotationTime.value, 1.0)

            if (progress >= 1) {
                rotate(targetYaw, targetPitch)
                block()
                break
            }

            val easedProgress = easeInOutCubic(progress).toFloat()
            val newYaw = interpolateYaw(currentYaw, targetYaw, easedProgress)
            val newPitch = lerp(currentPitch, targetPitch, easedProgress).toFloat()
            rotate(newYaw, newPitch)
        }
    }

    private fun getTargetVector(pos: BlockPos): Vec3 {
        val i = I4Helper.devBlocks.indexOf(pos).coerceAtLeast(0)
        val col = i % 3
        val row = i / 3

        val isBlockToTheRightDone = (col < 2) && (i + 1 < I4Helper.devBlocks.size) && (I4Helper.devBlocks[i + 1] in state.doneCoords)
        val targetX = if (col == 0 || isBlockToTheRightDone) 67.5 else 65.5

        val targetY = 131.3 - 2.0 * row
        return Vec3(targetX, targetY, 50.0)
    }

    private fun queue(priority: Int, block: suspend () -> Unit) {
        deviceActionQueue.add(Action(priority, block))
        deviceActionQueue.sortByDescending { it.priority }
        if (deviceJob?.isActive != true) {
            processDeviceQueue()
        }
    }

    private fun processDeviceQueue() {
        deviceJob = scope.launch {
            while (deviceActionQueue.isNotEmpty()) {
                runCatching {
                    deviceActionQueue.removeFirst().block()
                }
            }
        }
    }

    private suspend fun saveLeap() {
        if (! leapSetting.value) return
        if (state.hasLeaped) return
        deviceActionQueue.clear()
        state = state.copy(hasLeaped = true, tickTimer = - 1)
        val aliveTeammates = DungeonListener.dungeonTeammatesNoSelf.filterNot { it.isDead }

        val preferredClass = leapPriorities[preferredLeapClass.value]
        val target = aliveTeammates.find { it.clazz == preferredClass }
            ?: leapPriorities.firstNotNullOfOrNull { priority ->
                aliveTeammates.find { it.clazz == priority }
            } ?: return

        leapAction(target)
        while (I4Helper.isOnDev()) delay(50)
    }

    private fun onComplete() {
        if (state.hasAlerted) return
        state = state.copy(hasAlerted = true, tickTimer = - 1)
        queue(4, ::saveLeap)
    }

    fun simDev() {
        EventBus.post(ChatMessageEvent(Component.literal(STORM_DEATH_MESSAGE)))

        scope.launch {
            for (pos in I4Helper.devBlocks.shuffled()) {
                delay(800)
                ClientboundBlockUpdatePacket(pos, Blocks.EMERALD_BLOCK.defaultBlockState()).handle(mc.connection)
                delay(600)
                ClientboundBlockUpdatePacket(pos, Blocks.BLUE_TERRACOTTA.defaultBlockState()).handle(mc.connection)
            }
        }
    }
}
//#endif