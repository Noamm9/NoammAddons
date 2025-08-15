package noammaddons.features.impl.dungeons.solvers.devices

import kotlinx.coroutines.*
import net.minecraft.init.Blocks.*
import net.minecraft.item.ItemBow
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.events.EventDispatcher.postAndCatch
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.ActionUtils.easeInOutCubic
import noammaddons.utils.ActionUtils.leap
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.MathUtils.calcYawPitch
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.MathUtils.interpolateYaw
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.normalizePitch
import noammaddons.utils.MathUtils.normalizeYaw
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.rotate
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ThreadUtils.setTimeout
import java.awt.Color
import kotlin.math.abs
import kotlin.math.min


object AutoI4: Feature("Fully Automated I4.") {
    private val rotationTime by SliderSetting("Rotation Time", 0, 250, 10, 170)
    private val predictSetting by ToggleSetting("Predictions", true)
    private val rodSetting by ToggleSetting("Auto Rod", true)
    private val maskSetting by ToggleSetting("Auto Mask", true)
    private val leapSetting by ToggleSetting("Auto Leap", true)
    private val LeapPriorities = listOf(Mage, Tank, Healer, Archer)
    private val preferredLeapClass by DropdownSetting("Leap Priority", LeapPriorities.map(Any::toString))

    private const val STORM_DEATH_MESSAGE = "[BOSS] Storm: I should have known that I stood no chance."
    private val DEVICE_DONE_REGEX = Regex("^(\\w{3,16}) completed a device! \\(\\d/\\d\\)\$")

