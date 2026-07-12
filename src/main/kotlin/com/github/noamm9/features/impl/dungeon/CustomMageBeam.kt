package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.BeamRenderer
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.sqrt

object CustomMageBeam: Feature("Renders a fully custom, animated beam whenever you swing your weapon.") {
    private val performanceMode by ToggleSetting("Performance Mode", false)
        .withDescription("Ultra-lightweight mode. Skips the custom shape/animation system entirely and just draws a single line following Hypixel's real beam particles, like Odin's MageBeam.")
        .section("General")

    private val shapeIndex by DropdownSetting(
        "Beam Shape", 2,
        listOf("Straight", "Cylinder", "Spiral", "Double Helix", "Wave", "Ribbon", "Lightning")
    ).withDescription("The geometric path the beam follows.").section("Shape")
        .hideIf { performanceMode.value }

    private val width by SliderSetting("Width", 0.25, 0.05, 1.2, 0.01).withDescription("The thickness of the beam.")
        .hideIf { performanceMode.value }
    private val beamLength by SliderSetting("Beam Length", 10, 3, 30, 1, " blocks").withDescription("Maximum distance the beam extends to, and how far out hits are checked for the sound.")
        .hideIf { performanceMode.value }
    private val segmentCountSetting by SliderSetting("Segment Count", 28, 6, 64, 1).withDescription("Longitudinal resolution of the beam curve.")
        .hideIf { performanceMode.value }
    private val smoothnessSetting by SliderSetting("Smoothness", 10, 3, 24, 1).withDescription("Radial resolution of the beam's cross-section.")
        .hideIf { performanceMode.value }

    private val colorModeIndex by DropdownSetting("Color Mode", 0, listOf("Static", "Gradient", "Rainbow", "Chroma"))
        .withDescription("How the beam is colored, both along its length and over time.").section("Colors")
        .hideIf { performanceMode.value }
    private val primaryColor by ColorSetting("Primary Color", Color(255, 110, 0), false)
        .withDescription("Base color of the beam. Also used as the line color in Performance Mode.")
        .hideIf { ! performanceMode.value && colorModeIndex.value.equalsOneOf(2, 3) }
    private val secondaryColor by ColorSetting("Secondary Color", Color(200, 60, 255), false)
        .withDescription("The color the beam gradients towards. Only used by Gradient.")
        .hideIf { performanceMode.value || colorModeIndex.value != 1 }

    private val opacityPct by SliderSetting("Opacity", 90, 0, 100, 1, "%").withDescription("Overall transparency of the beam.").section("Animation")
        .hideIf { performanceMode.value }
    private val animSpeed by SliderSetting("Animation Speed", 1.0, 0.2, 4.0, 0.1, "x").withDescription("Speed multiplier for scrolling, spinning and pulsing. Also controls how fast the impact sound's delay ramps up.")
    private val glow by ToggleSetting("Glow", true).withDescription("Adds a soft, wider layer around the beam to fake bloom.")
        .hideIf { performanceMode.value }
    private val pulse by ToggleSetting("Pulse Width", false).withDescription("Makes the beam breathe in and out as it travels.")
        .hideIf { performanceMode.value }
    private val endpointFade by ToggleSetting("Endpoint Fade", true).withDescription("Fades the beam's start and tip instead of cutting them off sharply.")
        .hideIf { performanceMode.value }
    private val trail by ToggleSetting("Beam Trail", true).withDescription("Renders fading afterimages behind the animated beam.")
        .hideIf { performanceMode.value }

    private val lineWidth by SliderSetting("Line Width", 2, 1, 6, 1, "px").withDescription("Thickness of the line, in pixels.").section("Performance")
        .hideIf { ! performanceMode.value }
    private val perfDuration by SliderSetting("Duration", 40, 5, 100, 1, " ticks").withDescription("How long the line stays visible after the beam stops receiving new particles.")
        .hideIf { ! performanceMode.value }
    private val perfMinPoints by SliderSetting("Min Points", 3, 2, 10, 1).withDescription("Minimum number of particles a beam needs before it's rendered. Filters out stray/unrelated particles.")
        .hideIf { ! performanceMode.value }

    private val throughWalls by ToggleSetting("Through Walls", false).withDescription("Renders the beam even when obstructed by blocks, and lets the sound trigger through them too.").section("Trigger")
    private val hideParticles by ToggleSetting("Hide Particles", true).withDescription("Cancels Hypixel's own Wither Impact particle beam so only the custom render shows.")
    private val hitLeniency by SliderSetting("Hit Leniency", 0.75, 0.0, 2.5, 0.05, " blocks")
        .withDescription("How far off your aim can be and still count as a hit for the sound. Raise this if the sound rarely plays; lower it if it plays on obvious misses.")

    private val soundEnabled by ToggleSetting("Play Sound", true).withDescription("Plays a sound when your swing actually connects with a mob (not on every left click, and never from other players' beams).").section("Sound")
    private val soundSettings = createSoundSettings("Cast Sound", SoundEvents.NOTE_BLOCK_HARP.value()) { soundEnabled.value }

