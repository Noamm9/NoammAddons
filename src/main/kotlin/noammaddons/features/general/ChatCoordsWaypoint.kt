package noammaddons.features.general

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.ThreadUtils.setTimeout
import java.awt.Color


// regex from Doc's Chat Waypoints
// https://github.com/DocilElm/Doc/blob/main/features/misc/ChatWaypoint.js
object ChatCoordsWaypoint: Feature() {
    data class waypoint(val name: String, val loc: Vec3, val time: Long)

    private val regex = Regex("^(Co-op|Party)?(?: > )?(?:\\[\\d+] .? ?)?(?:\\[[\\w+]+] )?(\\w{1,16}): x: (.{1,4}), y: (.{1,4}), z: (.{1,4})")
    private val waypoints = mutableListOf<waypoint>()

    @SubscribeEvent
    fun coordsToWaypoint(event: Chat) {
        if (! config.ChatCoordsWayPoint) return
        val match = regex.find(event.component.noFormatText) ?: return

        val (_, name, x, y, z) = match.destructured
        ChatUtils.modMessage("&b$name&r &aSent a waypoint at &b$x, $y, $z")

        val waypoint = waypoint(name, Vec3(x.toDouble(), y.toDouble(), z.toDouble()), System.currentTimeMillis())
        setTimeout(20_000L) { waypoints.remove(waypoint) }
        waypoints.add(waypoint)
        event.isCanceled = true
    }

    @SubscribeEvent
    fun renderWaypoints(event: RenderWorld) {
        if (waypoints.isEmpty()) return

        waypoints.withIndex().forEach { (_, waypoint) ->
            val distance = MathUtils.distance3D(
                mc.thePlayer.renderVec,
                waypoint.loc
            ).toFloat()

            var scale = (distance * 0.2f).coerceIn(2f, 10f)
            if (distance < 10) scale = 2f

            if (distance <= 10) {
                waypoints.remove(waypoint)
                return
            }

            RenderUtils.drawBlockBox(
                BlockPos(waypoint.loc),
                config.ChatCoordsWayPointColor,
                outline = true,
                fill = true,
                phase = true
            )

            RenderUtils.drawString(
                waypoint.name,
                waypoint.loc.add(y = 1 + distance * 0.01f),
                Color(255, 0, 255),
                scale, phase = true
            )

            RenderUtils.drawTracer(
                waypoint.loc.add(0.5, 0.5, 0.5),
                config.ChatCoordsWayPointColor
            )
        }
    }
}
