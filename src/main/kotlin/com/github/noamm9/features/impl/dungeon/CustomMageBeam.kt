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
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.BeamRenderer
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.sqrt

object CustomMageBeam : Feature("Renders a fully custom, animated beam whenever you swing your weapon.") {

    private val performanceMode by ToggleSetting("Performance Mode", false)
        .withDescription("Ultra-lightweight: skips the custom shape/animation system and draws a single line following Hypixel's real beam particles.")
        .section("General")

    private val shapeIndex by DropdownSetting("Beam Shape", 2, listOf("Straight", "Cylinder", "Spiral", "Double Helix", "Wave", "Ribbon", "Lightning"))
        .withDescription("The geometric path the beam follows.").section("Shape").hideIf { performanceMode.value }
    private val width by SliderSetting("Width", 0.05, 0.05, 1.2, 0.01).withDescription("Beam thickness.").hideIf { performanceMode.value }
    private val segmentCountSetting by SliderSetting("Segment Count", 28, 6, 64, 1).withDescription("Longitudinal resolution of the curve.").hideIf { performanceMode.value }
    private val smoothnessSetting by SliderSetting("Smoothness", 3, 3, 24, 1).withDescription("Radial resolution of the cross-section.").hideIf { performanceMode.value }

    private val colorModeIndex by DropdownSetting("Color Mode", 0, listOf("Static", "Gradient", "Rainbow", "Chroma"))
        .withDescription("How the beam is colored, along its length and over time.").section("Colors").hideIf { performanceMode.value }
    private val primaryColor by ColorSetting("Primary Color", Color(255, 110, 0), false)
        .withDescription("Base color. Also the line color in Performance Mode.")
        .hideIf { !performanceMode.value && colorModeIndex.value.equalsOneOf(2, 3) }
    private val secondaryColor by ColorSetting("Secondary Color", Color(200, 60, 255), false)
        .withDescription("Gradient target color. Only used by Gradient.").hideIf { performanceMode.value || colorModeIndex.value != 1 }

    private val opacityPct by SliderSetting("Opacity", 80, 0, 100, 1, "%").withDescription("Overall beam transparency.").section("Animation").hideIf { performanceMode.value }
    private val animSpeed by SliderSetting("Animation Speed", 1.0, 0.2, 4.0, 0.1, "x").withDescription("Speed of scroll/spin/pulse and the sound's delay ramp.")
    private val glow by ToggleSetting("Glow", true).withDescription("Adds a soft, wider layer to fake bloom.").hideIf { performanceMode.value }
    private val pulse by ToggleSetting("Pulse Width", false).withDescription("Makes the beam breathe in and out.").hideIf { performanceMode.value }
    private val endpointFade by ToggleSetting("Endpoint Fade", true).withDescription("Fades the start/tip instead of a hard cut.").hideIf { performanceMode.value }
    private val trail by ToggleSetting("Beam Trail", true).withDescription("Renders fading afterimages behind the beam.").hideIf { performanceMode.value }

    private val lineWidth by SliderSetting("Line Width", 2, 1, 6, 1, "px").withDescription("Line thickness.").section("Performance").hideIf { !performanceMode.value }
    private val perfDuration by SliderSetting("Duration", 40, 5, 100, 1, " ticks").withDescription("How long the line lingers after particles stop.").hideIf { !performanceMode.value }
    private val perfMinPoints by SliderSetting("Min Points", 3, 2, 10, 1).withDescription("Min particles before a beam is rendered.").hideIf { !performanceMode.value }

    private val hideSheep by ToggleSetting("Hide Sheep", true).withDescription("Hides dungeon Sheep entities so the launched sheep's beam doesn't clutter the view.").section("Trigger")

    private val soundEnabled by ToggleSetting("Play Sound", true).withDescription("Plays a sound only on a confirmed hit.").section("Sound")
    private val soundSettings = createSoundSettings("Cast Sound", SoundEvents.NOTE_BLOCK_HARP.value()) { soundEnabled.value }

    private data class BeamInstance(val origin: Vec3, val direction: Vec3, val startTime: Long, val seed: Int, val length: Float)

    private class PerfBeam(val points: MutableList<Vec3>, var lastUpdateTick: Int) {
        var closest: Vec3? = null
        var furthest: Vec3? = null
        fun updateEndpoints(playerPos: Vec3) {
            if (points.isEmpty()) return
            closest = points.minByOrNull { it.distanceToSqr(playerPos) }
            furthest = points.maxByOrNull { it.distanceToSqr(playerPos) }
        }
    }

    private val activeBeams = mutableListOf<BeamInstance>()
    private val perfBeams = mutableListOf<PerfBeam>()
    private var lastTriggerTime = 0L
    private var beamSeedCounter = 0
    private var clickConsumed = false
    private var perfTick = 0

