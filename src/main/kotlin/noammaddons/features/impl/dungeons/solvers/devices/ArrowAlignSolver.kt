package noammaddons.features.impl.dungeons.solvers.devices

import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.init.Items
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.LocationUtils
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.MathUtils.floor
import noammaddons.utils.RenderUtils.drawString
import noammaddons.utils.ThreadUtils.loop
import java.awt.Color


// Big Thanks to Odin for Having their code available.
// https://github.com/odtheking/Odin/blob/1ad9222159bee1dcc08ed12fc6c7ab2d9a8a627f/src/main/kotlin/me/odinmain/features/impl/floor7/p3/ArrowAlign.kt
// @Modified
object ArrowAlignSolver: Feature() {
    private val frameGridCorner = Vec3(- 2.0, 120.0, 75.0)
    private val recentClickTimestamps = mutableMapOf<Int, Long>()
    val clicksRemaining = mutableMapOf<Int, Int>()
    var currentFrameRotations: List<Int>? = null
    private var targetSolution: List<Int>? = null

    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks")

    init {
        // Run on a timer Thread to reduce the impact on preformance
        loop(50) {
            if (! enabled) return@loop
            if (LocationUtils.F7Phase != 3) return@loop
            if (distance3D(mc.thePlayer.positionVector, Vec3(0.0, 120.0, 77.0)) > 14) {
                currentFrameRotations = null
                targetSolution = null
                clicksRemaining.clear()
                return@loop
            }

            currentFrameRotations = getFrames()

            possibleSolutions.forEach { arr ->
                for (i in arr.indices) {
                    val currentRotation = currentFrameRotations?.get(i) ?: - 1
                    if ((arr[i] == - 1 || currentRotation == - 1) && arr[i] != currentRotation) return@forEach
                }

                targetSolution = arr

                for (i in arr.indices) {
                    val currentRotation = currentFrameRotations?.get(i) ?: return@forEach
                    val clicksNeeded = calculateClicksNeeded(currentRotation, arr[i])
                    if (clicksNeeded == 0) continue
                    clicksRemaining[i] = clicksNeeded
                }
            }
        }
    }


    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (LocationUtils.F7Phase != 3) return
        val packet = event.packet as? C02PacketUseEntity ?: return
        if (packet.action != C02PacketUseEntity.Action.INTERACT) return
        val entity = packet.getEntityFromWorld(mc.theWorld) as? EntityItemFrame ?: return
        if (entity.displayedItem?.item != Items.arrow) return

        val entityPosition = entity.positionVector.floor()
        if (entityPosition.xCoord != frameGridCorner.xCoord) return

        val frameY = entityPosition.yCoord - frameGridCorner.yCoord
        val frameZ = (entityPosition.zCoord - frameGridCorner.zCoord) * 5
        val frameIndex = (frameY + frameZ).toInt()

        if (frameIndex !in 0 .. 24) return
        if (currentFrameRotations?.get(frameIndex) == - 1) return
        if (blockWrongClicks && frameIndex !in clicksRemaining.keys && mc.thePlayer.isSneaking) {
            event.isCanceled = true
            return
        }

        recentClickTimestamps[frameIndex] = System.currentTimeMillis()
        currentFrameRotations = currentFrameRotations?.toMutableList()?.apply {
            this[frameIndex] = (this[frameIndex] + 1) % 8
        }

        val currentRotation = currentFrameRotations?.get(frameIndex) ?: return
        val targetRotation = targetSolution?.get(frameIndex) ?: return

        if (calculateClicksNeeded(currentRotation, targetRotation) != 0) return
        clicksRemaining.remove(frameIndex)
    }


    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (LocationUtils.F7Phase != 3) return
        clicksRemaining.takeIf { it.isNotEmpty() }?.toMap()?.forEach { (index, clickNeeded) ->
            if (clickNeeded == 0) return@forEach

            val color = when {
                clickNeeded < 3 -> Color(0, 255, 0)
                clickNeeded < 5 -> Color(255, 125, 0)
                else -> Color(255, 0, 0)
            }

            drawString(
                "$clickNeeded",
                getFramePositionFromIndex(index).add(0.15, 0.6, 0.5),
                color, 1.3f, phase = true
            )
        }
    }


    private fun getFrames(): List<Int> {
        val itemFrames = mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityItemFrame>()
            ?.filter { it.displayedItem?.item == Items.arrow }
            ?.takeIf { it.isNotEmpty() }
            ?: return List(25) { - 1 }

        val frameMap = itemFrames.associate { it.positionVector.floor().toString() to it.rotation }

        return (0 .. 24).map { index ->
            val recentClick = recentClickTimestamps[index]?.let { System.currentTimeMillis() - it < 1000 } == true
            val currentRotation = currentFrameRotations?.get(index)

            when {
                recentClick && currentRotation != null -> currentRotation
                else -> frameMap[getFramePositionFromIndex(index).toString()] ?: - 1
            }
        }
    }

    private fun getFramePositionFromIndex(index: Int): Vec3 = frameGridCorner.add(0, index % 5, index / 5)
    private fun calculateClicksNeeded(currentRotation: Int, targetRotation: Int): Int = (8 - currentRotation + targetRotation) % 8

    // todo offload to json?
    private val possibleSolutions = listOf(
        listOf(7, 7, - 1, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, 3, - 1, - 1, - 1, - 1, 1, - 1, - 1, - 1, 7, 1),
        listOf(- 1, - 1, 7, 7, 5, - 1, 7, 1, - 1, 5, - 1, - 1, - 1, - 1, - 1, - 1, 7, 5, - 1, 1, - 1, - 1, 7, 7, 1),
        listOf(7, 7, - 1, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, - 1, 7, 5, - 1, - 1, - 1, - 1, 5, - 1, - 1, - 1, 3, 3),
        listOf(5, 3, 3, 3, - 1, 5, - 1, - 1, - 1, - 1, 7, 7, - 1, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, - 1),
        listOf(5, 3, 3, 3, 3, 5, - 1, - 1, - 1, 1, 7, 7, - 1, - 1, 1, - 1, - 1, - 1, - 1, 1, - 1, 7, 7, 7, 1),
        listOf(7, 7, 7, 7, - 1, 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, 3, - 1, - 1, - 1, - 1, 1, - 1, 7, 7, 7, 1),
        listOf(- 1, - 1, - 1, - 1, - 1, 1, - 1, 1, - 1, 1, 1, - 1, 1, - 1, 1, 1, - 1, 1, - 1, 1, - 1, - 1, - 1, - 1, - 1),
        listOf(- 1, - 1, - 1, - 1, - 1, 1, 3, 3, 3, 3, - 1, - 1, - 1, - 1, 1, 7, 7, 7, 7, 1, - 1, - 1, - 1, - 1, - 1),
        listOf(- 1, - 1, - 1, - 1, - 1, - 1, 1, - 1, 1, - 1, 7, 1, 7, 1, 3, 1, - 1, 1, - 1, 1, - 1, - 1, - 1, - 1, - 1)
    )
}
