package com.github.noamm9

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils
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
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.world.entity.ambient.Bat
import java.awt.Color

class TestGround {
    private var lastServerTime = - 1L
    private var lastRealTime = - 1L

    companion object {
        val experimental get() = NoammAddons.debugFlags.contains("tick")
        val rotation get() = NoammAddons.debugFlags.contains("rotation")
        val norotate get() = NoammAddons.debugFlags.contains("norotate")
        val bat get() = NoammAddons.debugFlags.contains("bat")
    }

    private var oldRot = MathUtils.Rotation(0f, 0f)

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


        EventBus.register<MainThreadPacketReceivedEvent.Pre> {
            if (! norotate) return@register
            if (event.packet is ClientboundPlayerPositionPacket) {
                oldRot.yaw = mc.player !!.yRot
                oldRot.pitch = mc.player !!.xRot
            }
        }

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            if (! norotate) return@register
            if (event.packet is ClientboundPlayerPositionPacket) {
                mc.player !!.yRot = oldRot.yaw
                mc.player !!.xRot = oldRot.pitch
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
/*
    fun onPacket(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is ClientboundPingPacket -> {
                if (lastPingParameter == packet.id) return
                lastPingParameter = packet.id

                totalServerTicks++
                ServerTickEvent.post()
            }
        }
    }

    private var lastPingParameter = 0
        var totalServerTicks: Long = 0L
        private set
 */

/*
object PathWalker {
    private var path: List<BlockPos> = emptyList()
    private var currentIndex = 0
    var active = false

    private const val REACH_THRESHOLD = 0.3
    private const val WALK_SPEED = 0.28

    fun start(newPath: List<BlockPos>) {
        if (newPath.isEmpty()) return
        path = newPath
        currentIndex = 0
        active = true
        ChatUtils.modMessage("§aStarted walking path with ${path.size} nodes.")
    }

    fun stop() {
        active = false
        path = emptyList()
        stopMotion()
        ChatUtils.modMessage("§cPath walking stopped.")
    }

    init {
        EventBus.register<TickEvent.Start> {
            if (! active || mc.player == null) return@register
            if (currentIndex >= path.size) return@register stop()

            val player = mc.player !!
            val targetPos = path[currentIndex]

            val targetVec = Vec3(targetPos.x + 0.5, player.y, targetPos.z + 0.5)

            val dx = targetVec.x - player.x
            val dz = targetVec.z - player.z
            val dist = sqrt(dx * dx + dz * dz)

            if (dist < REACH_THRESHOLD) {
                currentIndex ++
                if (currentIndex >= path.size) stop()
                return@register
            }

            if (dist > 0.0001) {
                val speedFactor = WALK_SPEED / dist
                val motionX = dx * speedFactor
                val motionZ = dz * speedFactor
                val currentY = player.deltaMovement.y

                player.deltaMovement = Vec3(motionX, currentY, motionZ)
            }
        }

        EventBus.register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> {
            PathWalker.start(MathUtils.getAllBlocksBetween(BlockPos(61, 83, - 148), event.pos))
        }
    }

    private fun stopMotion() {
        val player = mc.player ?: return
        player.deltaMovement = Vec3(0.0, player.deltaMovement.y, 0.0)
    }
}*/