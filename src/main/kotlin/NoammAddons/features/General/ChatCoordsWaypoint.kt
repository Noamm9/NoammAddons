package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.MathUtils
import NoammAddons.utils.RenderUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object ChatCoordsWaypoint {
    data class waypoint(val name: String, val x: Int, val y: Int, val z: Int, val time: Long)
    private val regex = Regex("^(Co-op|Party)?(?: > )?(?:\\[\\d+\\] .? ?)?(?:\\[[\\w\\+]+\\] )?(\\w{1,16})\\: x\\: (.{1,4}), y\\: (.{1,4}), z\\: (.{1,4})")
    private val selfDistractionTimeMs = 60_000
    private val waypointArray = mutableListOf<waypoint>()

    @SubscribeEvent
    fun coordsToWaypoint(event: ClientChatReceivedEvent) {
        if (!config.ChatCoordsWayPoint) return
        if (event.type.toInt() !in 0..1) return
        val match = regex.find(event.message.unformattedText.removeFormatting()) ?: return

        val (type, name, x, y, z) = match.destructured
        val time = System.currentTimeMillis()
        ChatUtils.modMessage("&b $type $name&r &aSent a waypoint at &b$x, $y, $z")

        waypointArray.add(waypoint(name, x.toInt(), y.toInt(), z.toInt(), time))
        event.isCanceled = true
    }

    @SubscribeEvent
    fun renderWaypoints(event: RenderWorldLastEvent) {
        if (waypointArray.isEmpty()) return
        waypointArray.removeIf { it.time + selfDistractionTimeMs <= System.currentTimeMillis() }

        waypointArray.forEach {
            val distance = MathUtils.distanceIn3DWorld(mc.thePlayer.positionVector, Vec3(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()))
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
                true, true, true
            )

            RenderUtils.drawString(
                it.name,
                Vec3(
                    it.x.toDouble(),
                    it.y + 1 + distance * 0.01f,
                    it.z.toDouble(),
                ),
                Color(255, 0, 255),
                scale, true, true
            )

            RenderUtils.drawTracer(Vec3(it.x + 0.5, it.y + 0.5, it.z + 0.5), config.ChatCoordsWayPointColor)
        }
    }
}
