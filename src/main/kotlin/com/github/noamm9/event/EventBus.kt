package com.github.noamm9.event

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.event.priority.PriorityComparator
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.remove
import java.util.concurrent.*

object EventBus {
    val listeners = ConcurrentHashMap<Class<out Event>, List<EventListener<*>>>()
    private val exceptionHandler: (Exception, EventListener<*>, Event) -> Unit = { exception, listener, event ->
        val eventName = event.javaClass.name.remove("com.github.noamm9.event.impl.")
        val stack = Thread.currentThread().stackTrace

        var stackInfo = ""
        for (i in 3 until stack.size) {
            val element = stack[i]
            val className = element.className

            if (className.startsWith("com.github.noamm9.event") ||
                className.startsWith("java.lang") || className.startsWith("kotlin.") ||
                className.contains("EventBus")
            ) continue

            val fileName = element.fileName ?: "Unknown File"
            val lineNumber = element.lineNumber
            val methodName = element.methodName

            stackInfo = "$fileName:$lineNumber ($methodName)"

            break
        }

        val msg = "EventBus Error at $eventName $stackInfo"
        NoammAddons.logger.error("EventBus error", exception)
        ChatUtils.modMessage(msg)
    }

    fun _registerListener(listener: EventListener<*>) {
        listeners.compute(listener.eventClass) { _, old ->
            (old.orEmpty() + listener).sortedWith(PriorityComparator)
        }
    }

    fun _unregisterListener(listener: EventListener<*>) {
        listeners.compute(listener.eventClass) { _, old ->
            old?.filter { it !== listener }?.takeIf(Collection<*>::isNotEmpty)
        }
    }

    @JvmStatic
    fun <T: Event> post(event: T): Boolean {
        val eventListeners = listeners[event.javaClass] ?: return event.isCanceled
        var context: EventContext<T>? = null

        @Suppress("UNCHECKED_CAST")
        for (listener in eventListeners) try {
            val typedListener = listener as EventListener<T>
            if (event.isCanceled && ! typedListener.receiveCancelled) continue
            val currentContext = context ?: EventContext(event, typedListener).also { context = it }
            currentContext.listener = typedListener
            typedListener.callback.invoke(currentContext)
        }
        catch (exception: Exception) {
            exceptionHandler.invoke(exception, listener, event)
        }

        return event.isCanceled
    }

    inline fun <reified T: Event> listener(
        priority: EventPriority = EventPriority.NORMAL,
        receiveCancelled: Boolean = false,
        noinline callback: EventContext<T>.() -> Unit
    ) = EventListener(T::class.java, priority, receiveCancelled, callback)

    inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        receiveCancelled: Boolean = false,
        noinline callback: EventContext<T>.() -> Unit
    ) = listener<T>(priority, receiveCancelled, callback).register()
}