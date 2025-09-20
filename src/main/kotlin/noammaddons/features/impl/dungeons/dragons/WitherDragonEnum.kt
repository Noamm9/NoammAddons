package noammaddons.features.impl.dungeons.dragons

import net.minecraft.entity.boss.EntityDragon
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.*
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dragons.DragonPriority.displaySpawningDragon
import noammaddons.features.impl.dungeons.dragons.DragonPriority.findPriority
import noammaddons.features.impl.dungeons.dragons.WitherDragons.currentTick
import noammaddons.features.impl.dungeons.dragons.WitherDragons.priorityDragon
import noammaddons.features.impl.dungeons.dragons.WitherDragons.sendArrowHit
import noammaddons.features.impl.dungeons.dragons.WitherDragons.sendSpray
import noammaddons.features.impl.dungeons.dragons.WitherDragons.sendTime
import noammaddons.utils.ChatUtils.modMessage
import java.awt.Color

enum class WitherDragonEnum(
    val spawnPos: Vec3,
    val boxesDimensions: AxisAlignedBB,
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
    var entity: EntityDragon? = null,
    var sprayedTime: Long? = null,
    var spawnedTime: Long = 0,
    var arrowsHit: Int = 0,
    var awaitingRespawn: Boolean = false,
    var lastPosition: Vec3? = null,
    var lastChunk: Pair<Int, Int>? = null
) {
    Red(Vec3(27.0, 14.0, 59.0), AxisAlignedBB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5), 'c', Color(mc.fontRendererObj.getColorCode('c')), 24.0 .. 30.0, 56.0 .. 62.0, 50, BlockPos(32, 19, 59)),
    Orange(Vec3(85.0, 14.0, 56.0), AxisAlignedBB(72.0, 8.0, 47.0, 102.0, 28.0, 77.0), '6', Color(mc.fontRendererObj.getColorCode('6')), 82.0 .. 88.0, 53.0 .. 59.0, 62, BlockPos(80, 19, 56)),
    Green(Vec3(27.0, 14.0, 94.0), AxisAlignedBB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0), 'a', Color(mc.fontRendererObj.getColorCode('a')), 23.0 .. 29.0, 91.0 .. 97.0, 52, BlockPos(32, 18, 94)),
    Blue(Vec3(84.0, 14.0, 94.0), AxisAlignedBB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5), 'b', Color(mc.fontRendererObj.getColorCode('b')), 82.0 .. 88.0, 91.0 .. 97.0, 47, BlockPos(79, 19, 94)),
    Purple(Vec3(56.0, 14.0, 125.0), AxisAlignedBB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5), '5', Color(mc.fontRendererObj.getColorCode('5')), 53.0 .. 59.0, 122.0 .. 128.0, 38, BlockPos(56, 18, 128)),
    None(Vec3(0.0, 0.0, 0.0), AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), 'f', Color(mc.fontRendererObj.getColorCode('f')), 0.0 .. 0.0, 0.0 .. 0.0, 0, BlockPos(- 1, - 1, - 1));

    fun setAlive(id: Int) {
        state = WitherDragonState.ALIVE
        dragonSpawnCount ++

        timeToSpawn = 100
        timesSpawned ++
        entityId = id
        spawnedTime = currentTick
        sprayedTime = null
        arrowsHit = 0

        awaitingRespawn = false
        lastPosition = null
        lastChunk = null
    }

    fun setDead(silent: Boolean = false) {
        if (state == WitherDragonState.DEAD) return

        state = WitherDragonState.DEAD
        entityId = null
        entity = null
        awaitingRespawn = false
        lastPosition = null
        lastChunk = null

        if (WitherDragons.enabled && currentTick - spawnedTime < skipKillTime && ! silent) {
            val stats = mutableListOf<String>()
            if (sendTime) stats.add("&7Time: &6${(currentTick - spawnedTime) / 20.0}s")
            if (sendArrowHit && this == priorityDragon) stats.add("&fArrows: &6$arrowsHit")
            if (sendSpray && sprayedTime != null) stats.add("&bSprayed: &c${sprayedTime}t")
            if (stats.isNotEmpty()) modMessage("&${colorCode}${name}: &f${stats.joinToString(" &7| ")}")
        }

        if (priorityDragon == this) priorityDragon = None
    }

    fun updateEntity(id: Int, hard: Boolean = false) {
        if (! hard) entity = (mc.theWorld.getEntityByID(id) as? EntityDragon) ?: return
        else {
            entityId = id
            state = WitherDragonState.ALIVE
            awaitingRespawn = false
            lastPosition = null
            lastChunk = null
        }
    }

    companion object {
        enum class WitherDragonState { SPAWNING, ALIVE, DEAD }

        var dragonSpawnCount = 0

        fun reset() {
            WitherDragonEnum.entries.forEach {
                it.timeToSpawn = 100
                it.timesSpawned = 0
                it.state = WitherDragonState.DEAD
                it.entityId = null
                it.entity = null
                it.sprayedTime = null
                it.spawnedTime = 0
                it.awaitingRespawn = false
                it.lastPosition = null
                it.lastChunk = null
            }
            priorityDragon = None
            dragonSpawnCount = 0
        }

        fun handleSpawnPacket(particle: S2APacketParticles) {
            if (particle.particleType != EnumParticleTypes.FLAME) return
            if (particle.xCoordinate % 1 != 0.0) return
            if (particle.zCoordinate % 1 != 0.0) return
            if (particle.particleCount != 20) return
            if (particle.yCoordinate != 19.0) return
            if (particle.particleSpeed != 0f) return
            if (! particle.isLongDistance) return
            if (particle.xOffset != 2f) return
            if (particle.yOffset != 3f) return
            if (particle.zOffset != 2f) return

            if (dragonSpawnCount >= 2) {
                val spawningDragon = WitherDragonEnum.entries.find {
                    particle.xCoordinate in it.xRange && particle.zCoordinate in it.zRange && it.state == WitherDragonState.DEAD
                } ?: return

                spawningDragon.state = WitherDragonState.SPAWNING
                displaySpawningDragon(spawningDragon)
                priorityDragon = spawningDragon
                return
            }

            val (spawned, dragons) = WitherDragonEnum.entries.fold(0 to mutableListOf<WitherDragonEnum>()) { (spawned, dragons), dragon ->
                val newSpawned = spawned + dragon.timesSpawned

                if (dragon.state != WitherDragonState.DEAD) {
                    if (dragon !in dragons) dragons.add(dragon)
                    return@fold newSpawned to dragons
                }

                if (particle.xCoordinate !in dragon.xRange || particle.zCoordinate !in dragon.zRange) return@fold newSpawned to dragons

                dragon.state = WitherDragonState.SPAWNING
                dragons.add(dragon)
                newSpawned to dragons
            }

            if (dragons.isNotEmpty() && (dragons.size == 2 || spawned >= 2)) {
                priorityDragon = findPriority(dragons)
                if (priorityDragon.state != WitherDragonState.SPAWNING) priorityDragon = dragons.first { it != priorityDragon }
                displaySpawningDragon(priorityDragon)
            }
        }
    }
}