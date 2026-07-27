package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ThreadUtils.scheduledTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.*
import kotlin.coroutines.resume

object ActionUtils: ISelfInit {
    private data class Action(val priority: Int, val blockInput: Boolean, val block: suspend () -> Unit): Comparable<Action> {
        override fun compareTo(other: Action) = other.priority.compareTo(this.priority)
    }

    private val actionQueue = PriorityBlockingQueue<Action>()
    private var processingJob: Job? = null
    private var running = false
    private val lock = Any()

    @Volatile private var isBlocked = false
    @Volatile private var blockTask = 0L

    /**
     * @param priority The priority of the action (higher values executed first).
     * @param block The suspendable action to execute.
     */
    fun queue(priority: Int = 0, blockInput: Boolean = false, block: suspend () -> Unit) = synchronized(lock) {
        actionQueue.add(Action(priority, blockInput, block))
        if (running) return@synchronized
        running = true
        processingJob = scope.launch { run() }
    }

    private suspend fun run() {
        while (true) {
            val action = synchronized(lock) {
                actionQueue.poll() ?: run { running = false; null }
            } ?: break

            try {
                isBlocked = action.blockInput
                if (isBlocked) ThreadUtils.setTimeout(5000) { if (blockTask == ++ blockTask) isBlocked = false }
                action.block()
            }
            finally {
                isBlocked = false
            }
        }
    }

    fun reset() = catch {
        synchronized(lock) {
            actionQueue.clear()
            processingJob?.cancel()
            processingJob = null
            running = false
            isBlocked = false
        }
    }

    suspend fun waitTicks(ticks: Int = 0, cb: Runnable = {}) = suspendCancellableCoroutine {
        scheduledTask(ticks) {
            cb.run()
            it.resume(Unit)
        }
    }

    override fun init() {
        EventBus.register<WorldChangeEvent> { reset() }
        EventBus.register<MouseClickEvent> { if (isBlocked) event.cancel() }
        EventBus.register<KeyboardEvent.KeyPressed> { if (isBlocked) event.cancel() }
        EventBus.register<KeyboardEvent.CharTyped> { if (isBlocked) event.cancel() }
    }
}