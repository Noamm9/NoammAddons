package noammaddons.features.impl.dungeons.dragons

import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.network.play.server.*
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.WitherDragonState.*
import noammaddons.features.impl.dungeons.dragons.WitherDragons.currentTick
import noammaddons.features.impl.dungeons.dragons.WitherDragons.sendSpray
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.ChatUtils.modMessage
import java.util.concurrent.CopyOnWriteArrayList

object DragonCheck {
    var lastDragonDeath: WitherDragonEnum = WitherDragonEnum.None
    val dragonEntityList = CopyOnWriteArrayList<EntityDragon>()

    fun dragonUpdate(packet: S1CPacketEntityMetadata) {
        val dragon = WitherDragonEnum.entries.find { it.entityId == packet.entityId }?.apply { if (entity == null) updateEntity(packet.entityId) } ?: return
        (packet.func_149376_c().find { it.dataValueId == 6 }?.`object` as? Float)?.let { health ->
            if (health <= 0 && dragon.state != DEAD) dragon.setDead()
        }
    }

    fun dragonSpawn(packet: S0FPacketSpawnMob) {
        if (packet.entityType != 63) return
        WitherDragonEnum.entries.find {
            isVecInXZ(Vec3(packet.x / 32.0, packet.y / 32.0, packet.z / 32.0), it.boxesDimensions) && it.state == SPAWNING
        }?.setAlive(packet.entityID)
    }

    fun dragonSprayed(packet: S04PacketEntityEquipment) {
        if (packet.itemStack?.item != Item.getItemFromBlock(Blocks.packed_ice)) return
        val sprayedEntity = mc.theWorld?.getEntityByID(packet.entityID) as? EntityArmorStand ?: return

        WitherDragonEnum.entries.forEach { dragon ->
            if (dragon.isSprayed || dragon.state != ALIVE || dragon.entity == null || sprayedEntity.getDistanceToEntity(dragon.entity) > 8) return@forEach
            if (sendSpray) modMessage("&${dragon.colorCode}${dragon.name} &fdragon was sprayed in &c${(currentTick - dragon.spawnedTime).let { "$it &ftick${if (it > 1) "s" else ""}" }}.")
            dragon.isSprayed = true
        }
    }

    private fun isVecInXZ(vec: Vec3, aabb: AxisAlignedBB) =
        vec.xCoord in aabb.minX .. aabb.maxX && vec.zCoord in aabb.minZ .. aabb.maxZ
}