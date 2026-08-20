package com.github.noamm9.features.impl.floor7.dragons

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.ChoiceConfig
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render3D.renderBillboardedCircle
import com.github.noamm9.utils.render.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.Render3D.renderString
import com.github.noamm9.utils.render.Render3D.renderTracer
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.*
import kotlin.math.sqrt

object WitherDragons: Feature("M7 dragons timers, boxes, priority, health, and alerts") {
    private val dragonTimer by BooleanConfig("Dragon Timer ", true).section("Dragon Timer")
    private val dragonTimerStyle by ChoiceConfig("Timer Style", 0, listOf("Milliseconds", "Seconds", "Ticks")).showIf { dragonTimer.value }
    private val showSymbol by BooleanConfig("Timer Symbol", true).showIf { dragonTimer.value }

    private val dragonArrowStack by BooleanConfig("Arrow Stack Indicator", true).section("Arrow Stack").withDescription("Shows the optimal arrow stack aim position")
    private val indicatorColor by ColorConfig("Indicator Color", Color.CYAN)
    private val indicatorSize by NumberConfig("Indicator Size", 2.0f, 0.1f, 5.0f, 0.1f)
    private val indicatorThickness by NumberConfig("Indicator Thickness", 3.0f, 1.0f, 10.0f, 0.5f)

    private val dragonBoxes by BooleanConfig("Dragon Skip Box ", true).section("Dragon Visuals")
    private val dragonHealth by BooleanConfig("Dragon Health", true)
    private val highlightDragons by BooleanConfig("Highlight Dragons")
    private val dragonTracers by BooleanConfig("Dragon Tracer", false)
    private val tracerThickness by NumberConfig("Tracer Width", 2f, 1f, 5f, 0.1f).showIf { dragonTracers.value }

    val sendTime by BooleanConfig("Send Dragon Time Alive", true).section("Dragon Alerts")
    val sendSpray by BooleanConfig("Send Ice Sprayed", true)
    val sendArrowHit by BooleanConfig("Send Arrows Hit", true)

    val dragonPriorityToggle by BooleanConfig("Dragon Priority", false).section("Dragon Priority")
    val normalPower by NumberConfig("Normal Power", 0f, 0f, 32f, 0.5f).showIf { dragonPriorityToggle.value }
    val easyPower by NumberConfig("Easy Power", 0f, 0f, 32f, 0.5f).showIf { dragonPriorityToggle.value }
    val soloDebuff by ChoiceConfig("Purple Solo Debuff", 0, listOf("Tank", "Healer")).showIf { dragonPriorityToggle.value }
    val soloDebuffOnAll by BooleanConfig("Solo Debuff on All Splits", true).showIf { dragonPriorityToggle.value }

    var priorityDragon = WitherDragonEnum.None

    private const val scoreboardGraceTicks = 40 // how long the dragon needs to be off scoreboard for it to count as dead

    private val smoothedVelocities = ConcurrentHashMap<Int, Vec3>()
    private val fixedStackPositions = mapOf(
        WitherDragonEnum.Green to vec(27f, WitherDragonEnum.Green.spawnPos.y, 90f),
        WitherDragonEnum.Red to vec(28f, WitherDragonEnum.Red.spawnPos.y, 58f),
        WitherDragonEnum.Blue to vec(84f, WitherDragonEnum.Blue.spawnPos.y, 97f)
    )

