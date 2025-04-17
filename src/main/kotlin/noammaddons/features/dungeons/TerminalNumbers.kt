package noammaddons.features.dungeons

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawString
import java.awt.Color


object TerminalNumbers: Feature() {
    private fun BlockPos.center(): Vec3 = Vec3(x + 0.5, y + 0.5, z + 0.5)

    // omg its soo ugly todo: maybe add devs and lavers
    private val terminals = listOf(
        // Section 1
        listOf(
            listOf(BlockPos(111, 113, 73), BlockPos(110, 113, 73)),
            listOf(BlockPos(111, 119, 79), BlockPos(110, 119, 79)),
            listOf(BlockPos(89, 112, 92), BlockPos(90, 112, 92)),
            listOf(BlockPos(89, 122, 101), BlockPos(90, 122, 101))
        ),
        // Section 2
        listOf(
            listOf(BlockPos(68, 109, 121), BlockPos(68, 109, 122)),
            listOf(BlockPos(59, 120, 122), BlockPos(59, 119, 123)),
            listOf(BlockPos(47, 109, 121), BlockPos(47, 109, 122)),
            listOf(BlockPos(39, 108, 143), BlockPos(39, 108, 142)),
            listOf(BlockPos(40, 124, 122), BlockPos(40, 124, 123))
        ),
        // Section 3
        listOf(
            listOf(BlockPos(- 3, 109, 112), BlockPos(- 2, 109, 112)),
            listOf(BlockPos(- 3, 119, 93), BlockPos(- 2, 119, 93)),
            listOf(BlockPos(19, 123, 93), BlockPos(18, 123, 93)),
            listOf(BlockPos(- 3, 109, 77), BlockPos(- 2, 109, 77))
        ),
        // Section 4
        listOf(
            listOf(BlockPos(41, 109, 29), BlockPos(41, 109, 30)),
            listOf(BlockPos(44, 121, 29), BlockPos(44, 121, 30)),
            listOf(BlockPos(67, 109, 29), BlockPos(67, 109, 30)),
            listOf(BlockPos(72, 115, 48), BlockPos(72, 114, 47))
        )
    )

    private fun renderTerminal(positions: List<BlockPos>, index: Int) {
        val boxPosition = positions[0]
        val textPosition = positions[1]

        val distance = distance3D(
            mc.thePlayer.renderVec,
            textPosition.center()
        ).toFloat()

        val scale = maxOf(distance * 0.18f, 2f)

        drawBlockBox(
            boxPosition,
            Color(0, 114, 255, 65),
            outline = true,
            fill = true,
            phase = true
        )

        drawString(
            "${index + 1}",
            textPosition.center(),
            Color(255, 0, 255),
            scale,
            phase = true
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.TerminalNumbers) return
        if (! inBoss) return
        if (P3Section == null) return

        val section = terminals.getOrNull(P3Section !! - 1) ?: return
        section.forEachIndexed { index, positions ->
            renderTerminal(positions, index)
        }
    }
}