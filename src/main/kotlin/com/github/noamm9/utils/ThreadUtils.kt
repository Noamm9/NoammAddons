package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.TickEvent
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object ThreadUtils {
    data class TickTask(var ticks: Int, val action: () -> Unit)

    private val scheduler = Executors.newScheduledThreadPool(1) { Thread(it, "$MOD_NAME-Scheduler").apply { isDaemon = true } }
    private val serverTickTasks = ConcurrentLinkedQueue<TickTask>()
    private val clientTickTasks = ConcurrentLinkedQueue<TickTask>()

    fun runOnMcThread(block: () -> Unit) {
        if (mc.isSameThread) safeRun(block)
        else mc.execute { safeRun(block) }
    }

    fun setTimeout(delay: Long, block: () -> Unit): ScheduledFuture<*> {
        return scheduler.schedule({ safeRun(block) }, delay, TimeUnit.MILLISECONDS)
    }

    fun scheduledTask(ticks: Int = 0, block: () -> Unit) {
        clientTickTasks.add(TickTask(ticks, block))
    }

    fun scheduledTaskServer(ticks: Int = 0, block: () -> Unit): TickTask {
        val task = TickTask(ticks, block)
        serverTickTasks.add(task)
        return task
    }

    fun loop(delayProvider: () -> Number, stopCondition: suspend () -> Boolean = { false }, block: suspend () -> Unit) {
        val taskWrapper = object: Runnable {
            override fun run() {
                safeRun(block)
                if (! runBlocking { stopCondition() }) {
                    scheduler.schedule(this, delayProvider().toLong(), TimeUnit.MILLISECONDS)
                }
            }
        }
        scheduler.execute(taskWrapper)
    }

    fun loop(delay: Number, stopCondition: suspend () -> Boolean = { false }, block: suspend () -> Unit) {
        loop({ delay }, stopCondition, block)
    }

    fun init() {
        register<TickEvent.Start>(EventPriority.HIGHEST) {
            if (clientTickTasks.isEmpty()) return@register

            clientTickTasks.removeIf { entry ->
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

        register<TickEvent.Server>(EventPriority.HIGHEST) {
            if (serverTickTasks.isEmpty()) return@register

            serverTickTasks.removeIf { entry ->
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

    private inline fun safeRun(crossinline block: suspend () -> Unit) {
        try {
            runBlocking { block() }
        }
        catch (e: Throwable) {
            logger.error("Error in ThreadUtils task: ${e.message}")
            e.printStackTrace()
        }
    }
}