    private data class BeamInstance(val origin: Vec3, val direction: Vec3, val startTime: Long, val seed: Int)

    private val activeBeams = mutableListOf<BeamInstance>()
    private var lastTriggerTime = 0L
    private var beamSeedCounter = 0

    private class PerfBeam(val points: MutableList<Vec3>, var lastUpdateTick: Int) {
        var closest: Vec3? = null
        var furthest: Vec3? = null

        fun updateEndpoints(playerPos: Vec3) {
            if (points.isEmpty()) return
            var closestPoint = points[0]
            var furthestPoint = points[0]
            var minDistSqr = closestPoint.distanceToSqr(playerPos)
            var maxDistSqr = minDistSqr

            for (i in 1 until points.size) {
                val point = points[i]
                val distSqr = point.distanceToSqr(playerPos)
                if (distSqr < minDistSqr) { minDistSqr = distSqr; closestPoint = point }
                if (distSqr > maxDistSqr) { maxDistSqr = distSqr; furthestPoint = point }
            }

            closest = closestPoint
            furthest = furthestPoint
        }
    }

    private val perfBeams = mutableListOf<PerfBeam>()
    private var perfTick = 0

    override fun init() {
        register<PlayerInteractEvent.LEFT_CLICK.AIR> { tryTrigger() }
        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> { tryTrigger() }
        register<PlayerInteractEvent.LEFT_CLICK.ENTITY> { tryTrigger() }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inDungeon) return@register
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@register
            if (packet.particle.type != ParticleTypes.FIREWORK) return@register

