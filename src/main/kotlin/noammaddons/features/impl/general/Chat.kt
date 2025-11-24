package noammaddons.features.impl.general

import gg.essential.elementa.utils.withAlpha
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.favoriteColor
import noammaddons.utils.DataDownloader
import java.util.concurrent.CopyOnWriteArrayList


object Chat: Feature() {
    data class Waypoint(val name: String, val loc: Vec3)

    private val regexList = DataDownloader.loadJson<List<String>>("uselessMessages.json").map { Regex(it) }

    private val hideUseless = ToggleSetting("Hide Useless Messages", false)
    private val printSbEXP = ToggleSetting("Print SkyBlock XP", false)

    private val chatWaypoints = ToggleSetting("Chat Waypoints", true)
    private val drawName = ToggleSetting("Sender Name", true).addDependency(chatWaypoints)
    private val drawTracer = ToggleSetting("Tracer", true).addDependency(chatWaypoints)
    private val drawBox = ToggleSetting("Box").addDependency(chatWaypoints)
    private val removeTimeout = SliderSetting("Remove Timeout (in seconds)", 10f, 120f, 1, 30.0).addDependency(chatWaypoints)
    private val removeOnReach = ToggleSetting("Remove On Reach", true).addDependency(chatWaypoints)

    private val nameColor = ColorSetting("Sender Name Color", favoriteColor, false).addDependency(chatWaypoints).addDependency(drawName)
    private val tracerColor = ColorSetting("Tracer Color", favoriteColor, false).addDependency(chatWaypoints).addDependency(drawTracer)
    private val boxColor = ColorSetting("Box Color", favoriteColor.withAlpha(0.3f)).addDependency(chatWaypoints).addDependency(drawBox)

    override fun init() {
        addSettings(
            SeperatorSetting("Spam Protection"),
            hideUseless, printSbEXP,
            SeperatorSetting("Waypoint"),
            drawName, drawTracer, drawBox,
            removeTimeout, removeOnReach,
            SeperatorSetting("Colors"),
            nameColor, tracerColor,
            boxColor
        )
    }

    private val waypoints = CopyOnWriteArrayList<Waypoint>()
    private val skyBlockExpRegex = Regex(".*(§b\\+\\d+ SkyBlock XP §.\\([^()]+\\)§b \\(\\d+/\\d+\\)).*")
    private val coordRegex = Regex("^(Co-op|Party)?(?: > )?(?:\\[\\d+] .? ?)?(?:\\[[\\w+]+] )?(\\w{1,16}): x: (.{1,4}), y: (.{1,4}), z: (.{1,4})")
    private var lastMatch: String? = null

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! chatWaypoints.value) return
        val match = coordRegex.find(event.component.noFormatText) ?: return
        val (_, name, x, y, z) = match.destructured
        val waypoint = Waypoint(name, Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
        setTimeout(removeTimeout.value.toLong() * 1000) { waypoints.remove(waypoint) } // lazy ass way ik XD
        waypoints.add(waypoint)
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
                waypoint.loc.add(x = 0.5, y = 1 + distance * 0.01f, z = 0.5),
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

    private var lastMessageBlank: Boolean = false

    @SubscribeEvent
    fun onNewChatMessage(event: AddMessageToChatEvent) {
        if (! hideUseless.value) return
        val text = event.component.noFormatText
        if (text.isBlank()) {
            if (lastMessageBlank) {
                return event.setCanceled(true)
            } else {
                lastMessageBlank = true
                return
            }
        }
        if (regexList.any { it.matches(text) }) return event.setCanceled(true)
        lastMessageBlank = false
    }

    @SubscribeEvent
    fun onActionbar(event: Actionbar) {
        if (! inSkyblock) return
        if (! printSbEXP.value) return
        val match = skyBlockExpRegex.find(event.component.formattedText)?.groupValues?.get(1) ?: return
        if (match != lastMatch) {
            lastMatch = match
            modMessage(match)
        }
    }
}