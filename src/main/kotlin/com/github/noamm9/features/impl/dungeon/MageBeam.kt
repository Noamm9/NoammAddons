package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderLine
import com.github.noamm9.utils.render.world.Render3D.renderRainbowLine
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.abs

object MageBeam: Feature("Renders a fully custom, animated beam whenever a mage casts their beam ability.") {
    private val color by ColorSetting("Primary Color", Color.WHITE, false).withDescription("The color of the beam line")
    private val lineWidth by SliderSetting("Line Width", 2, 1, 6, 1, "px").withDescription("Thickness of the line.")
    private val duration by SliderSetting("Duration", 40, 5, 100, 1, " ticks").withDescription("How long the beam shows.")
    private val fade by ToggleSetting("Fade").withDescription("Animates the beam slowly disappearing")
    private val hideSheep by ToggleSetting("Hide Sheep", true).withDescription("Prevents the Sheep from spawning.")
    private val rainbow by ToggleSetting("&dI am Skizo!!!!")

    private val beams = linkedSetOf<Beam>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inDungeon) return@register
            when (val packet = event.packet) {
                is ClientboundLevelParticlesPacket -> {
                    if (packet.particle.type != ParticleTypes.FIREWORK) return@register
                    Beam.onPoint(Vec3(packet.x, packet.y, packet.z), DungeonListener.currentTime, beams.lastOrNull())
                    event.isCanceled = true
                }

                is ClientboundAddEntityPacket -> {
                    if (! hideSheep.value) return@register
                    if (packet.type != EntityType.SHEEP) return@register
                    if (player.position().distanceToSqr(packet.x, packet.y, packet.z) > 9) return@register
                    event.isCanceled = true
                }
            }
        }

        register<RenderWorldEvent> {
            for (beam in beams) {
                if (beam.points.size < 6) continue
                val alpha = if (fade.value) beam.anim.update(0f).let { beam.anim.value } else 1f
                if (rainbow.value) event.ctx.renderRainbowLine(beam.min, beam.max, lineWidth.value, alpha)
                else event.ctx.renderLine(beam.min, beam.max, color.value.withAlpha(alpha), lineWidth.value)
            }
        }
    }

    private class Beam(point: Vec3, var updateTick: Long) {
        val points = mutableListOf(point)

        val anim by lazy { Animation(duration.value * 50L, 1f) }
        val min get() = points.first()
        val max get() = points.last()

        fun inLine(point: Vec3): Boolean {
            if (points.size < 2) return true

            val onLine = abs(max.subtract(min).normalize().dot(point.subtract(max).normalize())) > 0.99
            val distSq = point.distanceToSqr(max)

            return onLine && distSq < pointSpace * pointSpace
        }

        companion object {
            const val pointSpace = 0.5 // avg distance between 2 particals

            fun onPoint(point: Vec3, tick: Long, beam: Beam?) {
                if (beams.any { point in it.points }) return // hypixel sends the same beam twise, thx hypixel

                if (beam != null && tick - beam.updateTick <= 1 && beam.inLine(point)) {
                    beam.points.add(point)
                    beam.updateTick = tick
                }
                else {
                    val newBeam = Beam(point, tick)
                    ThreadUtils.scheduledTaskServer(duration.value) { beams.remove(newBeam) }
                    beams.add(newBeam)
                }
            }
        }
    }
}