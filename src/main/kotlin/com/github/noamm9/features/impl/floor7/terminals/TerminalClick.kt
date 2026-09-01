package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.Utils.send
import com.google.common.primitives.Shorts
import com.google.common.primitives.SignedBytes
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack

data class TerminalClick(val slotId: Int, val btn: Int = 0) {
    fun send() {
        val connection = mc.connection ?: return
        val stateId = mc.player?.containerMenu?.stateId ?: return
        val windowId = TerminalListener.lastWindowId.takeUnless { it == - 1 } ?: return

        ServerboundContainerClickPacket(
            windowId, stateId,
            Shorts.checkedCast(slotId.toLong()),
            SignedBytes.checkedCast((if (btn == 0) 2 else btn).toLong()),
            if (btn == 0) ContainerInput.CLONE else ContainerInput.PICKUP,
            Int2ObjectOpenHashMap(),
            HashedStack.create(ItemStack.EMPTY, connection.decoratedHashOpsGenenerator())
        ).send()

        ChatUtils.debug("terminal", "Clicked $slotId on ${TerminalListener.currentHandler?.displayName}")

        TerminalListener.clickedSlots.add(this)
        TerminalListener.currentHandler?.onSlotClick(slotId)
    }
}