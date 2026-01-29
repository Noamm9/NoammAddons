package com.github.noamm9.features.impl.dungeon.solvers.terminals

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.mixin.IServerboundInteractPacket
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack


object TerminalListener {
    var inTerm = false
    var currentType: TerminalType? = null
    var currentTitle = ""
    var initialOpen = 0L
    var lastWindowId = - 1

    private var interactCooldown = 0

    val currentItems = mutableMapOf<Int, ItemStack>()

    val packetRecivedListener = register<MainThreadPacketReceivedEvent.Pre> { onPacketReceived(event.packet) }.unregister()
    val packetSentListener = register<PacketEvent.Sent> { onPacketSent(event.packet, event) }.unregister()
    val tickListener = register<TickEvent.Server> { onTick() }.unregister()

    fun onPacketReceived(packet: Packet<*>) {
        if (LocationUtils.F7Phase != 3) return
        when (packet) {
            is ClientboundOpenScreenPacket -> {
                val title = packet.title.string
                val type = TerminalType.fromName(title)
                if (type != null) {
                    if (! inTerm) initialOpen = DungeonListener.currentTime
                    inTerm = true
                    currentType = type
                    currentTitle = title
                    lastWindowId = packet.containerId
                    currentItems.clear()
                    TerminalSolver.onTerminalOpen()
                }
                else reset()
            }

            is ClientboundContainerSetSlotPacket -> {
                if (! inTerm || packet.containerId != lastWindowId) return
                if (packet.slot > currentType !!.slotCount) return
                if (packet.slot < 0) return
                currentItems[packet.slot] = packet.item

                if (currentItems.size == currentType?.slotCount || currentType == TerminalType.MELODY) {
                    TerminalSolver.onItemsUpdated(packet.slot, packet.item)
                }
            }

            is ClientboundContainerSetContentPacket -> {
                if (! inTerm || packet.containerId != lastWindowId) return
                currentItems.clear()

                packet.items.forEachIndexed { index, itemStack ->
                    if (index < (currentType?.slotCount ?: 0)) {
                        currentItems[index] = itemStack
                    }
                }
            }

            // we use scheduled task to stop the real gui from drawing until the packet is prossed on the main thread
            is ClientboundContainerClosePacket -> if (inTerm) ThreadUtils.scheduledTask(1, ::reset)
        }
    }

    private fun onPacketSent(packet: Packet<*>, event: PacketEvent.Sent) {
        if (LocationUtils.F7Phase != 3) return
        when (packet) {
            is ServerboundContainerClickPacket -> {
                if (! inTerm) return

                if (DungeonListener.currentTime - initialOpen < TerminalSolver.FIRST_CLICK_DELAY || packet.containerId != lastWindowId) {
                    event.isCanceled = true
                }
            }

            is ServerboundContainerClosePacket -> if (inTerm) reset()

            is ServerboundInteractPacket -> {
                val entity = mc.level?.getEntity((packet as IServerboundInteractPacket).entityId) as? ArmorStand ?: return
                if (entity.displayName?.unformattedText != "Inactive Terminal") return

                if (interactCooldown > 0 || lastWindowId != - 1) event.isCanceled = true else interactCooldown = 15
            }
        }
    }

    private fun onTick() {
        if (LocationUtils.F7Phase != 3) return
        if (interactCooldown > 0) interactCooldown --
    }

    private fun reset() {
        inTerm = false
        currentType = null
        currentTitle = ""
        currentItems.clear()
        lastWindowId = - 1
        HumanClickOrder.lastClickedSlot = null
        TerminalSolver.onTerminalClose()
    }
}