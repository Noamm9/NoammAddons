package noammaddons.features.impl.dungeons.dragons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.server.*
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.WitherDragonState.*
import noammaddons.features.impl.dungeons.dragons.WitherDragons.currentTick
import noammaddons.features.impl.dungeons.dragons.WitherDragons.priorityDragon
import noammaddons.utils.MathUtils.Vec3
import noammaddons.utils.MathUtils.multiply
import noammaddons.utils.MathUtils.xzInAABB
import kotlin.math.floor

object DragonCheck {
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
            dragon.awaitingRespawn && dragon.lastPosition != null && dragon.lastPosition !!.squareDistanceTo(spawnVec) < 100
        }?.let { return it.updateEntity(newId, true) }

        WitherDragonEnum.entries.find { dragon ->
            spawnVec.xzInAABB(dragon.boxesDimensions) && dragon.state == SPAWNING
        }?.setAlive(newId)
    }

    fun dragonUnload(packet: S13PacketDestroyEntities) {
        val destroyedIDs = packet.entityIDs

        destroyedIDs.forEach { destroyedId ->
            val dragon = WitherDragonEnum.entries.find { it.entityId == destroyedId && it.state == ALIVE } ?: return@forEach
            val entity = mc.theWorld?.getEntityByID(dragon.entityId !!)
            if (entity != null) {
                dragon.lastPosition = entity.positionVector
                val chunkX = floor(entity.posX / 16).toInt()
                val chunkZ = floor(entity.posZ / 16).toInt()
                dragon.lastChunk = Pair(chunkX, chunkZ)
            }
            dragon.awaitingRespawn = true
            dragon.state = DEAD
        }
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
        WitherDragonEnum.entries.forEach { dragon ->
            if (dragon.state != ALIVE || currentTick - dragon.spawnedTime >= dragon.skipKillTime || dragon != priorityDragon) return@forEach
            dragon.arrowsHit ++
        }
    }
}