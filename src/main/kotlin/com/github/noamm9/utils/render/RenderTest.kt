package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.NoammDebugFlagEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.awt.Color

object RenderTest {
    private var pos: Vec3? = null

    fun init() {
        EventBus.register<RenderWorldEvent> {
            if ("render" !in NoammAddons.debugFlags) return@register
            val player = NoammAddons.mc.player ?: return@register
            if (pos == null) pos = player.position()

            val base = pos ?: return@register
            val ctx = event.ctx

            Render3D.renderString(
                text = "§6§lRender3D Showcase\n§fTesting all utilities",
                pos = base.add(0.0, 3.5, 0.0),
                color = Color.WHITE,
                scale = 1.2f,
                phase = false
            )

            Render3D.renderBlock(
                ctx = ctx,
                pos = BlockPos.containing(base),
                outlineColor = Color.WHITE,
                fillColor = Color(255, 255, 255, 60), // Semi-transparent white
                outline = true,
                fill = true,
                phase = false,
                lineWidth = 3.0
            )

            Render3D.renderCircle(
                ctx = ctx,
                center = base.add(0.0, 0.05, 0.0),
                radius = 2.0,
                color = Color.RED,
                thickness = 3,
                phase = false
            )

            val boxPos = base.add(4.0, 0.0, 0.0)
            Render3D.renderBox(
                ctx = ctx,
                x = boxPos.x,
                y = boxPos.y,
                z = boxPos.z,
                width = 1.0,
                height = 2.0,
                outlineColor = Color.GREEN,
                fillColor = Color(0, 255, 0, 45),
                outline = true,
                fill = true,
                phase = false,
                lineWidth = 2.5
            )

            Render3D.renderString("renderBox()", boxPos.add(0.0, 2.2, 0.0), Color.GREEN, 0.8f)

            val boundsPos = base.add(- 4.0, 0.0, 0.0)
            Render3D.renderBoxBounds(
                ctx = ctx,
                minX = boundsPos.x - 0.5,
                minY = boundsPos.y,
                minZ = boundsPos.z - 0.5,
                maxX = boundsPos.x + 0.5,
                maxY = boundsPos.y + 1.5,
                maxZ = boundsPos.z + 0.5,
                outlineColor = Color.BLUE,
                fillColor = Color(0, 0, 255, 45),
                outline = true,
                fill = true,
                phase = true,
                lineWidth = 2.0
            )

            Render3D.renderString("renderBoxBounds() \n§e(Phase/Through Walls)", boundsPos.add(0.0, 1.8, 0.0), Color.BLUE, 0.8f)

            val lineStart = base.add(0.0, 1.0, - 4.0)
            val lineEnd = base.add(0.0, 2.5, - 4.0)
            Render3D.renderLine(
                ctx = ctx,
                start = lineStart,
                finish = lineEnd,
                color = Color.MAGENTA,
                thickness = 4,
                phase = false
            )
            Render3D.renderString("renderLine()", lineEnd.add(0.0, 0.3, 0.0), Color.MAGENTA, 0.8f)

            val tracerTarget = base.add(0.0, 0.5, 4.0)
            Render3D.renderTracer(
                ctx = ctx,
                point = tracerTarget,
                color = Color.YELLOW,
                thickness = 2.0
            )
            Render3D.renderString("renderTracer()", tracerTarget.add(0.0, 1.0, 0.0), Color.YELLOW, 0.8f)
        }

        EventBus.register<NoammDebugFlagEvent.Remove> { if (event.flag == "render") pos = null }
    }
}