package com.github.noamm9

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.world.entity.ambient.Bat
import java.awt.Color

@Suppress("unused")
class TestGround {
    private var lastServerTime = - 1L
    private var lastRealTime = - 1L

    companion object {
        val experimental get() = NoammAddons.debugFlags.contains("tick")
        val rotation get() = NoammAddons.debugFlags.contains("rotation")
        val bat get() = NoammAddons.debugFlags.contains("bat")
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
    }
}