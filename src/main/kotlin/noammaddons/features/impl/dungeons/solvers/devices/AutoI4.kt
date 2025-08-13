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
import noammaddons.utils.ActionUtils.changeMask
import noammaddons.utils.ActionUtils.currentAction
import noammaddons.utils.ActionUtils.leap
import noammaddons.utils.ActionUtils.rodSwap
import noammaddons.utils.ActionUtils.rotateSmoothlyTo
import noammaddons.utils.ActionUtils.rotationJob
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ServerPlayer
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object AutoI4: Feature("Fully Automated I4.") {
    private val rotationTime = SliderSetting("Rotation Time", 0, 250, 10, 170)
    private val predictSetting = ToggleSetting("Predictions", true)
    private val rodSetting = ToggleSetting("Auto Rod", true)
    private val maskSetting = ToggleSetting("Auto Mask", true)
    private val leapSetting = ToggleSetting("Auto Leap", true)
    private val LeapPriorities = listOf(Mage, Tank, Healer, Archer)
    private val preferredLeapClass = DropdownSetting("Leap Priority", LeapPriorities.map { it.toString() })

    override fun init() = addSettings(
        rotationTime, predictSetting, rodSetting, maskSetting, leapSetting, preferredLeapClass
    )

    private data class PhaseState(
        val tickTimer: Int = - 1,
        val lastEmeraldTick: Int = - 1,
        val doneCoords: MutableSet<BlockPos> = mutableSetOf(),
        val hasLeaped: Boolean = false,
        val hasChangedMask: Boolean = false,
        val hasAlerted: Boolean = false
    )

    private var state = PhaseState()
    private var shootingAt: BlockPos? = null
    private var isPredicting = false
    private var deviceShootingJob: Job? = null

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (! isInDevicePhase() || ServerPlayer.player.getHeldItem()?.item !is ItemBow) return
        if (event.pos !in devBlocks || event.oldBlock != stained_hardened_clay || event.block != emerald_block) return

        state = state.copy(lastEmeraldTick = state.tickTimer)
        deviceShootingJob?.cancel()
        rotationJob?.cancel()

        deviceShootingJob = scope.launch {
            while (currentAction() == "Rod Swap") delay(1)

            val shotVec = getTargetVector(event.pos)
            state.doneCoords.add(event.pos)
            shootingAt = event.pos
            isPredicting = false
            rotateAndShoot(shotVec)
            while (rotationJob?.isActive == true) delay(1)

            if (! predictSetting.value) return@launch
            if (rotationTime.value > 0) delay(rotationTime.value.toLong())

            getPredictionTarget(event.pos)?.let { nextTarget ->
                val predictionVec = getTargetVector(nextTarget)
                shootingAt = nextTarget
                isPredicting = true
                rotateAndShoot(predictionVec)
                while (rotationJob?.isActive == true) delay(1)
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! isInDevicePhase()) return reset()

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
            message == STORM_DEATH_MESSAGE -> {
                state = state.copy(tickTimer = 0)
                setTimeout(30_000L) { state = state.copy(tickTimer = - 1) }
            }

            message.matches(DEVICE_DONE_REGEX) && state.tickTimer >= 0 && isInDevicePhase() && leapSetting.value -> {
                triggerPhaseCompletion("Completed Device")
            }
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (state.tickTimer == - 1) return
        state = state.copy(tickTimer = state.tickTimer + 1)
        if (! isInDevicePhase()) return

        val action = ! currentAction().equalsOneOf("Change Mask", "Leap")

        when {
            state.tickTimer.equalsOneOf(251, 307) && leapSetting.value && action -> performLeap()
            state.tickTimer == 244 && maskSetting.value && ! state.hasChangedMask && action -> changeMask()
            state.tickTimer == 174 && maskSetting.value && action -> {
                state = state.copy(hasChangedMask = true)
                changeMask()
            }

            state.tickTimer == 174 && rodSetting.value && action -> rodSwap()
        }

        val ticksSinceLastEmerald = state.tickTimer - state.lastEmeraldTick
        val hasEmeraldBlock = devBlocks.none { getBlockAt(it) == emerald_block }

        if (state.tickTimer > 150 && ticksSinceLastEmerald > 30 && hasEmeraldBlock && state.doneCoords.size > 4)
            triggerPhaseCompletion("Device stalled")
    }

    private fun reset() {
        if (state.doneCoords.size > 1) holdClick(false)
        deviceShootingJob?.cancel()
        state = PhaseState().copy(tickTimer = state.tickTimer)
        shootingAt = null
        isPredicting = false
    }

    private fun performLeap() {
        if (state.hasLeaped) return
        state = state.copy(hasLeaped = true)

        val aliveTeammates = leapTeammates.filterNot { it.isDead }
        val preferredClass = LeapPriorities[preferredLeapClass.value]

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
        scope.launch {
            state = state.copy(hasAlerted = true)
            deviceShootingJob?.cancel()
            state = state.copy(tickTimer = - 1)

            if (currentAction() != "Leap") performLeap()

            modMessage("Predicted ${9 - state.doneCoords.size}/9")
            modMessage("Trigger: $reason")
        }
    }

    private fun isInDevicePhase(): Boolean {
        val (x, y, z) = ServerPlayer.player.getVec()?.destructured() ?: return false
        return y == 127.0 && x in 62.0 .. 65.0 && z in 34.0 .. 37.0 && enabled
    }

    private fun getTargetVector(pos: BlockPos): Vec3 {
        val i = devBlocks.indexOf(pos)
        val col = i % 3
        val row = i / 3

        val isBlockToTheRightDone = (col < 2) && devBlocks[i + 1] in state.doneCoords
        val targetX = if (col == 0 || isBlockToTheRightDone) 67.5 else 65.5

        return Vec3(targetX, 131.3 - 2 * row, 50.0)
    }

    private fun rotateAndShoot(vec: Vec3) {
        if (rotationTime.value == 0) return
        rotateSmoothlyTo(vec, rotationTime.value.toLong()) {
            Thread.sleep(20)
            sendRightClickAirPacket()
        }
    }

    private fun getPredictionTarget(lastHitPos: BlockPos): BlockPos? {
        return devBlocks.shuffled().find { potentialTarget ->
            val isNotDone = potentialTarget !in state.doneCoords
            val isCorrectBlockType = getBlockAt(potentialTarget) == stained_hardened_clay
            val isNotAdjacent = potentialTarget.x == lastHitPos.x && potentialTarget.distanceSq(lastHitPos) > 4

            isNotDone && isCorrectBlockType && ! isNotAdjacent
        }
    }

    suspend fun testI4() {
        //    ChatUtils.sendFakeChatMessage(STORM_DEATH_MESSAGE)
        //    delay(STORM_PHASE_START_DELAY)

        for (pos in devBlocks.shuffled()) {
            delay(600)
            postAndCatch(BlockChangeEvent(pos, emerald_block, stained_hardened_clay))
            ghostBlock(pos, emerald_block.defaultState)
            delay(400)
            postAndCatch(BlockChangeEvent(pos, stained_hardened_clay, emerald_block))
            ghostBlock(pos, stained_hardened_clay.defaultState)
        }
    }


    private const val STORM_DEATH_MESSAGE = "[BOSS] Storm: I should have known that I stood no chance."
    private val DEVICE_DONE_REGEX = Regex("^(\\w{3,16}) completed a device! \\(\\d/\\d\\)\$")

    private val devBlocks = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )
}


