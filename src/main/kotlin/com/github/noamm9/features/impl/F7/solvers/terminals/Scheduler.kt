package com.github.noamm9.features.impl.F7.solvers.terminals

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import java.util.concurrent.CopyOnWriteArrayList


object Scheduler {
    private var currentTicks = 0L

    private class Task(val targetMs: Long, val targetTicks: Long, val action: Runnable) {
        @Volatile
        var msPassed = false
        @Volatile
        var ticksPassed = false
        @Volatile
        var executed = false
    }

    private val tasks = CopyOnWriteArrayList<Task>()

    /**
     * Schedules a task to run only after both [msDelay] and [tickDelay] have passed.
     */
    fun schedule(msDelay: Int, tickDelay: Int, action: Runnable) {
        tasks.add(Task(
            System.currentTimeMillis() + msDelay,
            currentTicks + tickDelay,
            action
        ))
    }

    val tickListener = EventBus.register<TickEvent.Server> {
        currentTicks ++
        process { task ->
            task.ticksPassed = currentTicks >= task.targetTicks
        }
    }

    val timeListener = EventBus.register<RenderWorldEvent> {
        process { task ->
            task.msPassed = System.currentTimeMillis() >= task.targetMs
        }
    }

    private inline fun process(updateState: (Task) -> Unit) {
        if (tasks.isEmpty()) return

        for (task in tasks) {
            updateState(task)

            if (task.msPassed && task.ticksPassed && ! task.executed) {
                synchronized(task) {
                    if (! task.executed) {
                        task.executed = true
                        task.action.run()
                        tasks.remove(task)
                    }
                }
            }
        }
    }
}