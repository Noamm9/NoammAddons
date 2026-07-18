package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.BeamRenderer
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import java.awt.Color

object CustomMageBeam : Feature("Renders a fully custom, animated beam whenever a mage casts their beam ability.") {

    private val performanceMode by ToggleSetting("Performance Mode", false).withDescription("Skips the custom shape/animation and just draws a straight line following the real particles.").section("General")

    private val shapeIndex by DropdownSetting("Beam Shape", 2, listOf("Straight", "Cylinder", "Spiral", "Double Helix", "Wave", "Ribbon", "Lightning")).withDescription("The path the beam follows.").section("Shape").hideIf { performanceMode.value }
    private val width by SliderSetting("Width", 0.05, 0.05, 1.2, 0.01).withDescription("Beam thickness.").hideIf { performanceMode.value }
    private val segments by SliderSetting("Segment Count", 28, 6, 64, 1).withDescription("Longitudinal resolution.").hideIf { performanceMode.value }
    private val smoothness by SliderSetting("Smoothness", 3, 3, 24, 1).withDescription("Radial resolution.").hideIf { performanceMode.value }

    private val colorModeIndex by DropdownSetting("Color Mode", 0, listOf("Static", "Gradient", "Rainbow", "Chroma")).withDescription("How the beam is colored along its length and over time.").section("Colors").hideIf { performanceMode.value }
    private val primaryColor by ColorSetting("Primary Color", Color(255, 110, 0), false).withDescription("Base color. Also the line color in Performance Mode.").hideIf { !performanceMode.value && colorModeIndex.value.equalsOneOf(2, 3) }
    private val secondaryColor by ColorSetting("Secondary Color", Color(200, 60, 255), false).withDescription("Gradient target color, only used by Gradient.").hideIf { performanceMode.value || colorModeIndex.value != 1 }

    private val opacityPct by SliderSetting("Opacity", 80, 0, 100, 1, "%").withDescription("Overall beam transparency.").section("Animation").hideIf { performanceMode.value }
    private val glow by ToggleSetting("Glow", true).withDescription("Adds a soft, wider layer to fake bloom.").hideIf { performanceMode.value }
    private val pulse by ToggleSetting("Pulse Width", false).withDescription("Makes the beam breathe in and out.").hideIf { performanceMode.value }
    private val endpointFade by ToggleSetting("Endpoint Fade", true).withDescription("Fades the start/tip instead of a hard cut.").hideIf { performanceMode.value }
    private val trail by ToggleSetting("Beam Trail", true).withDescription("Renders fading afterimages behind the beam.").hideIf { performanceMode.value }

    private val lineWidth by SliderSetting("Line Width", 2, 1, 6, 1, "px").withDescription("Thickness of the line.").section("Performance").hideIf { !performanceMode.value }
    private val duration by SliderSetting("Duration", 40, 5, 100, 1, " ticks").withDescription("How long a beam lingers after it stops receiving new particles.")
    private val minPoints by SliderSetting("Min Points", 3, 2, 10, 1).withDescription("Minimum particles required before a beam is rendered.")
    private val hideSheep by ToggleSetting("Hide Sheep", true).withDescription("Prevents dungeon Sheep from spawning, so a launched sheep's beam doesn't clutter your view.").section("Trigger")

    private class Beam(val points: MutableList<Vec3>, var lastUpdateTick: Long, val startTick: Long, playerPos: Vec3) {
        lateinit var min: Vec3
        lateinit var max: Vec3

        init { updateEndpoints(playerPos) }

        fun updateEndpoints(playerPos: Vec3) {
            min = points[0]; max = points[0]
            var minSqr = min.distanceToSqr(playerPos)
            var maxSqr = minSqr
            for (p in points) {
                val d = p.distanceToSqr(playerPos)
                if (d < minSqr) { minSqr = d; min = p }
                if (d > maxSqr) { maxSqr = d; max = p }
            }
        }

