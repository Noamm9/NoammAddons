package com.github.noamm9

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.Render3D
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.RegistryOps
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.item.ItemStack
import java.awt.Color


class TestGround {
    private var lastServerTime = - 1L
    private var lastRealTime = - 1L

    companion object {
        val experimental get() = NoammAddons.debugFlags.contains("tick")
        val rotation get() = NoammAddons.debugFlags.contains("rotation")
        val bat get() = NoammAddons.debugFlags.contains("bat")
        val slot get() = NoammAddons.debugFlags.contains("slot")
        val sound get() = NoammAddons.debugFlags.contains("sound")
    }

    init {
        EventBus.register<WorldChangeEvent> {
            if (experimental) {
                lastServerTime = - 1
                lastRealTime = - 1
            }
        }

        EventBus.register<PacketEvent.Received> {
            if (event.packet is ClientboundSetTimePacket) {
                if (! experimental) return@register
                val newServerTime = event.packet.gameTime
                val newRealTime = System.currentTimeMillis()

                if (lastServerTime == - 1L) {
                    lastServerTime = newServerTime
                    lastRealTime = newRealTime
                    return@register
                }

                val tickDiff = (newServerTime - lastServerTime).toInt()
                if (tickDiff <= 0) return@register

                val timePassed = newRealTime - lastRealTime
                val instantTickDuration = timePassed / tickDiff

                lastServerTime = newServerTime
                lastRealTime = newRealTime

                NoammAddons.scope.launch {
                    repeat(tickDiff) {
                        EventBus.post(TickEvent.Server)
                        delay(instantTickDuration)
                    }
                }
            }
        }

        EventBus.register<RenderWorldEvent> {
            if (! rotation) return@register
            DungeonScanner.clayBlocksCorners.forEachIndexed { index, (dx, dz) ->
                DungeonInfo.uniqueRooms.values.forEach { room ->
                    val centerr = BlockPos(room.mainRoom.x, room.highestBlock ?: ScanUtils.getHighestY(room.mainRoom.x, room.mainRoom.z), room.mainRoom.z)
                    Render3D.renderBlock(
                        event.ctx,
                        centerr.add(x = dx, z = dz),
                        (if (room.rotation?.div(90) == index) Color.GREEN else Color.red).withAlpha(60)
                    )

                    Render3D.renderString("$index", centerr.x + dx + 0.5, centerr.y, centerr.z + dz + 0.5, phase = true, scale = 3)
                }
            }
        }

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            if (! bat) return@register
            if (event.packet is ClientboundAddEntityPacket) {
                val bat = mc.level?.getEntity(event.packet.id) as? Bat ?: return@register
                val room = ScanUtils.getRoomFromPos(bat.position()) ?: return@register
                ThreadUtils.scheduledTask(5) {
                    ChatUtils.modMessage("bat hp: ${bat.maxHealth}. (${room.name})")
                }
            }
        }

        EventBus.register<ContainerEvent.SlotClick> {
            if (! slot) return@register
            val stack = event.screen.menu.getSlot(event.slotId).item
            ChatUtils.modMessage("skyblockid: " + stack.skyblockId)
            ChatUtils.modMessage("index: " + event.slotId)
            mc.keyboardHandler.clipboard = getNBT(stack)
        }

        EventBus.register<MainThreadPacketReceivedEvent.Pre> {
            if (! sound) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            val name = packet.sound.value().location
            val pitch = packet.pitch
            val volume = packet.volume
            ChatUtils.modMessage("name: $name, pitch: $pitch, volume: $volume")
        }
    }

    fun getNBT(itemStack: ItemStack?): String {
        if (itemStack == null || itemStack.isEmpty) return "{}"
        val ops = RegistryOps.create<JsonElement>(JsonOps.INSTANCE, mc.connection?.registryAccess() !!)
        val jsonElement = DataComponentPatch.CODEC.encodeStart(ops, itemStack.componentsPatch).result().get()
        return GsonUtils.gson.toJson(jsonElement)
    }
}

/*
{
  "minecraft:item_model": "hypixel_skyblock:item/slayer/enderman/weapons/terminator",
  "minecraft:tooltip_style": "hypixel_skyblock:mythic"
  "minecraft:custom_data": {
    "upgrade_level": 10,
    "enchantments": {
      "cubism": 5,
      "aiming": 5,
      "toxophilite": 10,
      "impaling": 5,
      "piercing": 1,
      "snipe": 4,
      "infinite_quiver": 10,
      "chance": 3,
      "power": 7,
      "dragon_hunter": 6,
      "flame": 2,
      "overload": 5,
      "ultimate_reiterate": 5
    },
    "timestamp": 1762120237866,
    "hot_potato_count": 15,
    "runes": {
      "GOLDEN": 3
    },
    "modifier": "spiritual",
    "rarity_upgrades": 1,
    "toxophilite_combat_xp": 2.1954964312038323E8,
    "uuid": "3c934dea-1ec1-4109-a71b-f47958bc578c",
    "id": "TERMINATOR",
    "art_of_war_count": 1,
    "dungeon_item": 1
  }
}
 */