package com.github.noamm9.utils

import com.github.noamm9.features.impl.floor7.terminals.Scheduler

class FCScheduler(
    private val delayTicks: Int = 7,
    private val schedule: (Int, Int, () -> Unit) -> Unit = { msDelay, tickDelay, action ->
        Scheduler.schedule(msDelay, tickDelay) {
            action()
        }
    }
) {
    private var generation = 0L
    private var isWaiting = false
    private var queuedClick: (() -> Unit)? = null

    fun start() {
        val currentGeneration = ++ generation
        isWaiting = true
        queuedClick = null

        schedule(delayTicks * 50, delayTicks) {
            if (currentGeneration != generation) return@schedule

            isWaiting = false
            val click = queuedClick
            queuedClick = null
            click?.invoke()
        }
    }

    fun cancel() {
        generation ++
        isWaiting = false
        queuedClick = null
    }

    fun runOrQueue(action: () -> Unit) {
        if (! isWaiting) action()
        else if (queuedClick == null) queuedClick = action
    }
}
