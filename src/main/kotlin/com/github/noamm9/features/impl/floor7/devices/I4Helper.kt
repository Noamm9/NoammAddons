package com.github.noamm9.features.impl.floor7.devices

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IServerboundChatCommandPacket
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.toPos
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.*
import kotlin.math.abs

object I4Helper: Feature(name = "I4 Helper") {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")
    private val showPrediction by ToggleSetting("Show Prediction", true).withDescription("Highlights the next block to shoot at.")
    private val highlightPosition by DropdownSetting("Highlight Position", 1, listOf("Blocks", "Panes")).withDescription("Whether to draw the highlight onto the stained glass or on the terracotta blocks.")

    private val targetColor by ColorSetting("Target Color", Color.GREEN.withAlpha(127)).withDescription("Color of the target position.").section("Colors")
    private val doneColor by ColorSetting("Complete Color", Color.RED).withDescription("Color of a complete position.")
    private val predictionColor by ColorSetting("Prediction Color", Color.YELLOW).withDescription("Color of the prediction.").showIf { showPrediction.value }

    val DEVICE_DONE_REGEX = Regex("^(\\w{3,16}) completed a device! \\(\\d/\\d\\)$")
    val devBlocks = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )

    private val doneCoords = ConcurrentHashMap.newKeySet<BlockPos>()
    @Volatile private var targetPos: Vec3? = null
    @Volatile private var predictionPos: Vec3? = null
    @Volatile private var alerted = false
    @Volatile var prediction: BlockPos? = null

    private val lastPredictions = ConcurrentHashMap<BlockPos, Int>()
    private const val MAX_PREDICTION_ATTEMPTS = 2

    override fun init() {
        register<BlockChangeEvent> {
            if (LocationUtils.P3Section != 4) return@register
            if (event.pos !in devBlocks) return@register

            if (event.oldBlock == Blocks.EMERALD_BLOCK && event.newBlock == Blocks.BLUE_TERRACOTTA) doneCoords.add(event.pos)
            else if (event.newBlock != Blocks.EMERALD_BLOCK) return@register

            targetPos = getRenderPos(event.pos)

            if (!showPrediction.value) {
                prediction = null
                predictionPos = null
                return@register
            }

            prediction = getPredictionTarget(event.pos, doneCoords)
            predictionPos = prediction?.let(::getRenderPos)
        }

        register<RenderWorldEvent> {
            if (LocationUtils.P3Section != 4) return@register
            if (! isOnDev()) return@register reset()

            fun highlight(pos: BlockPos, color: Color) {
                event.ctx.renderBlock(
                    pos, color,
                    mode.value.equalsOneOf(0, 2),
                    mode.value.equalsOneOf(1, 2),
                    phase = phase.value,
                    lineWidth.value
                )
            }

            highlight(targetPos?.toPos() ?: return@register, targetColor.value)
            doneCoords.forEach { highlight(it, doneColor.value) }
            predictionPos
                ?.takeIf { showPrediction.value && it != targetPos }
                ?.let { highlight(it.toPos(), predictionColor.value) }
        }

        register<ChatMessageEvent> {
            if (LocationUtils.P3Section != 4) return@register
            val msg = event.unformattedText.takeIf { it.contains("completed a device!") } ?: return@register
            if (DEVICE_DONE_REGEX.find(msg)?.groupValues?.get(1) != mc.user.name) return@register
            onComplete()
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (LocationUtils.P3Section != 4) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            if (level.getEntity(packet.id)?.name?.string == "Active") onComplete()
        }

        register<WorldChangeEvent> {
            reset()
            alerted = false
        }

        register<PacketEvent.Sent> {
            val packet = event.packet as? IServerboundChatCommandPacket ?: return@register
            if (packet.command != "start p3") return@register
            reset()
            alerted = false
        }
    }

    private fun reset() {
        doneCoords.clear()
        targetPos = null
        predictionPos = null
        prediction = null
        lastPredictions.clear()
    }

    private fun onComplete() {
        if (alerted) return
        alerted = true
        val remaining = devBlocks.size - doneCoords.size
        ChatUtils.showTitle("&aCompleted Device!", if (remaining < 9) "&ePredicted: $remaining/9" else "")
    }

    private fun getRenderPos(pos: BlockPos): Vec3 {
        return if (highlightPosition.value == 1) {
            getTargetVector(pos, doneCoords, addOffset = false)
        } else {
            Vec3.atCenterOf(pos)
        }
    }

    fun getTargetVector(pos: BlockPos, doneCoords: Collection<BlockPos>, addOffset: Boolean = true): Vec3 {
        val i = devBlocks.indexOf(pos).coerceAtLeast(0)
        val col = i % 3
        val row = i / 3

        val isLeftDone = (col < 2) && (devBlocks[i + 1] in doneCoords)
        val isRightDone = (col > 0) && (devBlocks[i - 1] in doneCoords)

        val targetX = when (col) {
            0 -> 67.5
            2 -> 65.5
            else -> when {
                isRightDone && ! isLeftDone -> 65.5
                isLeftDone && ! isRightDone -> 67.5
                else -> if (Math.random() < 0.5) 65.5 else 67.5
            }
        }

        val targetY = (if (addOffset) 131.0 else 130.0) - 2.0 * row
        return vec(targetX, targetY, 50)
    }

    fun getPredictionTarget(lastHitPos: BlockPos, doneCoords: Collection<BlockPos>): BlockPos? {
        val allValid = devBlocks.filter { it !in doneCoords && it != lastHitPos && WorldUtils.getBlockAt(it) == Blocks.BLUE_TERRACOTTA }.ifEmpty { return null }
        val candidates = allValid.filter { (lastPredictions[it] ?: 0) < MAX_PREDICTION_ATTEMPTS }.ifEmpty { allValid }

        val pairs = candidates.shuffled().groupBy { it.y }.flatMap { (_, blocks) ->
            val sorted = blocks.sortedBy { it.x }
            buildList {
                for (i in 0 until sorted.size - 1) {
                    if (sorted[i + 1].x - sorted[i].x == 2) {
                        add(sorted[i] to sorted[i + 1])
                    }
                }
            }
        }

        val chosen = if (pairs.isNotEmpty()) pairs.random().toList().random() else candidates.random()
        lastPredictions.merge(chosen, 1) { old, one -> old + one }
        return chosen
    }

    fun isOnDev() = abs(player.y - 127.0) < 0.5 && player.x in 62.0 .. 65.0 && player.z in 34.0 .. 37.0
}