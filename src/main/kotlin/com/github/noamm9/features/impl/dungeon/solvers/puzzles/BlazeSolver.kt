package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventListener
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.blazeCount
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.firstBlazeColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.lineColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.secondBlazeColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.thirdBlazeColor
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Blaze
import java.awt.Color

object BlazeSolver {
    private val blazeHpRegex = Regex("^\\[Lv15].+Blaze [\\d,]+/([\\d,]+)❤$")

    private var inBlaze = false
    private var reversed = false

    private val blazes = mutableListOf<Entity>()
    private val hpMap = mutableMapOf<Int, Int>()

    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! event.room.name.contains("Blaze")) return

        inBlaze = true
        reversed = event.room.name.equals("Lower Blaze", true) == true
        tickListener.register()
    }

    fun onEntityGlow(event: CheckEntityGlowEvent) {
        if (! inBlaze || blazes.isEmpty()) return
        event.color = blazes.withIndex().find {
            it.value.id == event.entity.id && it.index < blazeCount.value
        }?.let { getBlazeColor(it.index) } ?: return
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inBlaze || blazes.isEmpty()) return
        blazes.forEachIndexed { i, entity ->
            if (i >= blazeCount.value) return@forEachIndexed
            if (i <= 0) return@forEachIndexed
            val prev = blazes[i - 1]

            Render3D.renderLine(
                ctx,
                prev.position().add(y = prev.bbHeight / 2.0),
                entity.position().add(y = entity.bbHeight / 2.0),
                lineColor.value
            )
        }
    }

    private val tickListener = EventListener.create<TickEvent.Start> {
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
    }

    fun reset() {
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