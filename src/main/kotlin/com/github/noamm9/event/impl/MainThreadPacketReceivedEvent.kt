package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.network.protocol.Packet

abstract class MainThreadPacketReceivedEvent(cancellable: Boolean): Event(cancellable) {
    class Pre(val packet: Packet<*>): MainThreadPacketReceivedEvent(true)
    class Post(val packet: Packet<*>): MainThreadPacketReceivedEvent(false)
}