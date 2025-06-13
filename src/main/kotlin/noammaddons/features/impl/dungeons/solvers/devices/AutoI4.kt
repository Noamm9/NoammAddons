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
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
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
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object AutoI4: Feature("Fully Automated I4.") {
    private val rotationTime = SliderSetting("Rotation time", 0, 250, 10, 170)
    private val predit = ToggleSetting("Predictions", true)
    private val rod = ToggleSetting("Auto Rod")
    private val mask = ToggleSetting("Auto Mask")
    private val leap = ToggleSetting("Auto Leap")

    override fun init() = addSettings(rotationTime, predit, rod, mask, leap)

    private val doneCoords = mutableSetOf<BlockPos>()
    private val classLeapPriority = listOf(Mage, Tank, Healer, Archer) // correct me if wrong
    private val devBlocks = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )
    private val devDoneRegex = Regex("^(\\w{3,16}) completed a device! \\(\\d/\\d\\)\$")
    private var tickTimer = - 1
    private var lastEmeraldTick = - 1
    private var shootingAt: BlockPos? = null
    private var isPredicting = false
    private var alerted = true
    private var Leaped = false
    private var changedMask = false
    private var i4Job: Job? = null

    private fun isOnDev() = mc.thePlayer.posY == 127.0 && mc.thePlayer.posX in 62.0 .. 65.0 && mc.thePlayer.posZ in 34.0 .. 37.0 && enabled

    private fun getTargetVec(pos: BlockPos): Vec3 {
        val i = devBlocks.indexOf(pos)
        val x = i % 3
        val y = i / 3

        return Vec3(
            if (x == 0 || (x == 1 && doneCoords.contains(devBlocks[x + 1 + y * 3]))) 67.5 else 65.5,
            131.3 - 2 * y, 50.0
        )
    }

    private fun rotateAndShot(vec: Vec3) {
        if (rotationTime.value.toLong() == 0L) return
        rotateSmoothlyTo(vec, rotationTime.value.toLong()) {
            Thread.sleep(20)
            sendRightClickAirPacket()
        }
    }


    private fun reset() {
        if (doneCoords.size > 1) holdClick(false)
        doneCoords.clear()
        isPredicting = false
        alerted = false
        Leaped = false
        shootingAt = null
        changedMask = false
        i4Job?.cancel()
    }

    @SubscribeEvent
    fun onBlock(event: BlockChangeEvent) {
        if (! isOnDev()) return
        if (mc.thePlayer?.heldItem?.item !is ItemBow) return
        if (event.pos !in devBlocks) return
        if (event.oldBlock != stained_hardened_clay || event.block != emerald_block) return
        lastEmeraldTick = tickTimer

        i4Job?.cancel()
        rotationJob?.cancel()

        i4Job = scope.launch {
            while (currentAction() == "Rod Swap") delay(1)
            val shotVec = getTargetVec(event.pos)
            doneCoords.add(event.pos)
            shootingAt = event.pos
            isPredicting = false
            rotateAndShot(shotVec)

            if (! predit.value) return@launch
            if (rotationTime.value.toLong() != 0L) delay(rotationTime.value.toLong())

            val nextTarget = devBlocks.shuffled().find {
                val notDone = it !in doneCoords
                val isStained = getBlockAt(it) == stained_hardened_clay
                val nextToLast = it.x == event.pos.x && it.distanceSq(event.pos) <= 4

                notDone && isStained && ! nextToLast
            } ?: return@launch
            val predictionVec = getTargetVec(nextTarget)

            shootingAt = nextTarget
            isPredicting = true
            rotateAndShot(predictionVec)
        }
    }

    @SubscribeEvent
    fun renderDevBlocks(event: RenderWorld) {
        if (! isOnDev()) return reset()

        devBlocks.forEach {
            if (it in doneCoords) {
                drawBlockBox(
                    it,
                    when (getBlockAt(it)) {
                        emerald_block -> Color.GREEN
                        else -> Color.RED
                    },
                    outline = false, fill = true, phase = true
                )
            }
            else {
                drawBlockBox(
                    it,
                    Color(0, 136, 255),
                    outline = false, fill = true, phase = true
                )
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText

        when {
            msg == "[BOSS] Storm: I should have known that I stood no chance." -> {
                setTimeout(30_000) { tickTimer = - 1 } // lazy ass way ik
                tickTimer = 0
            }

            msg.matches(devDoneRegex) && tickTimer >= 0 && isOnDev() && leap.value -> alert("Trigger by Chat Message")
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (tickTimer == - 1) return
        tickTimer ++
        if (! isOnDev()) return

        val actionCheck = currentAction().equalsOneOf("Change Mask", "Leap")

        when {
            tickTimer == 174 && rod.value -> rodSwap()
            tickTimer == 174 && mask.value -> {
                changedMask = true
                changeMask()
            }

            tickTimer == 244 && ! actionCheck && mask.value && ! changedMask -> changeMask()

            tickTimer == 251 && ! actionCheck && ! changedMask && leap.value -> saveLeap()
            tickTimer == 307 && ! actionCheck && leap.value -> saveLeap()
        }

        if (tickTimer > 150 && tickTimer - lastEmeraldTick > 30 && devBlocks.none { getBlockAt(it) == emerald_block } && doneCoords.size > 4) alert(
            "Trigger by lastEmeraldTick:\ntimer: $tickTimer, emeraldTicks: $lastEmeraldTick"
        )
    }

    suspend fun testi4() {
        //   ChatUtils.sendFakeChatMessage("[BOSS] Storm: I should have known that I stood no chance.")
        // delay(5200)

        for (pos in devBlocks.shuffled()) {
            delay(600)

            postAndCatch(BlockChangeEvent(pos, emerald_block, stained_hardened_clay))
            ghostBlock(pos, emerald_block.defaultState)
            delay(400)
            postAndCatch(BlockChangeEvent(pos, stained_hardened_clay, emerald_block))
            ghostBlock(pos, stained_hardened_clay.defaultState)
        }
    }

    private fun saveLeap() {
        if (Leaped) return
        Leaped = true
        for (clazz in classLeapPriority) {
            leapTeammates.filterNot { it.isDead }.forEach {
                if (it.clazz == clazz) {
                    leap(it)
                    tickTimer = - 1
                    return
                }
            }
        }
    }

    private fun alert(s: String) {
        if (alerted) return
        scope.launch {
            alerted = true
            i4Job?.cancel()
            tickTimer = - 1
            if (currentAction() != "Leap") saveLeap()
            val str = listOf("&a&lI4 Done!", "Predicted ${9 - doneCoords.size}/9", s)
            str.forEach(::modMessage)
        }
    }
}


