package com.github.noamm9.debug

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import com.github.noamm9.utils.render.world.Render3D.renderString
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.SkullBlockEntity
import java.awt.Color

object TestGround: ISelfInit {
    private var lastServerTime = - 1L
    private var lastRealTime = - 1L

    val tick get() = NoammAddons.debugFlags.contains("tick")
    val rotation get() = NoammAddons.debugFlags.contains("rotation")
    val slot get() = NoammAddons.debugFlags.contains("slot")
    val sound get() = NoammAddons.debugFlags.contains("sound")

    override fun init() {
        EventBus.register<WorldChangeEvent> {
            if (tick) {
                lastServerTime = - 1
                lastRealTime = - 1
            }
        }

        EventBus.register<PacketEvent.Received> {
            if (event.packet is ClientboundSetTimePacket) {
                if (! tick) return@register
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
            DungeonScanner.uniqueRooms.values.forEach { room ->
                val highest = room.highestBlock ?: ScanUtils.getHighestY(room.mainRoom.x, room.mainRoom.z)
                val center = room.centerPos.above(highest)
                event.ctx.renderString(room.name, center.above(3).toVec(), Color.YELLOW, 6, phase = true)
                DungeonScanner.clayBlocksCorners.forEachIndexed { index, (dx, dz) ->
                    event.ctx.renderBlock(
                        center.add(x = dx, z = dz),
                        (if (room.rotation?.div(90) == index) Color.GREEN else Color.red).withAlpha(60)
                    )

                    event.ctx.renderString("$index", center.x + dx + 0.5, center.y, center.z + dz + 0.5, scale = 3, phase = true)
                }
            }
        }

        EventBus.register<ContainerEvent.SlotClick> {
            if (! slot) return@register
            val stack = event.screen.menu.getSlot(event.slotId).item
            ChatUtils.modMessage("skyblockid: " + stack.skyblockId)
            ChatUtils.modMessage("index: " + event.slotId)
            NoammAddons.mc.keyboardHandler.clipboard = getNBT(stack)
        }

        EventBus.register<MainThreadPacketReceivedEvent.Pre> {
            if (! sound) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            val name = packet.sound.value().location
            val pitch = packet.pitch
            val volume = packet.volume
            ChatUtils.modMessage("name: $name, pitch: $pitch, volume: $volume")
        }

        EventBus.register<NoammDebugFlagEvent.Add> {
            if (event.flag != "skull") return@register
            event.cancel()

            val pos = PlayerUtils.getSelectionBlock() !!
            (NoammAddons.mc.level?.getBlockEntity(pos) as? SkullBlockEntity)?.ownerProfile?.partialProfile()?.id.toString().let {
                ChatUtils.modMessage("skullid: $it")
            }
        }
    }

    fun getNBT(itemStack: ItemStack?): String {
        if (itemStack == null || itemStack.isEmpty) return "{}"
        val ops = RegistryOps.create<JsonElement>(JsonOps.INSTANCE, NoammAddons.mc.connection?.registryAccess() !!)
        val jsonElement = DataComponentPatch.CODEC.encodeStart(ops, itemStack.componentsPatch).result().get()
        return GsonUtils.gson.toJson(jsonElement)
    }
}