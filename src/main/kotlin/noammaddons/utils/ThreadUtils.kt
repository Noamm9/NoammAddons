package noammaddons.utils

/*
object ThreadUtils {
    private val executor = Executors.newSingleThreadExecutor()
    val tickTasks = ConcurrentLinkedQueue<Pair<Int, Runnable>>()

    fun setTimeout(delay: Long, callback: () -> Unit) {
        Timer("$MOD_NAME - Timer").schedule(delay) {
            callback()
        }
    }

    fun loop(delay: Long, stop: () -> Boolean = { false }, func: () -> Unit) {
        Timer("$MOD_NAME - Loop").run {
            schedule(object: TimerTask() {
                override fun run() {
                    try {
                        func()
                        if (stop()) cancel()
                    }
                    catch (e: Exception) {
                        for (stackTraceElement in e.stackTrace.take(10)) {
                            noammaddons.Logger.error(stackTraceElement)
                        }
                    }
                }
            }, 0, delay)
        }
    }


    fun scheduledTask(ticks: Int = 0, callback: Runnable) {
        tickTasks.add(Pair(ticks, callback))
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        runOnNewThread {
            val newTasks = mutableListOf<Pair<Int, Runnable>>()

            for ((ticks, task) in tickTasks) {
                if (ticks == 0) task.run()
                else newTasks.add(Pair(ticks - 1, task))
            }

            tickTasks.clear()
            tickTasks.addAll(newTasks)
        }
    }

    fun runOnNewThread(block: () -> Unit) {
        executor.submit(block)
    }
}*/

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.Logger
import java.util.concurrent.*

object ThreadUtils {
    private val executor = Executors.newSingleThreadExecutor()
    private val timerExecutor = Executors.newScheduledThreadPool(1)
    val tickTasks = ConcurrentLinkedQueue<Pair<Int, Runnable>>()

    fun setTimeout(delay: Long, callback: () -> Unit) {
        timerExecutor.schedule(callback, delay, TimeUnit.MILLISECONDS)
    }

    fun loop(delay: Long, stop: () -> Boolean = { false }, func: () -> Unit) {
        val task = object: Runnable {
            override fun run() {
                try {
                    func()
                }
                catch (e: Exception) {
                    e.stackTrace.take(10).forEach {
                        Logger.error(it)
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

    fun scheduledTask(ticks: Int = 0, callback: Runnable) {
        tickTasks.add(ticks to callback)
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        runOnNewThread {
            val iterator = tickTasks.iterator()
            while (iterator.hasNext()) {
                val (ticks, task) = iterator.next()
                if (ticks == 0) {
                    task.run()
                    iterator.remove()
                }
                else {
                    iterator.remove()
                    tickTasks.add(ticks - 1 to task)
                }
            }
        }
    }

    fun runOnNewThread(block: () -> Unit) {
        executor.submit {
            try {
                block()
            }
            catch (e: Exception) {
                Logger.error(e.stackTrace.take(10).joinToString("\n"))
            }
        }
    }
}
