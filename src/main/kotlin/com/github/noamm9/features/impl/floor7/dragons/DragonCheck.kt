package com.github.noamm9.features.impl.floor7.dragons

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.MathUtils.xzInAABB
import com.github.noamm9.utils.dungeons.DungeonListener
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.*
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

object DragonCheck {
    fun handleSpawnPacket(particle: ClientboundLevelParticlesPacket) {
        if (particle.particle.type != ParticleTypes.FLAME) return
        if (particle.x % 1 != 0.0) return
        if (particle.z % 1 != 0.0) return
        if (particle.count != 20) return
        if (particle.y != 19.0) return
        if (particle.maxSpeed != 0f) return
        if (! particle.isOverrideLimiter) return
        if (particle.xDist != 2f) return
        if (particle.yDist != 3f) return
        if (particle.zDist != 2f) return

        val dragons = mutableListOf<WitherDragonEnum>()

        WitherDragonEnum.entries.forEach { dragon ->
            if (dragon.state == WitherDragonState.SPAWNING) {
                dragons.add(dragon)
                return@forEach
            }

            if (particle.x in dragon.xRange && particle.z in dragon.zRange) {
                dragon.state = WitherDragonState.SPAWNING
                dragons.add(dragon)
            }
        }

        if (dragons.isNotEmpty()) {
            WitherDragons.priorityDragon = DragonPriority.findPriority(dragons)
        }
    }

    fun dragonUpdate(packet: ClientboundSetEntityDataPacket) {
        val dragon = WitherDragonEnum.entries.find { it.entityId == packet.id }?.apply {
            if (entity == null || entity?.isAlive != true) updateEntity(packet.id)
        } ?: return

        dragon.health = (packet.packedItems.find { it.id == 9 }?.value as? Float) ?: return

        if (dragon.health <= 0 && dragon.state != WitherDragonState.DEAD) {
            dragon.setDead()
        }
    }

    fun dragonSpawn(packet: ClientboundAddEntityPacket) {
        if (packet.type != EntityType.ENDER_DRAGON) return
        val spawnVec = Vec3(packet.x, packet.y, packet.z)
        val newId = packet.id

        WitherDragonEnum.entries.find { dragon ->
            spawnVec.xzInAABB(dragon.boxesDimensions) && dragon.state == WitherDragonState.SPAWNING
        }?.setAlive(newId)
    }

    fun dragonSprayed(packet: ClientboundSetEquipmentPacket) {
        if (packet.slots.none { it.second.item == Items.PACKED_ICE }) return
        val sprayedEntity = mc.level?.getEntity(packet.entity) as? ArmorStand ?: return

        WitherDragonEnum.entries.forEach { dragon ->
            if (dragon.sprayedTime != null || dragon.state != WitherDragonState.ALIVE || dragon.entity == null || sprayedEntity.distanceTo(dragon.entity) > 8) return@forEach
            dragon.sprayedTime = DungeonListener.currentTime - dragon.spawnedTime
        }
    }

    fun trackArrows(packet: ClientboundSoundPacket) {
        if (packet.sound.value() != SoundEvents.ARROW_HIT_PLAYER) return
        WitherDragons.priorityDragon.takeUnless { it == WitherDragonEnum.None }?.let {
            if (it.state == WitherDragonState.ALIVE && DungeonListener.currentTime - it.spawnedTime <= it.skipKillTime) {
                it.arrowsHit ++
            }
        }
    }
}