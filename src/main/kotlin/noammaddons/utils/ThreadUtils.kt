package noammaddons.utils

import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.scope
import noammaddons.events.Tick
import noammaddons.features.impl.DevOptions
import java.util.concurrent.*

object ThreadUtils {
    private data class Task(var ticks: Int, val task: Runnable)

    private val executor = Executors.newSingleThreadExecutor()
    private val timerExecutor = Executors.newScheduledThreadPool(1)
    private val tickTasks = ConcurrentLinkedQueue<Task>()

    fun setTimeout(delay: Long, callback: Runnable) = timerExecutor.schedule(callback, delay, TimeUnit.MILLISECONDS)
    fun scheduledTask(ticks: Int = 0, callback: Runnable) = tickTasks.add(Task(ticks, callback))

    fun runOnNewThread(block: () -> Unit) = executor.submit {
        runCatching(block).onFailure { e ->
            e.stackTrace.forEach(Logger::error)
        }
    }

    fun loop(delay: Long, stop: () -> Boolean = { false }, func: () -> Unit) {
        val task = object: Runnable {
            override fun run() {
                try {
                    func()
                }
                catch (e: Exception) {
                    if (! DevOptions.devMode) {
                        e.stackTrace.take(30).forEach { Logger.error(it) }
                    }
                }
                finally {
                    if (! stop()) {
                        timerExecutor.schedule(this, delay, TimeUnit.MILLISECONDS)
                    }
                }
            }
        }
        timerExecutor.execute(task)
    }

    fun loop(delay: () -> Number, stop: () -> Boolean = { false }, func: () -> Unit) {
        val task = object: Runnable {
            override fun run() {
                try {
                    func()
                }
                catch (e: Exception) {
                    if (! DevOptions.devMode) {
                        e.stackTrace.take(30).forEach { Logger.error(it) }
                    }
                }
                finally {
                    if (! stop()) {
                        timerExecutor.schedule(this, delay().toLong(), TimeUnit.MILLISECONDS)
                    }
                }
            }
        }
        timerExecutor.execute(task)
    }

    @SubscribeEvent
    fun onTick(@Suppress("UNUSED_PARAMETER") event: Tick) {
        scope.launch {
            tickTasks.removeIf { task ->
                if (task.ticks > 0) {
                    task.ticks --
                    false
                }
                else {
                    runCatching { task.task.run() }.onFailure {
                        it.stackTrace.take(30).forEach(Logger::error)
                    }
                    true
                }
            }
        }
    }
}
