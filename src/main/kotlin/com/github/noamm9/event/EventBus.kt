package com.github.noamm9.event

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.event.priority.PriorityComparator
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.startsWithOneOf
import net.minecraft.network.chat.Component
import java.util.concurrent.*

object EventBus {
    val listeners = ConcurrentHashMap<Class<out Event>, List<EventListener<*>>>()
    private val exceptionHandler: (Exception, EventListener<*>, Event) -> Unit = { exception, listener, event ->
        val packageName = Event::class.java.`package`.name
        val eventName = event.javaClass.name.remove("$packageName.impl.")
        var fileName = "Unknown File"
        var line = "line: -1 (unknown)"

        for (element in exception.stackTrace) {
            if (element.className.startsWithOneOf(packageName, "java.lang", "kotlin.")) continue
            fileName = element.fileName ?: "Unknown File"
            line = "line ${element.lineNumber} (${element.methodName})"
            break
        }

        NoammAddons.logger.error("EventBus error", exception)
        ChatUtils.chat(Component.empty().apply {
            append("§c----------------------------------------\n")
            append("§c>> Uncaught Exception in EventBus <<\n")
            append("§c   Event: §f${eventName}§r\n")
            append("§c   File: $fileName: §e$line§r\n")
            append("§c   Error: §f${exception.message}\n")
            append("§c----------------------------------------")
        })
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