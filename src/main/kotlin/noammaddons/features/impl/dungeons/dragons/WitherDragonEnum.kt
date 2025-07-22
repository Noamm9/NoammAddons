package noammaddons.features.impl.dungeons.dragons

import net.minecraft.entity.boss.EntityDragon
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.*
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dragons.DragonCheck.dragonEntityList
import noammaddons.features.impl.dungeons.dragons.DragonCheck.lastDragonDeath
import noammaddons.features.impl.dungeons.dragons.DragonPriority.displaySpawningDragon
import noammaddons.features.impl.dungeons.dragons.DragonPriority.findPriority
import noammaddons.features.impl.dungeons.dragons.WitherDragons.currentTick
import noammaddons.features.impl.dungeons.dragons.WitherDragons.priorityDragon
import noammaddons.features.impl.dungeons.dragons.WitherDragons.sendArrowHit
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
    var timeToSpawn: Int = 100,
    var state: WitherDragonState = WitherDragonState.DEAD,
    var timesSpawned: Int = 0,
    var entityId: Int? = null,
    var entity: EntityDragon? = null,
    var isSprayed: Boolean = false,
    var spawnedTime: Long = 0,
    val skipKillTime: Long = 0,
    var arrowsHit: Int = 0
) {
    Red(Vec3(27.0, 14.0, 59.0), AxisAlignedBB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5), 'c', Color(mc.fontRendererObj.getColorCode('c')), 24.0 .. 30.0, 56.0 .. 62.0, skipKillTime = 50),
    Orange(Vec3(85.0, 14.0, 56.0), AxisAlignedBB(72.0, 8.0, 47.0, 102.0, 28.0, 77.0), '6', Color(mc.fontRendererObj.getColorCode('6')), 82.0 .. 88.0, 53.0 .. 59.0, skipKillTime = 62),
    Green(Vec3(27.0, 14.0, 94.0), AxisAlignedBB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0), 'a', Color(mc.fontRendererObj.getColorCode('a')), 23.0 .. 29.0, 91.0 .. 97.0, skipKillTime = 52),
    Blue(Vec3(84.0, 14.0, 94.0), AxisAlignedBB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5), 'b', Color(mc.fontRendererObj.getColorCode('b')), 82.0 .. 88.0, 91.0 .. 97.0, skipKillTime = 47),
    Purple(Vec3(56.0, 14.0, 125.0), AxisAlignedBB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5), '5', Color(mc.fontRendererObj.getColorCode('5')), 53.0 .. 59.0, 122.0 .. 128.0, skipKillTime = 38),
    None(Vec3(0.0, 0.0, 0.0), AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), 'f', Color(mc.fontRendererObj.getColorCode('f')), 0.0 .. 0.0, 0.0 .. 0.0);

    fun setAlive(entityId: Int) {
        state = WitherDragonState.ALIVE

        timeToSpawn = 100
        timesSpawned ++
        this.entityId = entityId
        spawnedTime = currentTick
        isSprayed = false
        arrowsHit = 0
    }

    fun setDead() {
        state = WitherDragonState.DEAD
        dragonEntityList.remove(entity)
        entityId = null
        entity = null
        lastDragonDeath = this

        if (sendArrowHit && WitherDragons.enabled && currentTick - spawnedTime < skipKillTime && priorityDragon == this) {
            modMessage("&fArrows Hit on &${colorCode}${name}&f: &6$arrowsHit.")
        }

        if (priorityDragon == this) priorityDragon = None

        if (sendTime && WitherDragons.enabled) {
            modMessage("&${colorCode}${name} &7was alive for &6${(currentTick - spawnedTime) / 20.0}s")
        }
    }

    fun updateEntity(entityId: Int) {
        entity = (mc.theWorld.getEntityByID(entityId) as? EntityDragon)?.also { dragonEntityList.add(it) } ?: return
    }

    companion object {
        enum class WitherDragonState { SPAWNING, ALIVE, DEAD }

        fun reset(soft: Boolean = false) {
            if (soft) return WitherDragonEnum.entries.forEach {
                it.state = WitherDragonState.DEAD
                it.timesSpawned ++
            }

            WitherDragonEnum.entries.forEach {
                it.timeToSpawn = 100
                it.timesSpawned = 0
                it.state = WitherDragonState.DEAD
                it.entityId = null
                it.entity = null
                it.isSprayed = false
                it.spawnedTime = 0
            }
            dragonEntityList.clear()
            priorityDragon = None
            lastDragonDeath = None
        }

        fun handleSpawnPacket(particle: S2APacketParticles) {
            if (
                particle.particleCount != 20 ||
                particle.yCoordinate != 19.0 ||
                particle.particleType != EnumParticleTypes.FLAME ||
                particle.xOffset != 2f ||
                particle.yOffset != 3f ||
                particle.zOffset != 2f ||
                particle.particleSpeed != 0f ||
                ! particle.isLongDistance ||
                particle.xCoordinate % 1 != 0.0 ||
                particle.zCoordinate % 1 != 0.0
            ) return

            val (spawned, dragons) = WitherDragonEnum.entries.fold(0 to mutableListOf<WitherDragonEnum>()) { (spawned, dragons), dragon ->
                val newSpawned = spawned + dragon.timesSpawned

                if (dragon.state == WitherDragonState.SPAWNING) {
                    if (dragon !in dragons) dragons.add(dragon)
                    return@fold newSpawned to dragons
                }

                if (particle.xCoordinate !in dragon.xRange || particle.zCoordinate !in dragon.zRange) return@fold newSpawned to dragons

                dragon.state = WitherDragonState.SPAWNING
                dragons.add(dragon)
                newSpawned to dragons
            }

            if (dragons.isNotEmpty() && (dragons.size == 2 || spawned >= 2) && (priorityDragon == None || priorityDragon.entity?.isDead == false))
                priorityDragon = findPriority(dragons).also { displaySpawningDragon(it) }
        }
    }
}