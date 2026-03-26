package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.TickEvent
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object ThreadUtils {
    data class TickTask(var ticks: Int, val action: suspend () -> Unit)

    private val scheduler = Executors.newScheduledThreadPool(1) { Thread(it, "$MOD_NAME-Scheduler").apply { isDaemon = true } }
    private val serverTickTasks = ConcurrentLinkedQueue<TickTask>()
    private val clientTickTasks = ConcurrentLinkedQueue<TickTask>()

    init {
        register<TickEvent.Start>(EventPriority.HIGHEST) { prossess(clientTickTasks) }
        register<TickEvent.Server>(EventPriority.HIGHEST) { prossess(serverTickTasks) }
    }

    fun runOnMcThread(block: () -> Unit) {
        if (mc.isSameThread) safeRun(block)
        else scheduledTask { safeRun(block) }
    }

    fun setTimeout(delay: Number, block: () -> Unit): ScheduledFuture<*> {
        return scheduler.schedule({ safeRun(block) }, delay.toLong(), TimeUnit.MILLISECONDS)
    }

    fun async(block: () -> Unit) = scheduler.execute { safeRun(block) }
    fun scheduledTask(ticks: Int = 0, block: suspend () -> Unit) = clientTickTasks.add(TickTask(ticks, block))
    fun scheduledTaskServer(ticks: Int = 0, block: suspend () -> Unit) = serverTickTasks.add(TickTask(ticks, block))

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

    private fun safeRun(block: suspend () -> Unit) {
        try {
            runBlocking { block.invoke() }
        }
        catch (e: Throwable) {
            logger.error("Error in ThreadUtils task: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun prossess(list: ConcurrentLinkedQueue<TickTask>) {
        if (list.isEmpty()) return

        list.removeIf { entry ->
            if (entry.ticks <= 0) {
                safeRun(entry.action)
                true
            }
            else {
                entry.ticks --
                false
            }
        }
    }
}