package noammaddons.features.general

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderUtils
import java.awt.Color


// regex from Doc's Chat Waypoints
// https://github.com/DocilElm/Doc/blob/main/features/misc/ChatWaypoint.js
object ChatCoordsWaypoint: Feature() {
    private data class waypoint(val name: String, val x: Int?, val y: Int?, val z: Int?, val time: Long)

    private val regex = Regex("^(Co-op|Party)?(?: > )?(?:\\[\\d+] .? ?)?(?:\\[[\\w+]+] )?(\\w{1,16}): x: (.{1,4}), y: (.{1,4}), z: (.{1,4})")
    private val selfDistractionTimeMs = 60_000
    private val waypointArray = mutableListOf<waypoint>()

    @SubscribeEvent
    fun coordsToWaypoint(event: Chat) {
        if (! config.ChatCoordsWayPoint) return
        val match = regex.find(event.component.unformattedText.removeFormatting()) ?: return

        val (type, name, x, y, z) = match.destructured

        val time = System.currentTimeMillis()
        ChatUtils.modMessage("&b $type $name&r &aSent a waypoint at &b$x, $y, $z")

        waypointArray.add(waypoint(name, x.toIntOrNull(), y.toIntOrNull(), z.toIntOrNull(), time))
        event.isCanceled = true
    }

    @SubscribeEvent
    fun renderWaypoints(event: RenderWorld) {
        if (waypointArray.isEmpty()) return
        waypointArray.removeIf { it.time + selfDistractionTimeMs <= System.currentTimeMillis() }

        waypointArray.forEach {
            val distance = MathUtils.distanceIn3DWorld(
                Player !!.getRenderVec(),
                Vec3(
                    it.x?.toDouble() ?: return@forEach,
                    it.y?.toDouble() ?: return@forEach,
                    it.z?.toDouble() ?: return@forEach
                )
            )
            var scale = (distance * 0.2).toFloat()
            if (distance < 10) scale = 2f
            scale = scale.coerceIn(0.5f, 10f) // Cap the scale for distant waypoints

            if (distance <= 5) {
                waypointArray.remove(it)
                return
            }

            RenderUtils.drawBlockBox(
                BlockPos(it.x, it.y, it.z),
                config.ChatCoordsWayPointColor,
                outline = true,
                fill = true,
                phase = true
            )

            RenderUtils.drawString(
                it.name,
                it.x,
                it.y + 1 + distance * 0.01f,
                it.z,
                Color(255, 0, 255),
                scale, shadow = true,
                phase = true
            )

            RenderUtils.drawTracer(
                Vec3(
                    it.x + 0.5,
                    it.y + 0.5,
                    it.z + 0.5
                ),
                config.ChatCoordsWayPointColor
            )
        }
    }
}
