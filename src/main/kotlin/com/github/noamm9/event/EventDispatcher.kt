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
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.*
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks

object EventDispatcher {
    private var invWindowId: Int = - 1
    private var invTitle: Component? = null
    private var invSlotCount: Int = 0
    private val invItems = mutableMapOf<Int, ItemStack>()
    private var invAccept = false
    private var invFired = false


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
            else if (event.packet is ClientboundContainerClosePacket) {
                if (event.packet.containerId == invWindowId) resetInventoryState()
            }
            else if (event.packet is ClientboundOpenScreenPacket) {
                resetInventoryState()
                invAccept = true
                invWindowId = event.packet.containerId
                invTitle = event.packet.title
                invSlotCount = getSlotCount(event.packet.type)
            }
            else if (event.packet is ClientboundContainerSetContentPacket) {
                if (event.packet.containerId == invWindowId) {
                    event.packet.items.forEachIndexed { index, stack ->
                        if (index < invSlotCount && ! stack.isEmpty) {
                            invItems[index] = stack
                        }
                    }
                    finishInventoryLoading()
                }
            }
            else if (event.packet is ClientboundContainerSetSlotPacket) {
                if (invAccept && event.packet.containerId == invWindowId) {
                    val slot = event.packet.slot
                    if (slot < invSlotCount) {
                        if (! event.packet.item.isEmpty) invItems[slot] = event.packet.item
                    }
                    else finishInventoryLoading()

                    if (invItems.size >= invSlotCount) finishInventoryLoading()
                }
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


    private fun resetInventoryState() {
        invWindowId = - 1
        invTitle = null
        invSlotCount = 0
        invItems.clear()
        invAccept = false
        invFired = false
    }

    private fun finishInventoryLoading() {
        if (! invAccept) return
        invAccept = false

        // Capture state
        val winId = invWindowId
        val slotCount = invSlotCount
        val title = invTitle ?: return
        val items = HashMap(invItems)

        if (invFired) return
        if (invWindowId != winId) return

        invFired = true
        EventBus.post(InventoryFullyOpenedEvent(title, winId, slotCount, items))
    }

    private fun getSlotCount(type: MenuType<*>): Int {
        return when (type) {
            MenuType.GENERIC_9x1 -> 9
            MenuType.GENERIC_9x2 -> 18
            MenuType.GENERIC_9x3 -> 27
            MenuType.GENERIC_9x4 -> 36
            MenuType.GENERIC_9x5 -> 45
            MenuType.GENERIC_9x6 -> 54
            MenuType.GENERIC_3x3 -> 9
            MenuType.HOPPER -> 5
            else -> 0
        }
    }
}