        fun isContinuation(point: Vec3): Boolean {
            if (points.size < 2) return true
            val last = points.last()
            return last.subtract(points[0]).normalize().dot(point.subtract(last).normalize()) > 0.99
        }
    }

    private val beams = mutableListOf<Beam>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (!LocationUtils.inDungeon) return@register

            when (val packet = event.packet) {
                is ClientboundLevelParticlesPacket -> {
                    if (packet.particle.type != ParticleTypes.FIREWORK) return@register
                    event.isCanceled = true

                    val point = Vec3(packet.x, packet.y, packet.z)
                    val playerPos = mc.player?.position() ?: return@register
                    val recent = beams.lastOrNull()
                    val tick = DungeonListener.currentTime

                    if (recent != null && tick - recent.lastUpdateTick <= 1 && recent.isContinuation(point)) {
                        recent.points.add(point)
                        recent.lastUpdateTick = tick
                        recent.updateEndpoints(playerPos)
                    } else {
                        if (isNearSheep(point)) return@register
                        val newBeam = Beam(mutableListOf(point), tick, tick, playerPos)
                        beams.add(newBeam)
                        ThreadUtils.scheduledTaskServer(duration.value) { beams.remove(newBeam) }
                    }
                }
                is ClientboundAddEntityPacket -> if (hideSheep.value && packet.type == EntityType.SHEEP && mc.player!!.position().distanceToSqr(packet.x, packet.y, packet.z) <= 9) event.isCanceled = true
            }
        }

        register<RenderWorldEvent> {
            for (beam in beams) {
                if (beam.points.size < minPoints.value || beam.min == beam.max) continue
                if (performanceMode.value) Render3D.renderLine(event.ctx, beam.min, beam.max, primaryColor.value, lineWidth.value.toFloat(), false)
                else renderCustomBeam(event.ctx, beam)
            }
        }
    }

    private fun renderCustomBeam(ctx: RenderContext, beam: Beam) {
        val direction = beam.max.subtract(beam.min).normalize()
        val length = beam.min.distanceTo(beam.max).toFloat()
        val (grow, fade) = envelope(beam)
        val animTime = (DungeonListener.currentTime - beam.startTick) / 20f
        BeamRenderer.render(ctx, beam.min, direction, buildStyle(length), animTime, grow, fade, beam.hashCode())
    }

    private fun envelope(beam: Beam): Pair<Float, Float> {
        val now = DungeonListener.currentTime
        val grow = ((now - beam.startTick) / 3f).coerceIn(0f, 1f)
        val fade = 1f - ((now - beam.lastUpdateTick).toFloat() / duration.value).coerceIn(0f, 1f)
        return grow to fade
    }

    private fun buildStyle(length: Float) = BeamRenderer.BeamStyle(
        shape = BeamRenderer.BeamShape.entries[shapeIndex.value.coerceIn(0, BeamRenderer.BeamShape.entries.lastIndex)],
        colorMode = BeamRenderer.ColorMode.entries[colorModeIndex.value.coerceIn(0, BeamRenderer.ColorMode.entries.lastIndex)],
        primary = primaryColor.value, secondary = secondaryColor.value,
        width = width.value.toFloat(), opacity = (opacityPct.value / 100f).coerceIn(0f, 1f),
        length = length, segments = segments.value.coerceIn(2, 128), smoothness = smoothness.value.coerceIn(3, BeamRenderer.MAX_SIDES),
        glow = glow.value, throughWalls = false, endpointFade = endpointFade.value, pulse = pulse.value, trail = trail.value
    )

    private fun isNearSheep(point: Vec3): Boolean {
        val level = mc.level ?: return false
        val radiusSqr = SHEEP_EXCLUSION_RADIUS * SHEEP_EXCLUSION_RADIUS
        return level.entitiesForRendering().any { it.type == EntityType.SHEEP && it.position().distanceToSqr(point) <= radiusSqr }
    }

    private const val SHEEP_EXCLUSION_RADIUS = 1.5
}
