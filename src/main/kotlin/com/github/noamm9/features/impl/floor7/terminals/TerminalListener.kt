package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.impl.floor7.terminals.impl.MelodyTerminal
import com.github.noamm9.features.impl.floor7.terminals.impl.Terminal
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.mixin.IServerboundInteractPacket
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import gg.essential.universal.UChat
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.entity.decoration.ArmorStand
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

                    container.items.subList(0, handler.slotCount).forEachIndexed { index, stack ->
                        currentItems[index] = stack
                    }

                    handler.solve(currentItems, currentTitle, packet.slot, packet.item)

                    if (handler !is MelodyTerminal) for (slot in clickedSlots) handler.predict(slot)
                    EventBus.post(TerminalEvent.SlotUpdate(handler, packet))
                    UChat.chat("slot updated: ${packet.slot}")
                }

                is ClientboundContainerClosePacket -> if (inTerm) ThreadUtils.scheduledTask(1, ::reset)
            }
        }

        EventBus.register<PacketEvent.Sent> {
            if (LocationUtils.F7Phase != 3) return@register
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
            if (LocationUtils.F7Phase != 3) return@register
            if (interactCooldown > 0) interactCooldown --

            val click = lastClick ?: return@register
            val handler = currentHandler ?: return@register
            val container = mc.player?.containerMenu ?: return@register
            if (System.currentTimeMillis() - click < 800) return@register
            ChatUtils.debug("terminal", "TERMINAL BROKE!!!!!!!")
            
            container.items.subList(0, handler.slotCount).forEachIndexed { index, stack ->
                currentItems[index] = stack
            }
            handler.solve(currentItems, currentTitle, 0, ItemStack.EMPTY)

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

    private fun reset() {
        inTerm = false
        currentHandler = null
        currentTitle = ""
        currentItems.clear()
        clickedSlots.clear()
        lastWindowId = - 1
        lastClick = null
        Terminal.all.forEach(Terminal::reset)
        EventBus.post(TerminalEvent.Close)
    }
}