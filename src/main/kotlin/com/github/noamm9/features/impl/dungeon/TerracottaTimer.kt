package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.FlowerPotBlock
import java.util.concurrent.*

object TerracottaTimer: Feature("Displays a timer until terracottas respawn in F6/M6") {
    private var terracottaSpawns = CopyOnWriteArrayList<Pair<BlockPos, Long>>()

    override fun init() {
        register<WorldChangeEvent> { terracottaSpawns.clear() }

        register<BlockChangeEvent> {
            if (LocationUtils.dungeonFloorNumber != 6 || ! LocationUtils.inBoss) return@register
            if (event.newBlock !is FlowerPotBlock) return@register
            if (terracottaSpawns.any { it.first == event.pos }) return@register
            val time = if (LocationUtils.isMasterMode) 240 else 300
            val terracotta = Pair(event.pos, DungeonListener.currentTime + time)
            ThreadUtils.scheduledTaskServer(time) { terracottaSpawns.remove(terracotta) }
            terracottaSpawns.add(terracotta)
        }

        register<ChatMessageEvent> {
            if (LocationUtils.dungeonFloorNumber != 6 || ! LocationUtils.inBoss) return@register
            if (event.unformattedText == "[BOSS] Sadan: ENOUGH!") ThreadUtils.scheduledTaskServer(10) {
                terracottaSpawns.clear()
            }
        }

        register<RenderWorldEvent> {
            terracottaSpawns.ifEmpty { return@register }.forEach { (pos, time) ->
                val timeLeft = (time - DungeonListener.currentTime) / 20.0
                Render3D.renderString(timeLeft.toFixed(1), pos.center, phase = true, scale = 1.35)
            }
        }
    }
}