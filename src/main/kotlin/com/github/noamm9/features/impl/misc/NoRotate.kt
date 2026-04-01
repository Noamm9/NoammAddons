package com.github.noamm9.features.impl.misc

//#if CHEAT

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver.resyncTimeout
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ActionBarParser
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.network.PacketUtils.send
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object NoRotate: Feature("Prevents the server from snapping back your head when teleporting.") {
    private val tpItems by MultiCheckboxSetting("Teleport Items", mutableMapOf(
        Pair("Etherwarp", false),
        Pair("Instant Transmission", false),
        Pair("Wither Impact", false)
    ))

    private val pendingTeleports = ArrayDeque<Long>()

    override fun init() {
        register<WorldChangeEvent> { pendingTeleports.clear() }

        register<PacketEvent.Sent> {
            val packet = event.packet as? ServerboundUseItemPacket ?: return@register
            if (LocationUtils.world.equalsOneOf(WorldType.Home, WorldType.Garden)) return@register
            if (LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss) return@register
            if (ActionBarParser.currentMana < ActionBarParser.maxMana * 0.1) return@register
            if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return@register
            if (mc.player !!.isPassenger) return@register
            if (! isTeleport(mc.player !!.getItemInHand(packet.hand))) return@register
            val now = System.currentTimeMillis()

            pendingTeleports.add(now)
            ThreadUtils.setTimeout(resyncTimeout.value) { pendingTeleports.remove(now) }
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            val packet = event.packet as? ClientboundPlayerPositionPacket ?: return@register
            if (pendingTeleports.isEmpty()) return@register
            val tp = pendingTeleports.removeFirst()
            val now = System.currentTimeMillis()
            val ping = ServerUtils.averagePing

            if (now - tp < ping * 1.5) {
                val player = mc.player ?: return@register

                val old = PositionMoveRotation.of(player)
                val new = PositionMoveRotation.calculateAbsolute(old, packet.change, packet.relatives)

                player.setPos(new.position())
                player.deltaMovement = new.deltaMovement()

                val newOldPos = PositionMoveRotation.calculateAbsolute(
                    PositionMoveRotation(player.oldPosition(), Vec3.ZERO, player.yRotO, player.xRotO), packet.change(), packet.relatives()
                )

                player.xo = newOldPos.position().x.also { player.xOld = it }
                player.yo = newOldPos.position().y.also { player.yOld = it }
                player.zo = newOldPos.position().z.also { player.zOld = it }

                ServerboundAcceptTeleportationPacket(packet.id).send()
                ServerboundMovePlayerPacket.PosRot(player.x, player.y, player.z, new.yRot, new.xRot, false, false).send()

                (player as ILocalPlayer).setLastYaw(new.yRot)
                (player as ILocalPlayer).setLastPitch(new.xRot)

                event.isCanceled = true
            }
            else pendingTeleports.clear()
        }
    }

    private fun isTeleport(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty) return false
        val player = mc.player as ILocalPlayer
        val sbId = stack.skyblockId
        val nbt = stack.customData

        if (sbId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            return if (tpItems.value["Etherwarp"] !! && player.isSneakingServer && nbt.getByte("ethermerge").orElse(0) == 1.toByte()) true
            else if (tpItems.value["Instant Transmission"] !!) true else false
        }
        else if (tpItems.value["Wither Impact"] !! && nbt.getList("ability_scroll").toString().run {
                contains("SHADOW_WARP_SCROLL") && contains("IMPLOSION_SCROLL") && contains("WITHER_SHIELD_SCROLL")
            }) {
            return true
        }

        return false
    }
}
//#endif