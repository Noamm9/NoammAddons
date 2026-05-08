package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object ActionUtils {
    private data class Action(val priority: Int, val block: suspend () -> Unit)

    private val actionQueue = ArrayDeque<Action>()
    private var job: Job? = null
    @Volatile var isblocked = false

    /**
     * @param priority The priority of the action (higher values executed first).
     * @param block The suspendable action to execute.
     */
    fun queue(priority: Int = 0, blockInput: Boolean = false, block: suspend () -> Unit) {
        actionQueue.add(Action(priority, block))
        actionQueue.sortByDescending { it.priority }

        if (job?.isActive != true) job = scope.launch {
            while (actionQueue.isNotEmpty()) {
                isblocked = blockInput
                catch { actionQueue.removeFirst().block() }
                isblocked = false
            }
        }
    }

    fun reset() {
        actionQueue.clear()
        isblocked = false
        job?.cancel()
    }

    init {
        EventBus.register<WorldChangeEvent> { reset() }
        EventBus.register<MouseClickEvent> { if (isblocked) event.cancel() }
        EventBus.register<KeyboardEvent.KeyPressed> { if (isblocked) event.cancel() }
        EventBus.register<KeyboardEvent.CharTyped> { if (isblocked) event.cancel() }
    }
}