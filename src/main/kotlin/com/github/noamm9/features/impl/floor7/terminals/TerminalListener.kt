package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
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
    const val FIRST_CLICK_DELAY = 7

    @JvmField var inTerm = false
    var currentType: TerminalType? = null
    var currentTitle = ""
    var initialOpenTick = 0L
    var initialOpenTime = 0L

    var lastWindowId = - 1

    var interactCooldown = 0

    val currentItems = mutableMapOf<Int, ItemStack>()

    val packetReceivedListener = EventBus.listener<MainThreadPacketReceivedEvent.Post> { onPacketReceived(event.packet) }
    val packetSentListener = EventBus.listener<PacketEvent.Sent> { onPacketSent(event.packet, event) }
    val tickListener = EventBus.listener<TickEvent.Server> { onTick() }
    val worldChangeListener = EventBus.listener<WorldChangeEvent> { reset() }

    private fun onPacketReceived(packet: Packet<*>) {
        if (LocationUtils.F7Phase != 3) return
        when (packet) {
            is ClientboundOpenScreenPacket -> {
                val title = packet.title.string
                val type = TerminalType.fromName(title)
                if (type != null) {
                    if (! inTerm) {
                        initialOpenTick = DungeonListener.currentTime
                        initialOpenTime = System.currentTimeMillis()
                    }
                    inTerm = true
                    currentType = type
                    currentTitle = title
                    lastWindowId = packet.containerId
                    currentItems.clear()

                    TerminalSolver.onTerminalOpen()
                    //#if CHEAT
                    AutoTerminal.reset()
                    //#endif
                }
                else reset()
            }

            is ClientboundContainerSetSlotPacket -> {
                if (! inTerm || packet.containerId != lastWindowId) return
                val type = currentType ?: return
                if (packet.slot !in 0 until type.slotCount) return
                val container = mc.player?.containerMenu ?: return
                container.items.forEachIndexed { index, stack ->
                    if (index !in 0 until type.slotCount) return@forEachIndexed
                    if (stack.isEmpty) return@forEachIndexed
                    currentItems[index] = stack
                }

                if (packet.slot == type.slotCount - 1 || type == TerminalType.MELODY) {
                    TerminalSolver.onItemsUpdated(packet.slot, packet.item)
                    //#if CHEAT
                    if (AutoTerminal.enabled) AutoTerminal.onItemsUpdated()
                    //#endif
                }
            }

            is ClientboundContainerClosePacket -> if (inTerm) ThreadUtils.scheduledTask(1, ::reset)
        }
    }

    private fun onPacketSent(packet: Packet<*>, event: PacketEvent.Sent) {
        if (LocationUtils.F7Phase != 3) return
        when (packet) {
            is ServerboundContainerClickPacket -> {
                if (! inTerm) return
                val isMelody = currentType == TerminalType.MELODY

                if ((checkFcDelay() && ! isMelody) || packet.containerId != lastWindowId) {
                    event.isCanceled = true
                }
            }

            is ServerboundContainerClosePacket -> if (inTerm) reset()

            is IServerboundInteractPacket -> {
                val entity = mc.level?.getEntity(packet.entityId) as? ArmorStand ?: return
                if (entity.displayName.unformattedText != "Inactive Terminal") return

                if (interactCooldown > 0 || lastWindowId != - 1) event.isCanceled = true else interactCooldown = 15
            }
        }
    }

    private fun onTick() {
        if (LocationUtils.F7Phase != 3) return
        if (interactCooldown > 0) interactCooldown --
    }

    fun checkFcDelay(): Boolean {
        var delay = FIRST_CLICK_DELAY
        if (currentType == TerminalType.MELODY) delay += 3

        return DungeonListener.currentTime - initialOpenTick < delay ||
            System.currentTimeMillis() - initialOpenTime < (delay * 50)
    }

    private fun reset() {
        inTerm = false
        currentType = null
        currentTitle = ""
        currentItems.clear()
        lastWindowId = - 1
        HumanClickOrder.lastClickedSlot = null
        TerminalSolver.onTerminalClose()
        //#if CHEAT
        AutoTerminal.reset()
        //#endif
        TerminalType.reset()
    }
}