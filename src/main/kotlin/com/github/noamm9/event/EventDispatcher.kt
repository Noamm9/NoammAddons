package com.github.noamm9.event

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.dungeons.DungeonUtils.isSecret
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.RenderContext
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.SkullBlockEntity

object EventDispatcher: ISelfInit {
    private var invTitle: Component? = null
    private var invWindowId: Int? = null
    private var invSlotCount: Int? = 0
    private var invItems: MutableMap<Int, ItemStack>? = null

    override fun init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { context ->
            EventBus.post(RenderWorldEvent(RenderContext.fromContext(context)))
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ -> EventBus.post(WorldChangeEvent) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> EventBus.post(WorldChangeEvent) }

        ClientTickEvents.START_CLIENT_TICK.register { mc -> mc.level?.let { EventBus.post(TickEvent.Start) } }
        ClientTickEvents.END_CLIENT_TICK.register { mc -> mc.level?.let { EventBus.post(TickEvent.End) } }

        ClientLifecycleEvents.CLIENT_STARTED.register { EventBus.post(GameStartEvent) }
        ClientLifecycleEvents.CLIENT_STOPPING.register { EventBus.post(ShutdownEvent) }

        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            EventBus.post(EntityUnloadEvent(entity))

            // for items that are in the personal deletor
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            val entity = entity as? ItemEntity ?: return@register
            if (entity.item.hoverName.unformattedText !in DungeonUtils.dungeonItemDrops) return@register
            if (mc.player !!.distanceTo(entity) > 6) return@register

            EventBus.post(DungeonEvent.SecretEvent(SecretType.ITEM, entity.blockPosition()))
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            when (val packet = event.packet) {
                is ClientboundSystemChatPacket -> {
                    if (packet.overlay) return@register
                    if (EventBus.post(ChatMessageEvent(packet.content))) {
                        event.isCanceled = true
                    }
                }

                is ClientboundSoundPacket -> {
                    if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
                    if (packet.sound.value() != SoundEvents.BAT_DEATH) return@register

                    EventBus.post(DungeonEvent.SecretEvent(
                        SecretType.BAT, BlockPos(packet.x.toInt(), packet.y.toInt(), packet.z.toInt())
                    ))
                }

                is ClientboundTakeItemEntityPacket -> {
                    if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
                    val entity = mc.level?.getEntity(packet.itemId) as? ItemEntity ?: return@register
                    if (entity.item.hoverName.unformattedText !in DungeonUtils.dungeonItemDrops) return@register
                    if (mc.player !!.distanceTo(entity) > 6) return@register

                    EventBus.post(DungeonEvent.SecretEvent(SecretType.ITEM, entity.blockPosition()))
                }
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            when (val packet = event.packet) {
                is ClientboundOpenScreenPacket -> {
                    resetInventory()

                    invWindowId = packet.containerId
                    invTitle = packet.title
                    invItems = mutableMapOf()
                    invSlotCount = when (packet.type) {
                        MenuType.GENERIC_9x1 -> 9
                        MenuType.GENERIC_9x2 -> 18
                        MenuType.GENERIC_9x3 -> 27
                        MenuType.GENERIC_9x4 -> 36
                        MenuType.GENERIC_9x5 -> 45
                        MenuType.GENERIC_9x6 -> 54
                        MenuType.GENERIC_3x3 -> 9
                        else -> return@register
                    }
                }

                is ClientboundContainerSetContentPacket -> {
                    if (packet.containerId != invWindowId) return@register
                    val slotCount = invSlotCount ?: return@register
                    val con = mc.player?.containerMenu ?: return@register

                    for (i in packet.items.indices) {
                        if (i !in 0 until slotCount) continue
                        val item = con.items.getOrNull(i) ?: continue
                        invItems?.set(i, item)
                    }

                    checkInvAndPost(packet.items.lastIndex)
                }

                is ClientboundContainerSetSlotPacket -> {
                    if (packet.containerId != invWindowId) return@register
                    val slotCount = invSlotCount ?: return@register
                    if (packet.slot !in 0 until slotCount) return@register
                    val con = mc.player?.containerMenu ?: return@register
                    val item = con.items.getOrNull(packet.slot) ?: return@register

                    invItems?.set(packet.slot, item)
                    checkInvAndPost(packet.slot)
                }

                is ClientboundContainerClosePacket -> {
                    if (packet.containerId == invWindowId) resetInventory()
                }
            }
        }

        register<PacketEvent.Sent> {
            when (val packet = event.packet) {
                is ServerboundUseItemOnPacket -> {
                    if (! LocationUtils.inDungeon) return@register
                    val pos = packet.hitResult.blockPos
                    if (! isSecret(pos)) return@register
                    val block = WorldUtils.getBlockAt(pos)

                    val type = when (block) {
                        Blocks.CHEST, Blocks.TRAPPED_CHEST -> SecretType.CHEST
                        Blocks.LEVER -> SecretType.LEVER
                        Blocks.PLAYER_HEAD -> {
                            when ((mc.level?.getBlockEntity(pos) as? SkullBlockEntity)?.ownerProfile?.partialProfile()?.id.toString()) {
                                in DungeonUtils.WITHER_ESSENCE -> SecretType.WITHER_ESSENCE
                                in DungeonUtils.REDSTONE_KEY -> SecretType.REDSTONE_KEY
                                else -> return@register
                            }
                        }

                        else -> return@register
                    }

                    EventBus.post(DungeonEvent.SecretEvent(type, pos))
                }
            }
        }
    }

    private fun resetInventory() {
        invWindowId = null
        invTitle = null
        invSlotCount = null
        invItems = null
    }

    private fun checkInvAndPost(lastIndex: Int) {
        val title = invTitle ?: return
        val winId = invWindowId ?: return
        val slotCount = invSlotCount ?: return
        val items = invItems?.let(::HashMap) ?: return
        if (lastIndex != slotCount - 1) return

        EventBus.post(ContainerFullyOpenedEvent(title, winId, slotCount, items))
        resetInventory()
    }
}