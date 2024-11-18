package noammaddons.features.dungeons

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.entity.item.EntityArmorStand
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
import noammaddons.utils.ActionUtils.isActive
import noammaddons.utils.ActionUtils.leap
import noammaddons.utils.ActionUtils.rodSwap
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.rotateSmoothlyTo
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import java.util.regex.Pattern.matches
import kotlin.math.ceil


@DelicateCoroutinesApi
object AutoI4: Feature() {
    private val rightClickKey = mc.gameSettings.keyBindUseItem
    private val doneCoords = mutableSetOf<BlockPos>()
    private val classLeapPriority = listOf(Mage, Tank, Healer, Archer) // correct me if wrong
    private var alerted = true
    private var Leaped = false
    private var shouldUseMask = true
    private var tickTimer = - 1
    private val devBlocks = listOf(
        BlockPos(64, 126, 50), BlockPos(66, 126, 50), BlockPos(68, 126, 50),
        BlockPos(64, 128, 50), BlockPos(66, 128, 50), BlockPos(68, 128, 50),
        BlockPos(64, 130, 50), BlockPos(66, 130, 50), BlockPos(68, 130, 50)
    )

    private fun isOnDev(): Boolean = Player.posY == 127.0 && Player.posX in 62.0 .. 65.0 && Player.posZ in 34.0 .. 37.0

    private fun getXdiff(x: Int): Vec3 {
        val xdiff = when (x) {
            68 -> - 0.4
            66 -> if (Math.random() < 0.5) 1.4 else - 0.4
            64 -> 1.4
            else -> 0.5
        }

        return Vec3(xdiff, 1.1, .0)
    }

    @Synchronized
    @SubscribeEvent
    fun onTick(event: BlockChangeEvent) {
        if (! config.autoI4) return
        if (Player?.heldItem?.getItemId() != 261) return
        if (! isOnDev()) return
        if (event.pos !in devBlocks) return

        scope.launch {
            when (event.state.block) {
                emerald_block -> {
                    if (! rightClickKey.isKeyDown) holdClick(true)
                    val lookVec = Vec3(event.pos).add(getXdiff(event.pos.x))

                    rotateSmoothlyTo(lookVec, 200)
                    doneCoords.add(event.pos)
                }

                stained_hardened_clay -> {
                    if (! rightClickKey.isKeyDown) holdClick(true)
                    val remainingTargets = devBlocks.filter { it !in doneCoords }
                    val nextTarget = if (remainingTargets.isNotEmpty()) remainingTargets.random() else return@launch
                    val lookVec = Vec3(nextTarget).add(getXdiff(nextTarget.x))

                    rotateSmoothlyTo(lookVec, 200)
                }
            }
        }
    }

    @SubscribeEvent
    @Synchronized
    fun renderDevBlocks(event: RenderWorld) {
        if (! config.autoI4) return
        if (! isOnDev()) return

        devBlocks.filter { it !in doneCoords }.forEach {
            drawBlockBox(
                it,
                when (getBlockAt(it)) {
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
        if (! isOnDev()) return
        val msg = event.component.unformattedText.removeFormatting()

        when {
            msg == "[BOSS] Storm: I should have known that I stood no chance." -> tickTimer = 0
            matches("(.+) completed a device! \\(...\\)", msg) && tickTimer >= 0 -> alert()
        }
    }

    @Synchronized
    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! config.autoI4) return
        if (! isOnDev()) return
        if (tickTimer == - 1) return
        tickTimer ++

        val baseOffset = 104 + 40

        val rodSwapTime = 40 + baseOffset
        val maskChangeTime = 100 + baseOffset
        val leapTimePrimary = 107 + baseOffset
        val leapTimeFallback = 163 + baseOffset

        when {
            tickTimer == rodSwapTime -> rodSwap()
            tickTimer == maskChangeTime && shouldUseMask && ! isActive -> changeMask()
            tickTimer == leapTimePrimary && ! isActive -> saveLeap()
            tickTimer == leapTimeFallback -> saveLeap()
        }

        // time off message - 104 | 5.2 sec
        // atStart - 40 ticks | 2 sec

        // Spirit  - 50 ticks | 2.5 sec
        // Phoenix - 80 ticks | 4 sec
        // bonzo   - 60 ticks | 3 sec

        // dont ask why, if its works dont touch it - @Noamm9
        mc.theWorld.loadedEntityList.asSequence().filterIsInstance<EntityArmorStand>().filter {
            val isCorrectName = it.name.removeFormatting().equalsOneOf("device", "active")
            val isCorrectPosition = "${ceil(it.posX - 1)}, ${ceil(it.posY + 1)}, ${ceil(it.posZ)}" == "63.0, 127.0, 35.0"
            isCorrectName && isCorrectPosition
        }.run { if (count() != 2) return }

        tickTimer = - 1
        alert()

        if (rightClickKey.isKeyDown) holdClick(false)
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
        leapTeammates.filter { ! it.isDead }.find {
            it.clazz in classLeapPriority
        }?.run {
            Leaped = true
            leap(this)
        }
    }

    fun alert() {
        if (alerted) return

        alerted = true
        tickTimer = - 1
        saveLeap()
        sendPartyMessage("${CHAT_PREFIX.removeFormatting()} I4 Done!")
        Alert(FULL_PREFIX, "&a&lI4 Done!")
        if (rightClickKey.isKeyDown || rightClickKey.isPressed) holdClick(false)
    }
}