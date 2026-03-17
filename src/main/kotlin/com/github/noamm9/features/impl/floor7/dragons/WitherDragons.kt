package com.github.noamm9.features.impl.floor7.dragons

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.awt.Color

object WitherDragons: Feature(
    name = "Wither Dragons",
    description = "M7 dragons timers, boxes, priority, health, and alerts"
) {
    private val dragonTimer by ToggleSetting("Dragon Timer ", true).section("Dragon Timer")
    private val dragonTimerStyle by DropdownSetting("Timer Style", 0, listOf("Milliseconds", "Seconds", "Ticks")).showIf { dragonTimer.value }
    private val showSymbol by ToggleSetting("Timer Symbol", true).showIf { dragonTimer.value }

    private val dragonBoxes by ToggleSetting("Dragon Skip Box ", true).section("Dragon Box")

    private val dragonHealth by ToggleSetting("Dragon Health", true).section("Dragon Visuals")
    private val highlightDragons by ToggleSetting("Highlight Dragons")
    private val dragonTracers by ToggleSetting("Dragon Tracer", false)
    private val tracerThickness by SliderSetting("Tracer Width", 2f, 1f, 5f, 0.1f).showIf { dragonTracers.value }

    val sendTime by ToggleSetting("Send Dragon Time Alive", true).section("Dragon Alerts")
    val sendSpray by ToggleSetting("Send Ice Sprayed", true)
    val sendArrowHit by ToggleSetting("Send Arrows Hit", true)

    val dragonPriorityToggle by ToggleSetting("Dragon Priority", false).section("Dragon Priority")
    val normalPower by SliderSetting("Normal Power", 0f, 0f, 32f, 0.5f).showIf { dragonPriorityToggle.value }
    val easyPower by SliderSetting("Easy Power", 0f, 0f, 32f, 0.5f).showIf { dragonPriorityToggle.value }
    val soloDebuff by DropdownSetting("Purple Solo Debuff", 0, listOf("Tank", "Healer")).showIf { dragonPriorityToggle.value }
    val soloDebuffOnAll by ToggleSetting("Solo Debuff on All Splits", true).showIf { dragonPriorityToggle.value }

    var priorityDragon = WitherDragonEnum.None

    override fun init() {
        register<WorldChangeEvent> {
            priorityDragon = WitherDragonEnum.None
            WitherDragonEnum.reset()
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

        register<EntityDeathEvent> {
            if (LocationUtils.F7Phase != 5) return@register
            if (event.entity !is EnderDragon) return@register
            WitherDragonEnum.entries.find { it.entityId == event.entity.id }?.setDead()
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
            WitherDragonEnum.entries.forEach {
                if (it.state == WitherDragonState.SPAWNING) {
                    it.timeToSpawn --
                    if (it.timeToSpawn <= - 20) it.setDead(true)
                }
            }
        }

        register<RenderWorldEvent> {
            if (LocationUtils.F7Phase != 5) return@register

            WitherDragonEnum.entries.forEach { dragon ->
                if (dragonHealth.value && dragon.state == WitherDragonState.ALIVE) dragon.entity?.let {
                    Render3D.renderString(formatHealth(dragon.health), it.renderVec.add(y = - 1), scale = 6f, phase = true)
                }

                if (dragonTimer.value && dragon.state == WitherDragonState.SPAWNING && dragon.timeToSpawn > 0) Render3D.renderString(
                    "&${dragon.colorCode}${dragon.name}: ${getDragonTimer(dragon.timeToSpawn)}",
                    dragon.spawnPos, scale = 6f
                )

                if (dragonBoxes.value && dragon.state != WitherDragonState.DEAD) drawDragonBox(
                    event.ctx, dragon.boxesDimensions, dragon.color.withAlpha(0.5f)
                )
            }

            if (dragonTracers.value && priorityDragon != WitherDragonEnum.None && priorityDragon.state == WitherDragonState.SPAWNING) {
                Render3D.renderTracer(event.ctx, priorityDragon.spawnPos.add(0.5, 3.5, 0.5), priorityDragon.color, tracerThickness.value)
            }
        }

        register<RenderOverlayEvent> {
            if (! dragonTimer.value) return@register
            priorityDragon.takeIf { it != WitherDragonEnum.None }?.let { dragon ->
                if (dragon.state != WitherDragonState.SPAWNING || dragon.timeToSpawn <= 0) return@register
                Render2D.drawCenteredString(
                    event.context,
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

    private fun drawDragonBox(ctx: RenderContext, aabb: AABB, color: Color) {
        Render3D.renderBoxBounds(
            ctx,
            aabb.minX, aabb.minY, aabb.minZ,
            aabb.maxX, aabb.maxY, aabb.maxZ,
            color,
            color,
            outline = true,
            fill = false,
            phase = false,
            lineWidth = 2.0
        )
    }
}
