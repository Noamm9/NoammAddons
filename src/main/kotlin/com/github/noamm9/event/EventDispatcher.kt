package com.github.noamm9.event

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.dungeons.DungeonUtils.isSecret
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.RenderContext
import com.github.noamm9.utils.world.WorldUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks

object EventDispatcher {
    fun init() {
        WorldRenderEvents.END_MAIN.register { context ->
            EventBus.post(RenderWorldEvent(RenderContext.fromContext(context)))
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            EventBus.post(ServerEvent.Connect)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            EventBus.post(ServerEvent.Disconnect)
        }

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ ->
            EventBus.post(WorldChangeEvent)
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            mc.level?.let { EventBus.post(TickEvent.Start) }
        }

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            mc.level?.let { EventBus.post(TickEvent.End) }
        }

        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            EventBus.post(EntityDeathEvent(entity))
        }

        register<PacketEvent.Received> {
            if (event.packet is ClientboundPingPacket) {
                if (event.packet.id != 0) {
                    EventBus.post(TickEvent.Server)
                }
            }
            else if (event.packet is ClientboundSystemChatPacket) {
                if (EventBus.post(ChatMessageEvent(event.packet.content))) {
                    event.isCanceled = true
                }
            }
            else if (event.packet is ClientboundSoundPacket) {
                if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
                if (! event.packet.sound.value().equalsOneOf(SoundEvents.BAT_HURT, SoundEvents.BAT_DEATH)) return@register

                EventBus.post(DungeonEvent.SecretEvent(
                    DungeonEvent.SecretEvent.SecretType.BAT,
                    BlockPos(event.packet.x.toInt(), event.packet.y.toInt(), event.packet.z.toInt())
                ))
            }
            else if (event.packet is ClientboundTakeItemEntityPacket) {
                if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
                val entity = mc.level?.getEntity(event.packet.itemId) as? ItemEntity ?: return@register
                if (entity.item.hoverName.unformattedText !in DungeonUtils.dungeonItemDrops) return@register
                if (mc.player !!.distanceTo(entity) > 6) return@register

                EventBus.post(
                    DungeonEvent.SecretEvent(DungeonEvent.SecretEvent.SecretType.ITEM, entity.blockPosition())
                )
            }
        }

        register<PacketEvent.Sent> {
            if (event.packet is ServerboundUseItemOnPacket) {
                if (! LocationUtils.inDungeon) return@register
                if (LocationUtils.inBoss) return@register
                val pos = event.packet.hitResult.blockPos
                if (! isSecret(pos)) return@register
                val block = WorldUtils.getBlockAt(pos)

                val type = when (block) {
                    Blocks.CHEST, Blocks.TRAPPED_CHEST -> DungeonEvent.SecretEvent.SecretType.CHEST
                    Blocks.LEVER -> DungeonEvent.SecretEvent.SecretType.LEVER
                    Blocks.PLAYER_HEAD, Blocks.SKELETON_SKULL, Blocks.WITHER_SKELETON_SKULL -> DungeonEvent.SecretEvent.SecretType.SKULL
                    else -> return@register
                }

                EventBus.post(DungeonEvent.SecretEvent(type, pos))
            }
        }
    }

    fun checkForRoomChange(currentRoom: UniqueRoom?, lastKnownRoom: UniqueRoom?) {
        lastKnownRoom?.let {
            EventBus.post(DungeonEvent.RoomEvent.onExit(it))
        }
        currentRoom?.let {
            it.highestBlock = ScanUtils.getHighestY(it.mainRoom.x, it.mainRoom.z)
            it.findRotation()
            EventBus.post(DungeonEvent.RoomEvent.onEnter(it))
        }
    }
}