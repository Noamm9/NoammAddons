package com.github.noamm9.features.impl.floor7.dragons

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.S2CPacketM7Dragon
import gg.essential.universal.ChatColor
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
    val displayName: String,
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
    var health: Float = 1_000_000_000f,
    var offScoreboardTicks: Int = 0
) {
    Red(vec(27, 14, 59), aabb(14.5, 13, 45.5, 39.5, 28, 70.5), 'c', ChatColor.RED.color !!, "Power Dragon", 24.0 .. 30.0, 56.0 .. 62.0, 50, BlockPos(32, 19, 59)),
    Orange(vec(85, 14, 56), aabb(72, 8, 47, 102, 28, 77), '6', ChatColor.GOLD.color !!, "Flame Dragon", 82.0 .. 88.0, 53.0 .. 59.0, 62, BlockPos(80, 19, 56)),
    Green(vec(27, 14, 94), aabb(7, 8, 80, 37, 28, 110), 'a', ChatColor.GREEN.color !!, "Apex Dragon", 23.0 .. 29.0, 91.0 .. 97.0, 52, BlockPos(32, 18, 94)),
    Blue(vec(84, 14, 94), aabb(71.5, 16, 82.5, 96.5, 26, 107.5), 'b', ChatColor.AQUA.color !!, "Ice Dragon", 82.0 .. 88.0, 91.0 .. 97.0, 47, BlockPos(79, 19, 94)),
    Purple(vec(56, 14, 125), aabb(45.5, 13, 113.5, 68.5, 23, 136.5), '5', ChatColor.DARK_PURPLE.color !!, "Soul Dragon", 53.0 .. 59.0, 122.0 .. 128.0, 38, BlockPos(56, 18, 128)),
    None(vec(0, 0, 0), aabb(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), 'f', Color.WHITE, "None", 0.0 .. 0.0, 0.0 .. 0.0, 0, BlockPos(- 1, - 1, - 1));

    fun setAlive(id: Int) {
        state = WitherDragonState.ALIVE
        dragonSpawnCount ++

        timeToSpawn = 100
        timesSpawned ++
        entityId = id
        spawnedTime = DungeonListener.currentTime
        sprayedTime = null
        arrowsHit = 0
        offScoreboardTicks = 0

        if (DungeonListener.dungeonTeammatesNoSelf.isNotEmpty()) {
            WebSocket.send(S2CPacketM7Dragon(S2CPacketM7Dragon.DragonEvent.SPAWN, this))
        }
    }

    fun setDead(silent: Boolean = false) {
        if (state == WitherDragonState.DEAD) return

        state = WitherDragonState.DEAD
        timeToSpawn = 100
        entityId = null
        entity = null

        if (WitherDragons.enabled && ! silent) {
            val stats = mutableListOf<String>()
            if (WitherDragons.sendTime.value) stats.add("&7Time: &6${(DungeonListener.currentTime - spawnedTime) / 20.0}s")
            if (WitherDragons.sendArrowHit.value && this == WitherDragons.priorityDragon) stats.add("&fArrows: &6$arrowsHit")
            if (WitherDragons.sendSpray.value && sprayedTime != null) stats.add("&bSprayed: &c${sprayedTime}t")
            if (stats.isNotEmpty()) modMessage("&${colorCode}${name}: &f${stats.joinToString(" &7| ")}")
        }

        if (WitherDragons.priorityDragon == this) WitherDragons.priorityDragon = None

        if (DungeonListener.dungeonTeammatesNoSelf.isNotEmpty()) {
            WebSocket.send(S2CPacketM7Dragon(S2CPacketM7Dragon.DragonEvent.DEATH, this))
        }
    }

    fun updateEntity(id: Int, hard: Boolean = false) {
        if (hard) {
            entityId = id
            state = WitherDragonState.ALIVE
        }
        else {
            entity = (mc.level?.getEntity(id) as? EnderDragon) ?: return
        }
        offScoreboardTicks = 0
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
                it.offScoreboardTicks = 0
            }
            dragonSpawnCount = 0
        }
    }
}