    override fun init() {
        register<PlayerInteractEvent.LEFT_CLICK.AIR> { tryTrigger() }
        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> { tryTrigger() }
        register<PlayerInteractEvent.LEFT_CLICK.ENTITY> { tryTrigger() }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (!LocationUtils.inDungeon) return@register
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@register
            if (packet.particle.type != ParticleTypes.FIREWORK) return@register
            if (performanceMode.value) trackPerfParticle(packet)
            event.isCanceled = true
        }

        register<TickEvent.Server> {
            if (!LocationUtils.inDungeon) return@register
            if (hideSheep.value) mc.level?.entitiesForRendering()?.forEach { if (it.type == EntityType.SHEEP) it.setInvisible(true) }
            if (!performanceMode.value) return@register
            perfTick++
            val playerPos = mc.player?.position() ?: return@register
            perfBeams.forEach { it.updateEndpoints(playerPos) }
        }

        register<TickEvent.End> { if (!mc.options.keyAttack.isDown) clickConsumed = false }

        register<RenderWorldEvent> {
            if (performanceMode.value) renderPerfBeams(event.ctx) else renderProceduralBeams(event.ctx)
        }
    }

    override fun onDisable() {
        super.onDisable()
        activeBeams.clear()
        perfBeams.clear()
        perfTick = 0
        clickConsumed = false
    }

    private fun tryTrigger() {
        if (!enabled || !LocationUtils.inDungeon) return
        if (DungeonListener.thePlayer?.clazz != DungeonClass.Mage) return
        val player = mc.player ?: return
        if (player.mainHandItem.skyblockId in EXCLUDED_ITEM_IDS) return
        if (clickConsumed) return
        clickConsumed = true

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < TRIGGER_DEBOUNCE_MS) return
        lastTriggerTime = now

        val origin = player.eyePosition.subtract(0.0, ORIGIN_EYE_DROP, 0.0)
        val direction = player.lookAngle.normalize()

        val hitDistance = findHitDistance(origin, direction, BEAM_LENGTH, player)
        val visualLength = hitDistance ?: BEAM_LENGTH

        if (!performanceMode.value) {
            activeBeams.add(BeamInstance(origin, direction, now, beamSeedCounter++, visualLength))
            while (activeBeams.size > MAX_ACTIVE_BEAMS) activeBeams.removeAt(0)
        }
        if (soundEnabled.value && hitDistance != null) scheduleHitSound(hitDistance)
    }

    private fun scheduleHitSound(hitDistance: Float) {
        val growDurationMs = totalDurationMs().toFloat() * GROW_FRACTION
        val progress = (hitDistance / BEAM_LENGTH).coerceIn(0f, 1f)
        val t = (1f - sqrt((1f - progress).coerceIn(0f, 1f))).coerceIn(0f, 1f)
        val delayMs = (t * growDurationMs).toLong().coerceAtLeast(0L)

        ThreadUtils.setTimeout(delayMs) {
            if (enabled && soundEnabled.value) ThreadUtils.runOnMcThread {
                mc.soundManager.play(SimpleSoundInstance.forUI(soundSettings.sound.value, soundSettings.pitch.value, soundSettings.volume.value))
            }
        }
    }

    private fun findHitDistance(origin: Vec3, direction: Vec3, maxDistance: Float, player: Player): Float? {
        val level = mc.level ?: return null
        var closest: Float? = null
        for (entity in level.entitiesForRendering()) {
            if (!isValidMobTarget(entity, player)) continue
            val box = entity.boundingBox
            val alongBeam = box.center.subtract(origin).dot(direction).coerceIn(0.0, maxDistance.toDouble())
            val pointOnBeam = origin.add(direction.scale(alongBeam))
            val radius = maxOf(box.xsize, box.zsize) / 2.0 + HIT_LENIENCY
            if (pointOnBeam.distanceTo(box.center) > radius) continue
            if (!hasClearPath(origin, pointOnBeam, player, level)) continue
            closest = minOf(closest ?: Float.MAX_VALUE, alongBeam.toFloat())
        }
        return closest
    }

    private fun isValidMobTarget(entity: Entity, player: Player): Boolean {
        if (entity !is LivingEntity || entity is ArmorStand || entity.type == EntityType.SHEEP) return false
        if (entity == player || !entity.isAlive) return false
        if (isTeammate(entity)) return false
        return true
    }

    private fun isTeammate(entity: Entity): Boolean {
        if (entity !is AbstractClientPlayer || entity.uuid.version() != 4) return false
        return DungeonListener.dungeonTeammates.any { it.entity?.id == entity.id }
    }

    private fun hasClearPath(from: Vec3, to: Vec3, player: Player, level: Level): Boolean {
        return level.clip(ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).type == HitResult.Type.MISS
    }

    private fun isNearSheep(point: Vec3): Boolean {
        val level = mc.level ?: return false
        val r = SHEEP_EXCLUSION_RADIUS
        val box = AABB(point.x - r, point.y - r, point.z - r, point.x + r, point.y + r, point.z + r)
        return level.getEntities(null, box) { it.type == EntityType.SHEEP }.isNotEmpty()
    }

    private fun renderProceduralBeams(ctx: RenderContext) {
        if (activeBeams.isEmpty()) return
        val total = totalDurationMs()
        val now = System.currentTimeMillis()

        activeBeams.removeAll { now - it.startTime >= total }
        for (beam in activeBeams) {
            val elapsed = now - beam.startTime
            val (growProgress, fadeAlpha) = envelope(elapsed, total)
            val animTime = (elapsed / 1000f) * animSpeed.value.toFloat()
            BeamRenderer.render(ctx, beam.origin, beam.direction, buildStyle(beam.length), animTime, growProgress, fadeAlpha, beam.seed)
        }
    }

    private fun buildStyle(length: Float) = BeamRenderer.BeamStyle(
        shape = BeamRenderer.BeamShape.entries[shapeIndex.value.coerceIn(0, BeamRenderer.BeamShape.entries.lastIndex)],
        colorMode = BeamRenderer.ColorMode.entries[colorModeIndex.value.coerceIn(0, BeamRenderer.ColorMode.entries.lastIndex)],
        primary = primaryColor.value,
        secondary = secondaryColor.value,
        width = width.value.toFloat(),
        opacity = (opacityPct.value.toFloat() / 100f).coerceIn(0f, 1f),
        length = length,
        segments = segmentCountSetting.value.coerceIn(2, 128),
        smoothness = smoothnessSetting.value.coerceIn(3, BeamRenderer.MAX_SIDES),
        glow = glow.value,
        throughWalls = false,
        endpointFade = endpointFade.value,
        pulse = pulse.value,
        trail = trail.value
    )

    private fun totalDurationMs(): Long {
        val speed = animSpeed.value.toFloat().coerceAtLeast(0.05f)
        return (BASE_DURATION_MS / speed).toLong().coerceAtLeast(120L)
    }

    private fun envelope(elapsedMs: Long, totalMs: Long): Pair<Float, Float> {
        val t = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        val grow = Animation.easeOutQuad((t / GROW_FRACTION).coerceIn(0f, 1f).toDouble()).toFloat()
        val fadeStart = 1f - FADE_FRACTION
        val fade = if (t <= fadeStart) 1f else (1f - (t - fadeStart) / FADE_FRACTION).coerceIn(0f, 1f)
        return grow to fade
    }

    private fun trackPerfParticle(packet: ClientboundLevelParticlesPacket) {
        val newPoint = Vec3(packet.x, packet.y, packet.z)
        val recentBeam = perfBeams.lastOrNull()
        val isContinuation = recentBeam != null &&
                (perfTick - recentBeam.lastUpdateTick) <= 1 &&
                isPointInBeamDirection(recentBeam.points, newPoint)

        if (!isContinuation && isNearSheep(newPoint)) return

        if (isContinuation) {
            recentBeam.points.add(newPoint)
            recentBeam.lastUpdateTick = perfTick
        } else {
            val newBeam = PerfBeam(mutableListOf(newPoint), perfTick)
            perfBeams.add(newBeam)
            ThreadUtils.scheduledTaskServer(perfDuration.value.toInt()) { perfBeams.remove(newBeam) }
        }
    }

    private fun isPointInBeamDirection(points: List<Vec3>, newPoint: Vec3): Boolean {
        if (points.size <= 1) return true
        val last = points.last()
        return last.subtract(points[0]).normalize().dot(newPoint.subtract(last).normalize()) > 0.99
    }

    private fun renderPerfBeams(ctx: RenderContext) {
        if (perfBeams.isEmpty()) return
        val minPoints = perfMinPoints.value
        for (beam in perfBeams) {
            if (beam.points.size < minPoints) continue
            val closest = beam.closest ?: continue
            val furthest = beam.furthest ?: continue
            if (closest == furthest) continue
            Render3D.renderLine(ctx, closest, furthest, primaryColor.value, lineWidth.value.toFloat(), false)
        }
    }

    private const val BASE_DURATION_MS = 950f
    private const val GROW_FRACTION = 0.22f
    private const val FADE_FRACTION = 0.34f
    private const val TRIGGER_DEBOUNCE_MS = 75L
    private const val MAX_ACTIVE_BEAMS = 5
    private const val SHEEP_EXCLUSION_RADIUS = 1.5
    private const val ORIGIN_EYE_DROP = 0.45
    private const val BEAM_LENGTH = 30f
    private const val HIT_LENIENCY = 0.35
    private val EXCLUDED_ITEM_IDS = setOf(
        "DUNGEONBREAKER", "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP",
        "SUPERBOOM_TNT", "INFINITE_SUPERBOOM_TNT", "SKYBLOCK_MENU"
    )
}
