package com.github.noamm9.features.impl.F7.dragons

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.F7.dragons.WitherDragons.priorityDragon
import com.github.noamm9.features.impl.F7.dragons.WitherDragons.sendArrowHit
import com.github.noamm9.features.impl.F7.dragons.WitherDragons.sendSpray
import com.github.noamm9.features.impl.F7.dragons.WitherDragons.sendTime
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.dungeons.DungeonListener
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color

enum class WitherDragonEnum(
    val spawnPos: Vec3,
    val boxesDimensions: AABB,
    val colorCode: Char,
    val color: Color,
    val xRange: ClosedRange<Double>,
    val zRange: ClosedRange<Double>,
    val skipKillTime: Long = 0,
    val bottomChin: BlockPos,
    var timeToSpawn: Int = 100,
    var state: WitherDragonState = WitherDragonState.DEAD,
    var timesSpawned: Int = 0,
    var entityId: Int? = null,
    var entity: EnderDragon? = null,
    var sprayedTime: Long? = null,
    var spawnedTime: Long = 0,
    var arrowsHit: Int = 0,
    var health: Float = 1_000_000_000f
) {
    Red(Vec3(27.0, 14.0, 59.0), AABB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5), 'c', Color(ChatFormatting.RED.color !!), 24.0 .. 30.0, 56.0 .. 62.0, 50, BlockPos(32, 19, 59)),
    Orange(Vec3(85.0, 14.0, 56.0), AABB(72.0, 8.0, 47.0, 102.0, 28.0, 77.0), '6', Color(ChatFormatting.GOLD.color !!), 82.0 .. 88.0, 53.0 .. 59.0, 62, BlockPos(80, 19, 56)),
    Green(Vec3(27.0, 14.0, 94.0), AABB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0), 'a', Color(ChatFormatting.GREEN.color !!), 23.0 .. 29.0, 91.0 .. 97.0, 52, BlockPos(32, 18, 94)),
    Blue(Vec3(84.0, 14.0, 94.0), AABB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5), 'b', Color(ChatFormatting.AQUA.color !!), 82.0 .. 88.0, 91.0 .. 97.0, 47, BlockPos(79, 19, 94)),
    Purple(Vec3(56.0, 14.0, 125.0), AABB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5), '5', Color(ChatFormatting.DARK_PURPLE.color !!), 53.0 .. 59.0, 122.0 .. 128.0, 38, BlockPos(56, 18, 128)),
    None(Vec3(0.0, 0.0, 0.0), AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), 'f', Color.WHITE, 0.0 .. 0.0, 0.0 .. 0.0, 0, BlockPos(- 1, - 1, - 1));

    fun setAlive(id: Int) {
        state = WitherDragonState.ALIVE
        dragonSpawnCount ++

        timeToSpawn = 100
        timesSpawned ++
        entityId = id
        spawnedTime = DungeonListener.currentTime
        sprayedTime = null
        arrowsHit = 0
    }

    fun setDead(silent: Boolean = false) {
        if (state == WitherDragonState.DEAD) return

        state = WitherDragonState.DEAD
        timeToSpawn = 100
        entityId = null
        entity = null

        if (WitherDragons.enabled && ! silent) {
            val stats = mutableListOf<String>()
            if (sendTime.value) stats.add("&7Time: &6${(DungeonListener.currentTime - spawnedTime) / 20.0}s")
            if (sendArrowHit.value && this == priorityDragon) stats.add("&fArrows: &6$arrowsHit")
            if (sendSpray.value && sprayedTime != null) stats.add("&bSprayed: &c${sprayedTime}t")
            if (stats.isNotEmpty()) modMessage("&${colorCode}${name}: &f${stats.joinToString(" &7| ")}")
        }

        if (priorityDragon == this) priorityDragon = None
    }

    fun updateEntity(id: Int, hard: Boolean = false) {
        if (! hard) entity = (mc.level?.getEntity(id) as? EnderDragon) ?: return
        else {
            entityId = id
            state = WitherDragonState.ALIVE
        }
    }

    companion object {
        var dragonSpawnCount = 0

        fun reset() {
            entries.forEach {
                it.timeToSpawn = 100
                it.timesSpawned = 0
                it.state = WitherDragonState.DEAD
                it.entityId = null
                it.entity = null
                it.sprayedTime = null
                it.spawnedTime = 0
                it.health = 1_000_000_000f
            }
            dragonSpawnCount = 0
        }
    }
}

