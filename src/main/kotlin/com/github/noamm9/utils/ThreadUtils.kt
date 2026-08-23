package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.ShutdownEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.init.types.ISelfInit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.*
import java.util.concurrent.atomic.*

object ThreadUtils: ISelfInit {
    private val shutdownTasks = ConcurrentLinkedQueue<Runnable>()
    private val clientScheduler = TickScheduler()
    private val serverScheduler = TickScheduler()

    override fun init() {
        register<TickEvent.Start>(EventPriority.HIGHEST) { clientScheduler.tick() }
        register<TickEvent.Server>(EventPriority.HIGHEST) { serverScheduler.tick() }
        register<ShutdownEvent> { shutdownTasks.forEach { safeRun(it) } }
    }

    fun addShutdownHook(block: Runnable) = shutdownTasks.add(block)
    fun scheduledTask(ticks: Number = 0, block: Runnable) = clientScheduler.schedule(ticks, block)
    fun scheduledTaskServer(ticks: Number = 0, block: Runnable) = serverScheduler.schedule(ticks, block)

    fun setTimeout(delayMillis: Number, block: suspend () -> Unit) {
        scope.launch {
            delay(delayMillis.toLong())
            safeRun(block)
        }
    }

    fun async(block: suspend () -> Unit) {
        scope.launch { safeRun(block) }
    }

    fun loop(delayProvider: () -> Number, stopCondition: () -> Boolean = { false }, block: suspend () -> Unit) {
        scope.launch {
            while (true) {
                safeRun(block)
                if (stopCondition()) break
                delay(delayProvider().toLong())
            }
        }
    }

    fun loop(delayMillis: Number, stopCondition: () -> Boolean = { false }, block: suspend () -> Unit) =
        loop({ delayMillis }, stopCondition, block)


    private fun safeRun(action: Runnable) {
        runCatching { action.run() }.onFailure { logger.error("Error in ThreadUtils task", it) }
    }

    private suspend fun safeRun(block: suspend () -> Unit) {
        runCatching { block.invoke() }.onFailure { logger.error("Error in ThreadUtils task", it) }
    }

    private class TickScheduler {
        private val queue = PriorityBlockingQueue<TickTask>()
        private val tickCounter = AtomicLong()

        fun schedule(ticks: Number, action: Runnable) {
            val scheduledTick = tickCounter.get() + ticks.toLong().coerceAtLeast(0L) + 1L
            queue.add(TickTask(scheduledTick, action))
        }

        fun tick() {
            val currentTick = tickCounter.incrementAndGet()
            while (true) {
                val next = queue.peek() ?: return
                if (next.executeAtTick > currentTick) return
                queue.poll()?.run { safeRun(action) }
            }
        }
    }

    private class TickTask(val executeAtTick: Long, val action: Runnable): Comparable<TickTask> {
        override fun compareTo(other: TickTask) = executeAtTick.compareTo(other.executeAtTick)
    }
}