package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ActionBarParser
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.*
import kotlin.math.sqrt

object LeadIndicator: Feature("Shows a lead indicator for all Ender Dragons when holding a shortbow.") {
    private val indicatorColor by ColorSetting("Indicator Color", Color.CYAN)
    private val indicatorSize by SliderSetting("Indicator Size", 2.0f, 0.1f, 5.0f, 0.1f)
    private val indicatorThickness by SliderSetting("Indicator Thickness", 3.0f, 1.0f, 10.0f, 0.5f)
    private val showTracer by ToggleSetting("Show Tracer", false)

    private const val BASE_PROJECTILE_SPEED = 3.0
    private var cachedProjectileSpeed = BASE_PROJECTILE_SPEED
    private const val maxTicks = 160
    private const val ALPHA = 0.15

    private val smoothedVelocities = ConcurrentHashMap<Int, Vec3>()
    private val cachedDragons = HashSet<Int>()

    override fun init() {
        register<WorldChangeEvent> {
            cachedDragons.clear()
            smoothedVelocities.clear()
            cachedProjectileSpeed = BASE_PROJECTILE_SPEED
        }

        register<EntityUnloadEvent> {
            if (event.entity !is EnderDragon) return@register
            cachedDragons.remove(event.entity.id)
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@register
            if (packet.type != EntityType.ENDER_DRAGON) return@register
            cachedDragons.add(packet.id)
        }

        register<TickEvent.Start> {
            cachedProjectileSpeed = calculateProjectileSpeed()

            for (id in cachedDragons) {
                val dragon = mc.level?.getEntity(id) as? EnderDragon ?: continue
                val currentVel = dragon.position().subtract(dragon.xo, dragon.yo, dragon.zo)
                val lastVel = smoothedVelocities[dragon.id] ?: currentVel
                val smoothed = lastVel.scale(1.0 - ALPHA).add(currentVel.scale(ALPHA))
                smoothedVelocities[dragon.id] = smoothed
            }
        }

        register<RenderWorldEvent> {
            val player = mc.player ?: return@register
            val eyePos = player.eyePosition

            for (id in cachedDragons) {
                val dragon = mc.level?.getEntity(id) as? EnderDragon ?: continue
                val leadPos = calculateLead(eyePos, dragon, cachedProjectileSpeed) ?: continue

                val distance = eyePos.distanceTo(dragon.position())
                val scaledSize = indicatorSize.value * sqrt(distance / 50.0).coerceAtLeast(0.5)

                Render3D.renderBillboardedCircle(event.ctx, leadPos, scaledSize, indicatorColor.value, indicatorThickness.value, phase = true)

                if (showTracer.value) {
                    val entityPos = dragon.renderVec.add(0.0, dragon.bbHeight / 2.0, 0.0)
                    Render3D.renderLine(event.ctx, entityPos, leadPos, indicatorColor.value, indicatorThickness.value, phase = true)
                }
            }
        }
    }

    private fun calculateProjectileSpeed(): Double {
        var speed = BASE_PROJECTILE_SPEED

        var terrorPieces = 0
        var maxTierMultiplier = 0.01

        for (item in PlayerUtils.getArmor()) {
            val id = item.skyblockId
            if (id.contains("TERROR")) {
                terrorPieces ++
                val tierMultiplier = when {
                    id.startsWith("INFERNAL_") -> 0.03
                    id.startsWith("FIERY_") -> 0.025
                    id.startsWith("BURNING_") -> 0.02
                    id.startsWith("HOT_") -> 0.015
                    else -> 0.01
                }
                if (tierMultiplier > maxTierMultiplier) maxTierMultiplier = tierMultiplier
            }
        }

        if (terrorPieces >= 2 && ActionBarParser.stackSymbol == "⁑") {
            speed *= (1.0 + ActionBarParser.netherArmorStacks * maxTierMultiplier)
        }

        return speed
    }

    private fun calculateLead(playerPos: Vec3, target: EnderDragon, vP: Double): Vec3? {
        val targetPos = target.renderVec.add(0.0, target.bbHeight / 2.0, 0.0)
        val targetVel = smoothedVelocities[target.id] ?: target.position().subtract(target.xo, target.yo, target.zo)

        var currentArrowDist = 0.0
        var currentSpeed = vP

        var drop = 0.0
        var currentYVel = 0.0

        for (t in 1 .. maxTicks) {
            currentArrowDist += currentSpeed
            currentSpeed *= 0.99

            drop += currentYVel
            currentYVel -= 0.05
            currentYVel *= 0.99

            val futureX = targetPos.x + (targetVel.x * t)
            val futureY = targetPos.y + (targetVel.y * t)
            val futureZ = targetPos.z + (targetVel.z * t)

            val dx = futureX - playerPos.x
            val dy = futureY - playerPos.y
            val dz = futureZ - playerPos.z
            val distToTargetSq = (dx * dx) + (dy * dy) + (dz * dz)

            if ((currentArrowDist * currentArrowDist) >= distToTargetSq) {
                return Vec3(futureX, futureY - drop, futureZ)
            }
        }
        return null
    }
}