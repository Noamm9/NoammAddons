package com.github.noamm9.utils

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.init.types.ISelfInit
import java.util.concurrent.*

object Scheduler: ISelfInit {
    private val tasks = CopyOnWriteArrayList<Task>()
    private var currentTicks = 0L

    /**
     * Schedules a task to run only after both [msDelay] and [tickDelay] have passed.
     */
    fun schedule(msDelay: Int, tickDelay: Int = msDelay / 50, action: Runnable) {
        tasks.add(Task(
            System.currentTimeMillis() + msDelay,
            currentTicks + tickDelay,
            action
        ))
    }

    override fun init() {
        EventBus.register<TickEvent.Server> {
            currentTicks ++
            process { task ->
                task.ticksPassed = currentTicks >= task.targetTicks
            }
        }

        EventBus.register<RenderWorldEvent> {
            process { task ->
                task.msPassed = System.currentTimeMillis() >= task.targetMs
            }
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

    private class Task(val targetMs: Long, val targetTicks: Long, val action: Runnable) {
        @Volatile var msPassed = false
        @Volatile var ticksPassed = false
        @Volatile var executed = false
    }
}