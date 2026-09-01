package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.impl.floor7.terminals.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.mixin.IServerboundInteractPacket
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object TerminalListener: ISelfInit {
    const val FIRST_CLICK_DELAY = 7
    var interactCooldown = 0

    @JvmField var inTerm = false
    var currentHandler: Terminal? = null
    var initialOpenTick = 0L
    var initialOpenTime = 0L

    var currentTitle = ""
    var lastWindowId = - 1
    var currentItems = mutableMapOf<Int, ItemStack>()

    val clickedSlots = mutableListOf<TerminalClick>()
    var lastClick: Long? = null

    override fun init() {
        EventBus.register<ContainerFullyOpenedEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            val handler = Terminal.fromTitle(event.title.string) ?: return@register reset()
            inTerm = true
            initialOpenTick = DungeonListener.currentTime
            initialOpenTime = System.currentTimeMillis()
            currentTitle = event.title.string
            lastWindowId = event.windowId
            currentItems.clear()
            currentItems.putAll(event.items)
            clickedSlots.clear()
            currentHandler = handler

            if (handler is MelodyTerminal) currentItems.forEach { (slot, stack) ->
                handler.solve(currentItems, currentTitle, slot, stack)
            }
            else handler.solve(currentItems, currentTitle, 0, ItemStack.EMPTY)

            EventBus.post(TerminalEvent.Open(handler))
        }

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            when (val packet = event.packet) {
                is ClientboundContainerSetSlotPacket -> {
                    if (! inTerm || packet.containerId != lastWindowId) return@register
                    val container = mc.player?.containerMenu ?: return@register
                    val handler = currentHandler ?: return@register
                    if (packet.slot !in 0 until handler.slotCount) return@register
                    if (packet.item.`is`(Items.BLACK_STAINED_GLASS_PANE)) return@register
                    if (handler is RubixTerminal) return@register

                    handler.sync(container, packet.slot, packet.item)

                    if (handler !is MelodyTerminal) for (slot in clickedSlots) handler.predict(slot)
                    EventBus.post(TerminalEvent.SlotUpdate(handler, packet))
                    ChatUtils.debug("terminal", "slot updated: ${packet.slot}")
                }

                is ClientboundContainerClosePacket -> if (inTerm) ThreadUtils.scheduledTask(1, ::reset)
            }
        }

        EventBus.register<PacketEvent.Sent> {
            when (event.packet) {
                is ServerboundContainerClickPacket -> {
                    if (! inTerm) return@register
                    val isMelody = currentHandler is MelodyTerminal
                    if ((checkFcDelay() && ! isMelody)) {
                        event.isCanceled = true
                        return@register
                    }

                    if (isMelody) return@register
                    lastClick = System.currentTimeMillis()
                }

                is ServerboundContainerClosePacket -> if (inTerm) reset()

                is IServerboundInteractPacket -> {
                    val entity = mc.level?.getEntity(event.packet.entityId) as? ArmorStand ?: return@register
                    if (entity.displayName.unformattedText != "Inactive Terminal") return@register

                    if (interactCooldown > 0 || lastWindowId != - 1) event.isCanceled = true else interactCooldown = 15
                }
            }
        }

        EventBus.register<TickEvent.Server> {
            if (interactCooldown > 0) interactCooldown --

            val click = lastClick ?: return@register
            val handler = currentHandler ?: return@register
            val container = mc.player?.containerMenu ?: return@register
            if (System.currentTimeMillis() - click < TerminalSolver.breakTimeout.value) return@register
            ChatUtils.debug("terminal", "TERMINAL BROKE!!!!!!!")

            handler.sync(container)

            clickedSlots.clear()
            lastClick = System.currentTimeMillis()
        }

        EventBus.register<WorldChangeEvent> { reset() }
    }

    fun checkFcDelay(): Boolean {
        var delay = FIRST_CLICK_DELAY
        if (currentHandler is MelodyTerminal) delay += 3

        return DungeonListener.currentTime - initialOpenTick < delay ||
            System.currentTimeMillis() - initialOpenTime < (delay * 50)
    }

    private fun Terminal.sync(container: AbstractContainerMenu, slotId: Int = 0, stack: ItemStack = ItemStack.EMPTY) {
        container.items.toList().subList(0, slotCount).forEachIndexed { index, stack ->
            currentItems[index] = stack
        }

        solve(currentItems, currentTitle, slotId, stack)

        EventBus.post(TerminalEvent.Break(this))
    }

    private fun reset() {
        inTerm = false
        currentHandler = null
        currentTitle = ""
        currentItems.clear()
        clickedSlots.clear()
        lastWindowId = - 1
        lastClick = null
        initialOpenTick = 0
        initialOpenTime = 0

        Terminal.all.forEach(Terminal::reset)
        EventBus.post(TerminalEvent.Close)
    }
}