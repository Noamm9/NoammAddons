package noammaddons.features.impl.dungeons.dragons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.server.*
import net.minecraft.util.EnumParticleTypes
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dragons.DragonPriority.displaySpawningDragon
import noammaddons.features.impl.dungeons.dragons.DragonPriority.findPriority
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.WitherDragonState.*
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.dragonSpawnCount
import noammaddons.features.impl.dungeons.dragons.WitherDragons.currentTick
import noammaddons.features.impl.dungeons.dragons.WitherDragons.priorityDragon
import noammaddons.utils.MathUtils.Vec3
import noammaddons.utils.MathUtils.multiply
import noammaddons.utils.MathUtils.xzInAABB

object DragonCheck {
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
                particle.xCoordinate in it.xRange && particle.zCoordinate in it.zRange && it.state == DEAD
            } ?: return

            spawningDragon.state = SPAWNING
            displaySpawningDragon(spawningDragon)
            priorityDragon = spawningDragon
            return
        }

        val (spawned, dragons) = WitherDragonEnum.entries.fold(0 to mutableListOf<WitherDragonEnum>()) { (spawned, dragons), dragon ->
            val newSpawned = spawned + dragon.timesSpawned

            if (dragon.state != DEAD) {
                if (dragon !in dragons) dragons.add(dragon)
                return@fold newSpawned to dragons
            }

            if (particle.xCoordinate !in dragon.xRange || particle.zCoordinate !in dragon.zRange) return@fold newSpawned to dragons

            dragon.state = SPAWNING
            dragons.add(dragon)
            newSpawned to dragons
        }

        if (dragons.isNotEmpty() && (dragons.size == 2 || spawned >= 2)) {
            priorityDragon = findPriority(dragons)
            if (priorityDragon.state != SPAWNING) priorityDragon = dragons.first { it != priorityDragon }
            displaySpawningDragon(priorityDragon)
        }
    }

    fun dragonUpdate(packet: S1CPacketEntityMetadata) {
        val dragon = WitherDragonEnum.entries.find { it.entityId == packet.entityId }?.apply {
            if (entity == null || entity?.isDead == true) updateEntity(packet.entityId)
        } ?: return

        (packet.func_149376_c().find { it.dataValueId == 6 }?.getObject() as? Float)?.let { health ->
            if (health <= 0 && dragon.state != DEAD) dragon.setDead()
        }
    }

    fun dragonSpawn(packet: S0FPacketSpawnMob) {
        if (packet.entityType != 63) return
        val spawnVec = Vec3(packet.x, packet.y, packet.z).multiply(0.03125)
        val newId = packet.entityID

        WitherDragonEnum.entries.find { dragon ->
            spawnVec.xzInAABB(dragon.boxesDimensions) && dragon.state == SPAWNING
        }?.setAlive(newId)
    }

    fun dragonSprayed(packet: S04PacketEntityEquipment) {
        if (packet.itemStack?.item != Item.getItemFromBlock(Blocks.packed_ice)) return
        val sprayedEntity = mc.theWorld?.getEntityByID(packet.entityID) as? EntityArmorStand ?: return

        WitherDragonEnum.entries.forEach { dragon ->
            if (dragon.sprayedTime != null || dragon.state != ALIVE || dragon.entity == null || sprayedEntity.getDistanceToEntity(dragon.entity) > 8) return@forEach
            dragon.sprayedTime = currentTick - dragon.spawnedTime
        }
    }

    fun trackArrows(packet: S29PacketSoundEffect) {
        if (packet.soundName != "random.successful_hit") return
        priorityDragon.takeUnless { it == WitherDragonEnum.None }?.let {
            if (it.state == ALIVE && currentTick - it.spawnedTime <= it.skipKillTime) {
                it.arrowsHit ++
            }
        }
    }
}