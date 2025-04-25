package noammaddons.features.impl.general

import gg.essential.elementa.utils.withAlpha
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.favoriteColor
import java.util.concurrent.CopyOnWriteArrayList


// regex from Doc's Chat Waypoints
// https://github.com/DocilElm/Doc/blob/main/features/misc/ChatWaypoint.js
object ChatCoordsWaypoint: Feature("Shows A waypoint when someone sends coords in chat (only works with patcher coords)") {
    data class Waypoint(val name: String, val loc: Vec3)

    private val drawName = ToggleSetting("Sender Name", true)
    private val drawTracer = ToggleSetting("Tracer", true)
    private val drawBox = ToggleSetting("Box")
    private val removeTimeout = SliderSetting("Remove Timeout (in seconds)", 10f, 120f, 30.0)
    private val removeOnReach = ToggleSetting("Remove On Reach", true)

    private val nameColor = ColorSetting("Sender Name Color", favoriteColor, false)
    private val tracerColor = ColorSetting("Tracer Color", favoriteColor, false)
    private val boxColor = ColorSetting("Box Color", favoriteColor.withAlpha(0.3f))

    override fun init() {
        addSettings(
            drawName, drawTracer, drawBox,
            removeTimeout, removeOnReach,
            SeperatorSetting("Colors"),
            nameColor, tracerColor,
            boxColor
        )
    }

    private val waypoints = CopyOnWriteArrayList<Waypoint>()


    init {
        onChat(Regex("^(Co-op|Party)?(?: > )?(?:\\[\\d+] .? ?)?(?:\\[[\\w+]+] )?(\\w{1,16}): x: (.{1,4}), y: (.{1,4}), z: (.{1,4})")) {
            val (_, name, x, y, z) = it.destructured
            val waypoint = Waypoint(name, Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
            setTimeout(removeTimeout.value.toLong()) { waypoints.remove(waypoint) } // lazy ass way ik XD
            waypoints.add(waypoint)
        }
    }

    @SubscribeEvent
    fun renderWaypoints(event: RenderWorld) {
        if (waypoints.isEmpty()) return

        waypoints.forEach { waypoint ->
            val distance = distance3D(mc.thePlayer.renderVec, waypoint.loc).toFloat()
            var scale = (distance * 0.2f).coerceIn(2f, 10f)
            if (distance < 10) scale = 2f

            if (drawBox.value) RenderUtils.drawBlockBox(
                BlockPos(waypoint.loc),
                boxColor.value,
                outline = true,
                fill = true,
                phase = true
            )

            if (drawName.value) RenderUtils.drawString(
                waypoint.name,
                waypoint.loc.add(y = 1 + distance * 0.01f),
                nameColor.value,
                scale, phase = true
            )

            if (drawTracer.value) RenderUtils.drawTracer(
                waypoint.loc.add(0.5, 0.5, 0.5),
                tracerColor.value
            )

            if (distance <= 10 && removeOnReach.value) waypoints.remove(waypoint)
        }
    }
}