    private val devBlocks = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )

    private data class PhaseState(
        val tickTimer: Int = - 1,
        val lastEmeraldTick: Int = - 1,
        val doneCoords: Set<BlockPos> = emptySet(),
        val hasLeaped: Boolean = false,
        val hasChangedMask: Boolean = false,
        val hasAlerted: Boolean = false
    )

    private var state = PhaseState()
    private var shootingAt: BlockPos? = null
    private var isPredicting = false
    private val deviceShootingJobs = ArrayDeque<suspend () -> Unit>()
    private var deviceJob: Job? = null


    private fun queue(action: suspend () -> Unit) {
        deviceShootingJobs.add(action)
        if (deviceJob?.isActive != true) {
            processDeviceQueue()
        }
    }

    private fun processDeviceQueue() {
        deviceJob = scope.launch {
            while (deviceShootingJobs.isNotEmpty()) {
                runCatching {
                    deviceShootingJobs.removeFirst().invoke()
                }
            }
        }
    }

    private fun cancelAllJobs() {
        deviceShootingJobs.clear()
        deviceJob?.cancel()
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (! isOnDev() || mc.thePlayer.heldItem?.item !is ItemBow) return
        if (event.pos !in devBlocks || event.oldBlock != stained_hardened_clay || event.block != emerald_block) return

        state = state.copy(
            lastEmeraldTick = state.tickTimer,
            doneCoords = state.doneCoords + event.pos
        )

        queue {
            val shotVec = getTargetVector(event.pos)
            shootingAt = event.pos
            isPredicting = false
            rotateAndShoot(shotVec)

            if (! predictSetting) return@queue
            getPredictionTarget(event.pos)?.let { nextTarget ->
                val predictionVec = getTargetVector(nextTarget)
                shootingAt = nextTarget
                isPredicting = true
                rotateAndShoot(predictionVec)
            }

        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! isOnDev()) return reset()

        devBlocks.forEach { pos ->
            val color = when {
                pos in state.doneCoords -> if (getBlockAt(pos) == emerald_block) Color.GREEN else Color.RED
                else -> Color(0, 136, 255)
            }
            drawBlockBox(pos, color, outline = false, fill = true, phase = true)
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val message = event.component.noFormatText
        when {
            message == STORM_DEATH_MESSAGE -> state = PhaseState(tickTimer = 0).also {
                setTimeout(30_000L) { state = state.copy(tickTimer = - 1) }
            }

            message.matches(DEVICE_DONE_REGEX) && state.tickTimer >= 0 && isOnDev() && leapSetting ->
                triggerPhaseCompletion("Completed Device")
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (state.tickTimer == - 1) return
        state = state.copy(tickTimer = state.tickTimer + 1)
        if (! isOnDev()) return

        when {
            state.tickTimer == 307 && leapSetting -> {
                cancelAllJobs()
                queue(::performLeap)
            }

            state.tickTimer == 244 && maskSetting && ! state.hasChangedMask -> queue(ActionUtils::changeMaskAction)

            state.tickTimer == 174 && rodSetting -> queue(ActionUtils::rodSwapAction)

            state.tickTimer == 174 && maskSetting -> {
                state = state.copy(hasChangedMask = true)
                queue(ActionUtils::changeMaskAction)
            }
        }

        val ticksSinceLastEmerald = state.tickTimer - state.lastEmeraldTick
        val hasEmeraldBlock = devBlocks.any { getBlockAt(it) == emerald_block }
        val shouldCheckForStall = rotationTime > 0 && state.tickTimer > 150 &&
                ticksSinceLastEmerald > 30 && state.doneCoords.size > 4

        if (shouldCheckForStall) {
            if (hasEmeraldBlock) {
                state = state.copy(lastEmeraldTick = state.tickTimer)
            }
            else {
                triggerPhaseCompletion("Device stalled")
            }
        }
    }

    private fun performLeap() {
        if (! leapSetting || state.hasLeaped) return
        state = state.copy(hasLeaped = true)

        val aliveTeammates = leapTeammates.filterNot { it.isDead }
        val preferredClass = LeapPriorities[preferredLeapClass]

        val target = aliveTeammates.find { it.clazz == preferredClass }
            ?: LeapPriorities.firstNotNullOfOrNull { priority ->
                aliveTeammates.find { it.clazz == priority }
            }

        target?.let {
            leap(it)
            state = state.copy(tickTimer = - 1)
        }
    }

    private fun triggerPhaseCompletion(reason: String) {
        if (state.hasAlerted) return

        cancelAllJobs()
        val remainingBlocks = 9 - state.doneCoords.size

        queue {
            if (state.hasAlerted) return@queue
            state = state.copy(hasAlerted = true, tickTimer = - 1)
            performLeap()
            modMessage("Predicted $remainingBlocks/9")
            modMessage("Trigger: $reason")
        }
    }

    private suspend fun rotateAndShoot(targetVec: Vec3) {
        if (rotationTime == 0) return
        val block = suspend {
            delay(50)
            sendRightClickAirPacket()
        }


        val (targetYaw, targetPitch) = calcYawPitch(targetVec)
        val currentYaw = normalizeYaw(mc.thePlayer.rotationYaw)
        val currentPitch = normalizePitch(mc.thePlayer.rotationPitch)
        val tolerance = 1.0f

        if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) {
            return block()
        }

        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = if (rotationTime <= 0) 1.0 else min(elapsed.toDouble() / rotationTime, 1.0)

            if (progress >= 1.0) {
                block()
                break
            }

            val easedProgress = easeInOutCubic(progress).toFloat()
            val newYaw = interpolateYaw(currentYaw, targetYaw, easedProgress)
            val newPitch = lerp(currentPitch, targetPitch, easedProgress).toFloat()
            rotate(newYaw, newPitch)
        }
    }

    private fun reset() {
        if (state.doneCoords.size > 1) holdClick(false)
        cancelAllJobs()
        state = PhaseState().copy(tickTimer = state.tickTimer)
        shootingAt = null
        isPredicting = false
    }

    private fun isOnDev(): Boolean {
        val (x, y, z) = ServerPlayer.player.getVec()?.destructured() ?: return false
        return enabled && y == 127.0 && x in 62.0 .. 65.0 && z in 34.0 .. 37.0
    }

    private fun getTargetVector(pos: BlockPos): Vec3 {
        val i = devBlocks.indexOf(pos)
        val col = i % 3
        val row = i / 3

        val isBlockToTheRightDone = (col < 2) && devBlocks[i + 1] in state.doneCoords
        val targetX = if (col == 0 || isBlockToTheRightDone) 67.5 else 65.5

        return Vec3(targetX, 131.3 - 2 * row, 50.0)
    }

    private fun getPredictionTarget(lastHitPos: BlockPos): BlockPos? {
        return devBlocks.shuffled().find { potentialTarget ->
            val isNotDone = potentialTarget !in state.doneCoords
            val isCorrectBlockType = getBlockAt(potentialTarget) == stained_hardened_clay
            val isNonAdjacentInSameColumn = potentialTarget.x == lastHitPos.x && potentialTarget.distanceSq(lastHitPos) > 4
            isNotDone && isCorrectBlockType && ! isNonAdjacentInSameColumn
        }
    }

    suspend fun testI4() {
        ChatUtils.sendFakeChatMessage(STORM_DEATH_MESSAGE)
        delay(5000)

        for (pos in devBlocks.shuffled()) {
            delay(800)
            ghostBlock(pos, emerald_block.defaultState)
            postAndCatch(BlockChangeEvent(pos, stained_hardened_clay, emerald_block))
            delay(600)
            ghostBlock(pos, stained_hardened_clay.defaultState)
            postAndCatch(BlockChangeEvent(pos, emerald_block, stained_hardened_clay))
        }
    }
}