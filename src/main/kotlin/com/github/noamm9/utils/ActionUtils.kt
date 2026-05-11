package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

object ActionUtils {
    private data class Action(val priority: Int, val blockInput: Boolean, val block: suspend () -> Unit): Comparable<Action> {
        override fun compareTo(other: Action): Int = other.priority.compareTo(this.priority)
    }

    private val mutex = Mutex()
    private val actionQueue = PriorityQueue<Action>()
    private var processingJob: Job? = null

    @Volatile private var isBlocked = false

    /**
     * @param priority The priority of the action (higher values executed first).
     * @param block The suspendable action to execute.
     */
    fun queue(priority: Int = 0, blockInput: Boolean = false, block: suspend () -> Unit) = scope.launch {
        mutex.withLock {
            actionQueue.add(Action(priority, blockInput, block))
            if (processingJob?.isActive != true) {
                processingJob = scope.launch {
                    runProcessor()
                }
            }
        }
    }

    private suspend fun runProcessor() {
        while (true) {
            val currentAction = mutex.withLock {
                if (actionQueue.isEmpty()) return@runProcessor
                actionQueue.poll()
            }

            try {
                isBlocked = currentAction.blockInput
                currentAction.block()
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            finally {
                isBlocked = false
            }
        }
    }

    fun reset() {
        processingJob?.cancel()
        processingJob = null

        scope.launch {
            mutex.withLock {
                actionQueue.clear()
                isBlocked = false
            }
        }
    }

    init {
        EventBus.register<WorldChangeEvent> { reset() }
        EventBus.register<MouseClickEvent> { if (isBlocked) event.cancel() }
        EventBus.register<KeyboardEvent.KeyPressed> { if (isBlocked) event.cancel() }
        EventBus.register<KeyboardEvent.CharTyped> { if (isBlocked) event.cancel() }
    }
}