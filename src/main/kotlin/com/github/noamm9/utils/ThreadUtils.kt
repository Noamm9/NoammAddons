package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.TickEvent
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object ThreadUtils {
    private val scheduler = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "$MOD_NAME-Scheduler").apply { isDaemon = true }
    }

    private val serverTickTasks = PriorityBlockingQueue<TickTask>()
    private val clientTickTasks = PriorityBlockingQueue<TickTask>()

    private val taskOrder = AtomicLong()
    private val serverTickCounter = AtomicLong()
    private val clientTickCounter = AtomicLong()

    init {
        register<TickEvent.Start>(EventPriority.HIGHEST) { process(clientTickTasks, clientTickCounter.incrementAndGet()) }
        register<TickEvent.Server>(EventPriority.HIGHEST) { process(serverTickTasks, serverTickCounter.incrementAndGet()) }
    }

    fun runOnMcThread(block: () -> Unit) {
        if (mc.isSameThread) safeRun(block)
        else mc.execute { safeRun(block) }
    }

    fun setTimeout(delay: Number, block: () -> Unit): ScheduledFuture<*> {
        return scheduler.schedule({ safeRun(block) }, delay.toLong(), TimeUnit.MILLISECONDS)
    }

    fun async(block: () -> Unit) = scheduler.execute { safeRun(block) }

    fun scheduledTask(ticks: Number = 0, block: suspend () -> Unit) {
        enqueue(clientTickTasks, clientTickCounter, ticks, SuspendTaskAction(block))
    }

    fun scheduledTaskServer(ticks: Number = 0, block: suspend () -> Unit) {
        enqueue(serverTickTasks, serverTickCounter, ticks, SuspendTaskAction(block))
    }

    fun loop(delayProvider: () -> Number, stopCondition: () -> Boolean = { false }, block: suspend () -> Unit) {
        val task = object: Runnable {
            override fun run() {
                safeRun(block)
                if (! stopCondition()) {
                    scheduler.schedule(this, delayProvider().toLong(), TimeUnit.MILLISECONDS)
                }
            }
        }

        scheduler.execute(task)
    }

    fun loop(delay: Number, stopCondition: () -> Boolean = { false }, block: suspend () -> Unit) {
        loop({ delay }, stopCondition, block)
    }

    private fun enqueue(queue: PriorityBlockingQueue<TickTask>, currentTick: AtomicLong, ticks: Number, action: TaskAction) {
        val scheduledTick = currentTick.get() + ticks.toLong().coerceAtLeast(0L) + 1L
        queue.add(TickTask(scheduledTick, taskOrder.getAndIncrement(), action))
    }

    private fun process(queue: PriorityBlockingQueue<TickTask>, currentTick: Long) {
        while (true) {
            val next = queue.peek() ?: return
            if (next.executeAtTick > currentTick) return

            val task = queue.poll() ?: continue
            safeRun(task.action)
        }
    }

    private fun safeRun(block: () -> Unit) {
        try {
            block.invoke()
        }
        catch (e: Throwable) {
            logger.error("Error in ThreadUtils task", e)
        }
    }

    private fun safeRun(block: suspend () -> Unit) {
        try {
            runBlocking { block.invoke() }
        }
        catch (e: Throwable) {
            logger.error("Error in ThreadUtils task", e)
        }
    }

    private fun safeRun(action: TaskAction) {
        try {
            action.run()
        }
        catch (e: Throwable) {
            logger.error("Error in ThreadUtils task", e)
        }
    }

    private sealed interface TaskAction {
        fun run()
    }

    private class SuspendTaskAction(private val block: suspend () -> Unit): TaskAction {
        override fun run() = runBlocking { block.invoke() }
    }

    private data class TickTask(
        val executeAtTick: Long,
        val order: Long,
        val action: TaskAction
    ): Comparable<TickTask> {
        override fun compareTo(other: TickTask): Int {
            return compareValuesBy(this, other, TickTask::executeAtTick, TickTask::order)
        }
    }
}