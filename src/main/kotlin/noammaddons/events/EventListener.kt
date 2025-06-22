package noammaddons.events

import net.minecraftforge.fml.common.eventhandler.Event
import noammaddons.events.EventBus.listeners

class EventListener<T: Event>(val type: Class<T>, val cb: (T) -> Unit) {
    var isRegistered = true

    fun unregister(): EventListener<T> {
        if (! isRegistered) return this
        isRegistered = false
        listeners.remove(this)
        return this
    }

    fun register(): EventListener<T> {
        if (isRegistered) return this
        isRegistered = true
        listeners.add(this)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun invoke(event: Event) {
        if (type.isInstance(event)) {
            cb(event as T)
        }
    }
}