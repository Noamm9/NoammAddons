package com.github.noamm9.features.impl.dungeon.solvers.devices

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import kotlin.math.abs

object I4Helper: Feature(name = "I4 Helper") {
    private val targetColor by ColorSetting("Target Color", Color.GREEN.withAlpha(127)).withDescription("Color of the target position.")
    private val doneColor by ColorSetting("Complate Color", Color.RED).withDescription("Color of a complate position.")

    private val showPrediction by ToggleSetting("Show Prediction", true).withDescription("Highlights the next block to shoot at")
    private val predictionColor by ColorSetting("Prediction Color", Color.YELLOW).withDescription("Color of the prediction")

    val DEVICE_DONE_REGEX = Regex("^(\\w{3,16}) completed a device! \\(\\d/\\d\\)$")

    val devBlocks = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )

    val doneCoords = mutableSetOf<BlockPos>()
    var target: BlockPos? = null
    var prediction: BlockPos? = null
    var alerted = false

    override fun init() {
        register<BlockChangeEvent> {
            if (LocationUtils.P3Section != 4) return@register
            if (event.pos !in devBlocks) return@register

            if (event.oldBlock == Blocks.EMERALD_BLOCK && event.newBlock == Blocks.BLUE_TERRACOTTA) doneCoords.add(event.pos)
            else if (event.newBlock != Blocks.EMERALD_BLOCK) return@register

            target = event.pos

            if (! showPrediction.value) return@register
            prediction = getPredictionTarget(event.pos, doneCoords)
        }

        register<RenderWorldEvent> {
            if (LocationUtils.P3Section != 4) return@register
            if (! isOnDev()) return@register reset()
            if (target == prediction && target != null) target?.let { Render3D.renderBlock(event.ctx, it, targetColor.value) }
            else {
                target?.let { Render3D.renderBlock(event.ctx, it, targetColor.value) }
                prediction?.let { Render3D.renderBlock(event.ctx, it, predictionColor.value) }
            }
            doneCoords.forEach { Render3D.renderBlock(event.ctx, it, doneColor.value) }
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
            if (mc.level?.getEntity(packet.id)?.name?.string == "Active") onComplete()
        }

        register<WorldChangeEvent> {
            reset()
            alerted = false
        }
    }

    private fun reset() {
        doneCoords.clear()
        target = null
        prediction = null
    }

    private fun onComplete() {
        if (alerted) return
        alerted = true
        val remaining = devBlocks.size - doneCoords.size
        ChatUtils.showTitle("&aCompleted Device!", if (remaining < 9) "&ePredicted: $remaining/9" else "")
    }

    fun getPredictionTarget(lastHitPos: BlockPos, doneCoords: Collection<BlockPos>): BlockPos? {
        return devBlocks.shuffled().find { potentialTarget ->
            val isNotDone = ! doneCoords.contains(potentialTarget)
            val isCorrectBlockType = WorldUtils.getBlockAt(potentialTarget) == Blocks.BLUE_TERRACOTTA
            val isNonAdjacentInSameColumn = potentialTarget.x == lastHitPos.x && potentialTarget.distSqr(lastHitPos) > 4.0
            isNotDone && isCorrectBlockType && ! isNonAdjacentInSameColumn
        }
    }

    fun isOnDev(): Boolean {
        val playerPos = mc.player?.position() ?: return false
        return abs(playerPos.y - 127.0) < 0.5 && playerPos.x in 62.0 .. 65.0 && playerPos.z in 34.0 .. 37.0
    }
}