            if (performanceMode.value) trackPerfParticle(packet)
            if (hideParticles.value) event.isCanceled = true
        }

        register<TickEvent.Server> {
            if (! performanceMode.value || ! LocationUtils.inDungeon) return@register
            perfTick ++

            val playerPos = mc.player?.position() ?: return@register
            for (beam in perfBeams) beam.updateEndpoints(playerPos)
        }

        register<RenderWorldEvent> {
            if (performanceMode.value) renderPerfBeams(event.ctx)
            else renderProceduralBeams(event.ctx)
        }
    }

    override fun onDisable() {
        super.onDisable()
        activeBeams.clear()
        perfBeams.clear()
        perfTick = 0
    }

    private fun isValidMobTarget(entity: Entity): Boolean {
        if (entity !is LivingEntity) return false
        if (entity is ArmorStand || entity.type == EntityType.SHEEP) return false
        if (entity == mc.player) return false
        return entity.isAlive
    }

    private fun hasClearPath(from: Vec3, to: Vec3): Boolean {
        if (throughWalls.value) return true
        val level = mc.level ?: return true
        val player = mc.player ?: return true

        val ctx = ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        return level.clip(ctx).type == HitResult.Type.MISS
    }

    private fun tryTrigger() {
        if (! enabled) return
        if (! LocationUtils.inDungeon) return
        if (DungeonListener.thePlayer?.clazz != DungeonClass.Mage) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < TRIGGER_DEBOUNCE_MS) return
        lastTriggerTime = now

        val player = mc.player ?: return
        val origin = player.eyePosition
        val direction = player.lookAngle.normalize()

        if (! performanceMode.value) fireBeam(origin, direction, now)
        if (soundEnabled.value) scheduleHitSound(origin, direction)
    }

    private fun fireBeam(origin: Vec3, direction: Vec3, startTime: Long) {
        activeBeams.add(BeamInstance(origin, direction, startTime, beamSeedCounter ++))
        while (activeBeams.size > MAX_ACTIVE_BEAMS) activeBeams.removeAt(0)
    }

    private fun scheduleHitSound(origin: Vec3, direction: Vec3) {
        val length = beamLength.value.toFloat()
        val hitDistance = findHitDistance(origin, direction, length) ?: return

        val growDurationMs = totalDurationMs().toFloat() * GROW_FRACTION
        val targetProgress = (hitDistance / length).coerceIn(0f, 1f)

        val t = (1f - sqrt((1f - targetProgress).coerceIn(0f, 1f))).coerceIn(0f, 1f)
        val delayMs = (t * growDurationMs).toLong().coerceAtLeast(0L)

        ThreadUtils.setTimeout(delayMs) {
            if (! enabled || ! soundEnabled.value) return@setTimeout
            playCastSound()
        }
    }

    private fun findHitDistance(origin: Vec3, direction: Vec3, maxDistance: Float): Float? {
        val level = mc.level ?: return null
        val leniency = hitLeniency.value

        var closest: Float? = null
        for (entity in level.entitiesForRendering()) {
            if (! isValidMobTarget(entity)) continue

            val box = entity.boundingBox
            val center = box.center

            val alongBeam = center.subtract(origin).dot(direction).coerceIn(0.0, maxDistance.toDouble())
            val closestPointOnBeam = origin.add(direction.scale(alongBeam))
            val distFromBeam = closestPointOnBeam.distanceTo(center)

            val entityRadius = maxOf(box.xsize, box.zsize) / 2.0 + leniency
            if (distFromBeam > entityRadius) continue

            if (! hasClearPath(origin, closestPointOnBeam)) continue

            val dist = alongBeam.toFloat()
            if (closest == null || dist < closest !!) closest = dist
        }

        return closest
    }

    private fun renderProceduralBeams(ctx: RenderContext) {
        if (activeBeams.isEmpty()) return

        val style = buildStyle()
        val total = totalDurationMs()
        val now = System.currentTimeMillis()

        val iterator = activeBeams.iterator()
        while (iterator.hasNext()) {
            val beam = iterator.next()
            val elapsed = now - beam.startTime

            if (elapsed >= total) {
                iterator.remove()
                continue
            }

            val (growProgress, fadeAlpha) = envelope(elapsed, total)
            val animTime = (elapsed / 1000f) * animSpeed.value.toFloat()

            BeamRenderer.render(ctx, beam.origin, beam.direction, style, animTime, growProgress, fadeAlpha, beam.seed)
        }
    }

    private fun playCastSound() {
        ThreadUtils.runOnMcThread {
            mc.soundManager.play(SimpleSoundInstance.forUI(soundSettings.sound.value, soundSettings.pitch.value, soundSettings.volume.value))
        }
    }

    private fun buildStyle(): BeamRenderer.BeamStyle {
        return BeamRenderer.BeamStyle(
            shape = BeamRenderer.BeamShape.entries[shapeIndex.value.coerceIn(0, BeamRenderer.BeamShape.entries.lastIndex)],
            colorMode = BeamRenderer.ColorMode.entries[colorModeIndex.value.coerceIn(0, BeamRenderer.ColorMode.entries.lastIndex)],
            primary = primaryColor.value,
            secondary = secondaryColor.value,
            width = width.value.toFloat(),
            opacity = (opacityPct.value.toFloat() / 100f).coerceIn(0f, 1f),
            length = beamLength.value.toFloat(),
            segments = segmentCountSetting.value.coerceIn(2, 128),
            smoothness = smoothnessSetting.value.coerceIn(3, BeamRenderer.MAX_SIDES),
            glow = glow.value,
            throughWalls = throughWalls.value,
            endpointFade = endpointFade.value,
            pulse = pulse.value,
            trail = trail.value
        )
    }

    private fun totalDurationMs(): Long {
        val speed = animSpeed.value.toFloat().coerceAtLeast(0.05f)
        return (BASE_DURATION_MS / speed).toLong().coerceAtLeast(120L)
    }

    private fun envelope(elapsedMs: Long, totalMs: Long): Pair<Float, Float> {
        val t = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        val grow = (t / GROW_FRACTION).coerceIn(0f, 1f)
        val growEased = Animation.easeOutQuad(grow.toDouble()).toFloat()

        val fadeStart = 1f - FADE_FRACTION
        val fade = if (t <= fadeStart) 1f else (1f - (t - fadeStart) / FADE_FRACTION).coerceIn(0f, 1f)

        return growEased to fade
    }

    private fun trackPerfParticle(packet: ClientboundLevelParticlesPacket) {
        val newPoint = Vec3(packet.x, packet.y, packet.z)

        val recentBeam = perfBeams.lastOrNull()
        if (recentBeam != null && (perfTick - recentBeam.lastUpdateTick) <= 1 && isPointInBeamDirection(recentBeam.points, newPoint)) {
            recentBeam.points.add(newPoint)
            recentBeam.lastUpdateTick = perfTick
        } else {
            val newBeam = PerfBeam(mutableListOf(newPoint), perfTick)
            perfBeams.add(newBeam)

            val durationTicks = perfDuration.value.toInt()
            ThreadUtils.scheduledTaskServer(durationTicks) { perfBeams.remove(newBeam) }
        }
    }

    private fun isPointInBeamDirection(points: List<Vec3>, newPoint: Vec3): Boolean {
        if (points.size <= 1) return true
        val lastPoint = points.last()
        return lastPoint.subtract(points[0]).normalize().dot(newPoint.subtract(lastPoint).normalize()) > 0.99
    }

    private fun renderPerfBeams(ctx: RenderContext) {
        if (perfBeams.isEmpty()) return

        val minPoints = perfMinPoints.value
        val color = primaryColor.value
        val width = lineWidth.value.toFloat()
        val phase = throughWalls.value

        for (beam in perfBeams) {
            if (beam.points.size < minPoints) continue
            val closest = beam.closest ?: continue
            val furthest = beam.furthest ?: continue
            if (closest == furthest) continue

            Render3D.renderLine(ctx, closest, furthest, color, width, phase)
        }
    }

    private const val BASE_DURATION_MS = 950f
    private const val GROW_FRACTION = 0.22f
    private const val FADE_FRACTION = 0.34f
    private const val TRIGGER_DEBOUNCE_MS = 150L
    private const val MAX_ACTIVE_BEAMS = 5
}