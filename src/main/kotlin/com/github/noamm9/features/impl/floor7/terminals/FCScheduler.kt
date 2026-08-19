package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.utils.Scheduler

class FCScheduler(var delayTicks: Int = 7) {
    private var generation = 0L
    private var isWaiting = false
    private var queuedClick: Runnable? = null

    fun start() {
        val currentGeneration = ++ generation
        isWaiting = true
        queuedClick = null

        Scheduler.schedule(delayTicks * 50) {
            if (currentGeneration != generation) return@schedule

            isWaiting = false
            val click = queuedClick
            queuedClick = null
            click?.run()
        }
    }

    fun cancel() {
        generation ++
        isWaiting = false
        queuedClick = null
    }

    fun runOrQueue(action: Runnable) {
        if (! isWaiting) action.run()
        else if (queuedClick == null) queuedClick = action
    }
}