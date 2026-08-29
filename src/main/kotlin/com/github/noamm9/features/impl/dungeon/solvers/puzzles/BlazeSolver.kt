package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.blazeCount
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.firstBlazeColor
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.lineColor
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.secondBlazeColor
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.thirdBlazeColor
import com.github.noamm9.utils.ChatUtils.sendPartyMessage
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.render.world.Render3D.renderLine
import com.github.noamm9.utils.render.world.RenderContext
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Blaze
import java.awt.Color
import java.util.concurrent.*

object BlazeSolver: PuzzleSolver {
    override val enabled get() = PuzzleSolvers.blaze.value
    private val blazeHpRegex = Regex("^\\[Lv15].+Blaze [\\d,]+/([\\d,]+)❤$")
    private val blazes = CopyOnWriteArrayList<Blaze>()
    private val hpMap = ConcurrentHashMap<Int, Int>()
    private var lastBlazeCount = 10

    private var inBlaze = false
    private var reversed = false

    override fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! event.room.name.contains("Blaze")) return

        inBlaze = true
        reversed = event.room.name.equals("Lower Blaze", true) == true
        tickListener.register()
        lastBlazeCount = 10
    }

    override fun onEntityGlow(event: CheckEntityGlowEvent) {
        if (! inBlaze || hpMap.isEmpty()) return
        if (event.entity !is Blaze) return
        val index = blazes.indexOf(event.entity)
        if (index == - 1) return
        if (index >= blazeCount.value) return
        event.color = getBlazeColor(index)
    }

    override fun onRenderWorld(ctx: RenderContext) {
        if (! inBlaze || blazes.isEmpty()) return
        blazes.forEachIndexed { i, entity ->
            if (i == 0) return@forEachIndexed
            if (i >= blazeCount.value) return@forEachIndexed
            val prev = blazes.getOrNull(i - 1) ?: return@forEachIndexed

            ctx.renderLine(
                prev.position().add(y = prev.bbHeight / 2.0),
                entity.position().add(y = entity.bbHeight / 2.0),
                lineColor.value
            )
        }
    }

    private val tickListener = EventBus.listener<TickEvent.Start> {
        blazes.clear()
        hpMap.clear()

        mc.level?.entitiesForRendering()?.filterIsInstance<ArmorStand>()?.forEach { armorStand ->
            val name = armorStand.customName?.unformattedText ?: return@forEach
            val match = blazeHpRegex.find(name) ?: return@forEach
            val health = match.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach

            val blaze = mc.level !!.getEntitiesOfClass(
                Blaze::class.java,
                armorStand.boundingBox.expandTowards(0.0, - 2.0, 0.0)
            ).firstOrNull() ?: return@forEach

            if (blaze in blazes || hpMap.containsKey(blaze.id)) return@forEach

            hpMap[blaze.id] = health
            blazes.add(blaze)
        }

        blazes.sortBy { hpMap[it.id] }
        if (reversed) blazes.reverse()

        if (blazes.isEmpty() && lastBlazeCount == 1) {
            sendPartyMessage("Blaze Done!")
            lastBlazeCount = 0
        }

        lastBlazeCount = blazes.size
    }

    override fun reset() {
        tickListener.unregister()
        inBlaze = false
        reversed = false
        blazes.clear()
        hpMap.clear()
    }

    private fun getBlazeColor(index: Int): Color = when (index) {
        0 -> firstBlazeColor.value
        1 -> secondBlazeColor.value
        else -> thirdBlazeColor.value
    }
}