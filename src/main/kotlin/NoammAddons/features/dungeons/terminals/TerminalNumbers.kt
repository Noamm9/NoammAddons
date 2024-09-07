package NoammAddons.features.dungeons.terminals

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils.P3Section
import NoammAddons.utils.LocationUtils.inBoss
import NoammAddons.utils.MathUtils
import NoammAddons.utils.RenderUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color


object TerminalNumbers {
    private val Terminals = listOf(
        // Section 1
        listOf(
            listOf(listOf(111, 113, 73), listOf(110, 113, 73)),
            listOf(listOf(111, 119, 79), listOf(110, 119, 79)),
            listOf(listOf(89, 112, 92), listOf(90, 112, 92)),
            listOf(listOf(89, 122, 101), listOf(90, 122, 101))
        ),
        // Section 2
        listOf(
            listOf(listOf(68, 109, 121), listOf(68, 109, 122)),
            listOf(listOf(59, 120, 122), listOf(59, 119, 123)),
            listOf(listOf(47, 109, 121), listOf(47, 109, 122)),
            listOf(listOf(40, 124, 122), listOf(40, 124, 123)),
            listOf(listOf(39, 108, 143), listOf(39, 108, 142))
        ),
        // Section 3
        listOf(
            listOf(listOf(-3, 109, 112), listOf(-2, 109, 112)),
            listOf(listOf(-3, 119, 93), listOf(-2, 119, 93)),
            listOf(listOf(19, 123, 93), listOf(18, 123, 93)),
            listOf(listOf(-3, 109, 77), listOf(-2, 109, 77))
        ),
        // Section 4
        listOf(
            listOf(listOf(41, 109, 29), listOf(41, 109, 30)),
            listOf(listOf(44, 121, 29), listOf(44, 121, 30)),
            listOf(listOf(67, 109, 29), listOf(67, 109, 30)),
            listOf(listOf(72, 115, 48), listOf(72, 114, 47))
        )
    )

    private fun render(value: List<List<Int>>, index: Int) {
        var (x, y, z) = value[0].map { it + 0.5 }
        y -= 0.5
        val (tX, tY, tZ) = value[1].map { it + 0.5 }

        val distance = MathUtils.distanceIn3DWorld(
            Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
            Vec3(tX, tY, tZ)
        )

        RenderUtils.drawBlockBox(
            BlockPos(x, y, z),
            Color(0, 114, 255, 85),
            outline = true,
            fill = true,
            phase = true
        )

        RenderUtils.drawString(
            "${index + 1}",
            Vec3(tX, tY, tZ),
            Color(255, 0, 255),
            if (distance < 10) 3f else (distance * 0.3).toFloat(),
            shadow = true, phase = true
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!inBoss || P3Section == null || !config.TerminalNumbers) return

        try {
            Terminals[P3Section!! - 1].forEachIndexed { index, value -> render(value, index) }
        }
        catch (_: Exception) {}
    }
}


