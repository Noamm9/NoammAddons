@file:Suppress("UNCHECKED_CAST")

package com.github.noamm9.event

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils
import java.util.concurrent.*

object EventBus {
    class EventContext<T: Event>(val event: T, var listener: EventListener<T>)

    val listeners = ConcurrentHashMap<Class<out Event>, List<EventListener<*>>>()

    @JvmStatic
    fun post(event: Event): Boolean {
        val handlers = listeners[event.javaClass] ?: return event.cancelable && event.isCanceled
        var context: EventContext<Event>? = null

        for (i in handlers.indices) {
            val handler = handlers[i]
            try {
                val typedHandler = handler as EventListener<Event>
                val currentContext = context ?: EventContext(event, typedHandler).also { context = it }
                currentContext.listener = typedHandler
                typedHandler.callback.invoke(currentContext)
            }
            catch (e: Exception) {
                val stacktrace = e.stackTrace.joinToString("\n")
                NoammAddons.logger.error("EventBus Error in ${event.javaClass.name}", e)
                ChatUtils.clickableChat("EventBus Error: class ${event.javaClass.name}. message: ${e.message}", true, copy = stacktrace, hover = stacktrace)
            }
        }

        return event.cancelable && event.isCanceled
    }

    @JvmStatic
    inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        noinline block: EventContext<T>.() -> Unit
    ): EventListener<T> {
        return EventListener(T::class.java, priority, block).register()
    }

    fun unregister(listener: EventListener<*>) = synchronized(listeners) {
        val oldListeners = listeners[listener.eventClass] ?: return@synchronized
        val newListeners = oldListeners.filter { it !== listener }

        if (newListeners.isEmpty()) listeners.remove(listener.eventClass)
        else listeners[listener.eventClass] = newListeners
    }

    fun register(listener: EventListener<*>) = synchronized(listeners) {
        val oldListeners = listeners[listener.eventClass] ?: emptyList()
        if (oldListeners.contains(listener)) return@synchronized
        listeners[listener.eventClass] = (oldListeners + listener).sortedBy { it.priority.ordinal }
    }
}