    override fun init() {
        register<WorldChangeEvent> {
            priorityDragon = WitherDragonEnum.None
            WitherDragonEnum.reset()
            smoothedVelocities.clear()
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (LocationUtils.F7Phase != 5) return@register
            when (val packet = event.packet) {
                is ClientboundLevelParticlesPacket -> DragonCheck.handleSpawnPacket(packet)
                is ClientboundSetEquipmentPacket -> DragonCheck.dragonSprayed(packet)
                is ClientboundAddEntityPacket -> DragonCheck.dragonSpawn(packet)
                is ClientboundSetEntityDataPacket -> DragonCheck.dragonUpdate(packet)
                is ClientboundSoundPacket -> DragonCheck.trackArrows(packet)
            }
        }

        register<EntityUnloadEvent> {
            if (LocationUtils.F7Phase != 5) return@register
            if (event.entity !is EnderDragon) return@register
            val dragon = WitherDragonEnum.entries.find { it.entityId == event.entity.id } ?: return@register
            if (dragon.state != WitherDragonState.ALIVE) return@register
            ChatUtils.debug("dragon", "${dragon.displayName} unloaded with ${formatHealth(dragon.health)} health")

            dragon.entity = null
            dragon.offScoreboardTicks = 0
        }

        register<BlockChangeEvent> {
            if (LocationUtils.F7Phase != 5) return@register
            if (event.newBlock == Blocks.AIR && event.oldBlock == Blocks.COBBLESTONE_SLAB) {
                WitherDragonEnum.entries.find {
                    it.bottomChin.x == event.pos.x && it.bottomChin.z == event.pos.z
                }?.setDead(true)
            }
        }

        register<TickEvent.Server> {
            WitherDragonEnum.entries.forEach { dragon ->
                if (dragon.state == WitherDragonState.SPAWNING) {
                    dragon.timeToSpawn --
                    if (dragon.timeToSpawn <= - 20) dragon.setDead(true)
                }

                if (dragon.state == WitherDragonState.ALIVE && dragon.entity == null) {
                    if (DragonCheck.isAliveOnScoreboard(dragon)) dragon.offScoreboardTicks = 0
                    else if (dragon.offScoreboardTicks < scoreboardGraceTicks) dragon.offScoreboardTicks ++
                    else dragon.setDead().also { ChatUtils.debug("dragon", "${dragon.displayName} set to dead by scoreboard") }
                }
            }
        }

        register<RenderWorldEvent> {
            if (LocationUtils.F7Phase != 5) return@register

            WitherDragonEnum.entries.forEach { dragon ->
                if (dragonHealth.value && dragon.state == WitherDragonState.ALIVE) dragon.entity?.let {
                    event.ctx.renderString(formatHealth(dragon.health), it.renderVec.add(y = - 1), scale = 6f, phase = true)
                }

                if (dragonTimer.value && dragon.state == WitherDragonState.SPAWNING && dragon.timeToSpawn > 0) event.ctx.renderString(
                    "&${dragon.colorCode}${dragon.name}: ${getDragonTimer(dragon.timeToSpawn)}",
                    dragon.spawnPos, scale = 6f
                )

                if (dragonBoxes.value && dragon.state != WitherDragonState.DEAD) event.ctx.renderBoxBounds(
                    dragon.boxesDimensions, dragon.color.withAlpha(0.5f), fill = false, lineWidth = 2.0
                )

                if (dragonArrowStack.value && dragon.state == WitherDragonState.SPAWNING) {
                    val targetPos = (fixedStackPositions[dragon] ?: dragon.spawnPos).add(0.5, 3.5, 0.5)
                    val leadPos = calculateLead(targetPos) ?: return@forEach

                    val distance = player.eyePosition.distanceTo(targetPos)
                    val scaledSize = indicatorSize.value * sqrt(distance / 50.0).coerceAtLeast(0.5)

                    event.ctx.renderBillboardedCircle(leadPos, scaledSize, indicatorColor.value, indicatorThickness.value, phase = true)
                }
            }

            if (dragonTracers.value && priorityDragon != WitherDragonEnum.None && priorityDragon.state == WitherDragonState.SPAWNING) {
                event.ctx.renderTracer(priorityDragon.spawnPos.add(0.5, 3.5, 0.5), priorityDragon.color, tracerThickness.value)
            }
        }

        register<RenderOverlayEvent> {
            if (! dragonTimer.value) return@register
            priorityDragon.takeIf { it != WitherDragonEnum.None }?.let { dragon ->
                if (dragon.state != WitherDragonState.SPAWNING || dragon.timeToSpawn <= 0) return@register
                event.context.drawCenteredString(
                    "&${dragon.colorCode}${getDragonTimer(dragon.timeToSpawn)}",
                    mc.window.guiScaledWidth / 2f,
                    mc.window.guiScaledHeight * 0.4f,
                    scale = 3f,
                )
            }
        }

        register<CheckEntityGlowEvent> {
            if (! highlightDragons.value) return@register
            if (LocationUtils.F7Phase != 5) return@register

            WitherDragonEnum.entries.forEach { dragon ->
                if (dragon.state != WitherDragonState.ALIVE) return@forEach
                if (event.entity.id != dragon.entityId) return@forEach
                event.color = dragon.color
                return@register
            }
        }
    }


    private fun getDragonTimer(spawnTime: Int): String = when (dragonTimerStyle.value) {
        0 -> "${(spawnTime * 50)}${if (showSymbol.value) "ms" else ""}"
        1 -> "${(spawnTime / 20f).toFixed(1)}${if (showSymbol.value) "s" else ""}"
        else -> "${spawnTime}${if (showSymbol.value) "t" else ""}"
    }

    private fun formatHealth(health: Float): String {
        val color = when {
            health >= 750_000_000 -> "&a"
            health >= 500_000_000 -> "&e"
            health >= 250_000_000 -> "&6"
            else -> "&c"
        }

        val str = when {
            health >= 1_000_000_000 -> {
                val b = health / 1_000_000_000
                "${if (b > 1) b.toFixed(1) else b.toInt()}b"
            }

            health >= 1_000_000 -> "${(health / 1_000_000).toInt()}m"
            health >= 1_000 -> "${(health / 1_000).toInt()}k"
            else -> "${health.toInt()}"
        }

        return color + str
    }

    private fun calculateLead(targetPos: Vec3): Vec3? {
        val distToTargetSq = targetPos.distanceToSqr(player.eyePosition)

        var currentArrowDist = 0.0
        var currentSpeed = 3.0
        var currentYVel = 0.0
        var drop = 0.0

        repeat(160) {
            currentArrowDist += currentSpeed
            currentSpeed *= 0.99

            drop += currentYVel
            currentYVel -= 0.05
            currentYVel *= 0.99

            if ((currentArrowDist * currentArrowDist) >= distToTargetSq) {
                return targetPos.subtract(0.0, drop, 0.0)
            }
        }

        return null
    }
}