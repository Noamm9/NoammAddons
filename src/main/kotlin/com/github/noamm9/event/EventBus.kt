@file:Suppress("UNCHECKED_CAST")

package com.github.noamm9.event

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils
import java.util.concurrent.ConcurrentHashMap

object EventBus {
    class EventContext<T: Event>(val event: T, var listener: EventListener<T>)

    val listeners = ConcurrentHashMap<Class<out Event>, Array<EventListener<*>>>()

    @JvmStatic
    fun post(event: Event): Boolean {
        val handlers = listeners[event.javaClass] ?: return event.cancelable && event.isCanceled
        var context: EventContext<Event>? = null

        for (handler in handlers) {
            val typedHandler = handler as EventListener<Event>

            try {
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
        val eventListener = EventListener(T::class.java, priority, block)

        synchronized(listeners) {
            val oldListeners = listeners[T::class.java] ?: emptyArray()
            listeners[T::class.java] = sortListeners(oldListeners.asList() + eventListener)
        }

        return eventListener
    }

    fun unregister(listener: EventListener<*>) {
        synchronized(listeners) {
            val oldListeners = listeners[listener.eventClass] ?: return
            val newListeners = oldListeners.filter { it !== listener }

            if (newListeners.isEmpty()) listeners.remove(listener.eventClass)
            else listeners[listener.eventClass] = newListeners.toTypedArray()
        }
    }

    fun register(listener: EventListener<*>) {
        synchronized(listeners) {
            val oldListeners = listeners[listener.eventClass] ?: emptyArray()
            if (oldListeners.any { it === listener }) return

            listeners[listener.eventClass] = sortListeners(oldListeners.asList() + listener)
        }
    }

    @PublishedApi
    internal fun sortListeners(listeners: List<EventListener<*>>): Array<EventListener<*>> {
        return listeners.sortedBy { it.priority.ordinal }.toTypedArray()
    }
}