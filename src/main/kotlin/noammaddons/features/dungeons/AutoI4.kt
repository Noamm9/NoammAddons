package noammaddons.features.dungeons

import kotlinx.coroutines.*
import net.minecraft.init.Blocks.emerald_block
import net.minecraft.init.Blocks.stained_hardened_clay
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.events.RegisterEvents.postAndCatch
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.utils.ActionUtils.changeMask
import noammaddons.utils.ActionUtils.currentAction
import noammaddons.utils.ActionUtils.leap
import noammaddons.utils.ActionUtils.rodSwap
import noammaddons.utils.ActionUtils.rotateSmoothlyTo
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import java.util.regex.Pattern.matches


object AutoI4: Feature() {
    private val doneCoords = mutableSetOf<BlockPos>()
    private val classLeapPriority = listOf(Mage, Tank, Healer, Archer) // correct me if wrong
    private val devBlocks = listOf(
        BlockPos(64, 126, 50), BlockPos(66, 126, 50), BlockPos(68, 126, 50),
        BlockPos(64, 128, 50), BlockPos(66, 128, 50), BlockPos(68, 128, 50),
        BlockPos(64, 130, 50), BlockPos(66, 130, 50), BlockPos(68, 130, 50)
    )
    private val rotationTime get() = config.AutoI4RotatinTime.toLong()
    private var tickTimer = - 1
    private var shootingAt: BlockPos? = null
    private var isPredicting = false
    private var alerted = true
    private var Leaped = false

    private fun isOnDev() = Player.posY == 127.0 && Player.posX in 62.0 .. 65.0 && Player.posZ in 34.0 .. 37.0

    private fun getXdiff(x: Int): Vec3 {
        val xdiff = when (Player.heldItem.SkyblockID) {
            "TERMINATOR" -> when (x) {
                68 -> - 0.4
                66 -> if (Math.random() < 0.5) 1.4 else - 0.4
                64 -> 1.4
                else -> 0.5
            }

            else -> 0.5
        }

        return Vec3(xdiff, 1.1, .0)
    }

    private fun reset() {
        if (doneCoords.size > 1) holdClick(false)
        doneCoords.clear()
        isPredicting = false
        alerted = false
        Leaped = false
        shootingAt = null
    }

    @SubscribeEvent
    fun onTick(e: Tick) {
        if (! config.autoI4) return
        if (Player?.heldItem?.getItemId() != 261) return
        if (! isOnDev()) return

        for (pos in doneCoords.filter { getBlockAt(it) != stained_hardened_clay }) {
            if (shootingAt != pos) doneCoords.remove(pos)
        }

        val pos = devBlocks.find { getBlockAt(it) == emerald_block && it !in doneCoords } ?: return

        scope.launch {
            val shotVec = Vec3(pos).add(getXdiff(pos.x))

            shootingAt = pos
            isPredicting = false
            doneCoords.add(pos)

            rotateSmoothlyTo(shotVec, rotationTime)
            delay(rotationTime)

            while (getBlockAt(pos) == emerald_block) delay(1)

            val shuffledTargets = devBlocks.shuffled().filterNot { it in doneCoords }.shuffled()
            val nextTarget = if (shuffledTargets.isNotEmpty()) shuffledTargets.random()
            else {
                shootingAt = null
                isPredicting = false
                return@launch
            }

            val predictionVec = Vec3(nextTarget).add(getXdiff(nextTarget.x))
            shootingAt = nextTarget
            isPredicting = true
            holdClick(true)
            rotateSmoothlyTo(predictionVec, rotationTime)
        }
    }

    @SubscribeEvent
    fun renderDevBlocks(event: RenderWorld) {
        if (! config.autoI4) return
        if (! isOnDev()) return reset()

        devBlocks.filter { it !in doneCoords }.forEach {

            drawBlockBox(
                it,
                if (it == shootingAt) when (isPredicting) {
                    false -> Color(0, 255, 0)
                    true -> Color(255, 255, 0)
                }
                else when (getBlockAt(it)) {
                    emerald_block -> Color(255, 0, 80)
                    else -> Color(0, 136, 255)
                },
                outline = false, fill = true, phase = true
            )

        }
    }


    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.autoI4) return
        val msg = event.component.unformattedText.removeFormatting()

        when {
            msg == "[BOSS] Storm: I should have known that I stood no chance." -> {
                tickTimer = 0
                setTimeout(30_000) { tickTimer = - 1 }
                modMessage("AutoI4: tickTimer started")
            }

            matches("(.+) completed a device! \\(...\\)", msg) && tickTimer >= 0 && isOnDev() -> alert()
        }
    }


    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! config.autoI4) return
        if (tickTimer == - 1) return
        tickTimer ++
        if (! isOnDev()) return

        val actionCheck = currentAction().equalsOneOf("Change Mask", "Leap")
        if (Player?.heldItem?.getItemId() == 261 && ! actionCheck) holdClick(true)

        when {
            tickTimer == 174 -> rodSwap()
            tickTimer == 244 && ! actionCheck -> changeMask()
            tickTimer == 251 && ! actionCheck -> saveLeap()
            tickTimer == 307 && ! actionCheck -> saveLeap()
        }
    }

    suspend fun testi4() {
        //   ChatUtils.sendFakeChatMessage("[BOSS] Storm: I should have known that I stood no chance.")
        // delay(5200)

        for (pos in devBlocks.shuffled()) {
            delay(600)

            postAndCatch(BlockChangeEvent(pos, emerald_block.defaultState))
            ghostBlock(pos, emerald_block.defaultState)
            delay(400)
            postAndCatch(BlockChangeEvent(pos, stained_hardened_clay.defaultState))
            ghostBlock(pos, stained_hardened_clay.defaultState)
        }
    }

    private fun saveLeap() {
        if (Leaped) return
        for (clazz in classLeapPriority) {
            leapTeammates.filterNot { it.isDead }.forEach {
                if (it.clazz == clazz) {
                    scope.launch {
                        while (isActive) {
                            holdClick(false)
                            if (Leaped) cancel()
                        }
                    }

                    leap(it)
                    Leaped = true
                    tickTimer = - 1
                    return
                }
            }
        }
    }

    private fun alert() {
        if (alerted) return

        alerted = true
        tickTimer = - 1
        saveLeap()
        sendPartyMessage("${CHAT_PREFIX.removeFormatting()} I4 Done!")
        Alert(FULL_PREFIX, "&a&lI4 Done!")
    }
}