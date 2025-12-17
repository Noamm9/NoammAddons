package noammaddons.utils

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.DevOptions
import java.util.concurrent.*

object ThreadUtils {
    private data class TickTask(var ticks: Int, val action: () -> Unit)

    private fun createDaemonFactory(name: String): ThreadFactory {
        return ThreadFactory { r ->
            Thread(r, name).apply { isDaemon = true }
        }
    }

    private val asyncExecutor = Executors.newCachedThreadPool(createDaemonFactory("$MOD_NAME-Async"))
    private val scheduler = Executors.newScheduledThreadPool(1, createDaemonFactory("$MOD_NAME-Scheduler"))
    private val tickTasks = ConcurrentLinkedQueue<TickTask>()


    fun runOnMcThread(block: () -> Unit) {
        if (mc.isCallingFromMinecraftThread) safeRun(block)
        else mc.addScheduledTask { safeRun(block) }
    }

    fun runAsync(block: () -> Unit) {
        asyncExecutor.submit { safeRun(block) }
    }

    fun setTimeout(delay: Long, block: () -> Unit): ScheduledFuture<*> {
        return scheduler.schedule({ safeRun(block) }, delay, TimeUnit.MILLISECONDS)
    }

    fun scheduledTask(ticks: Int = 0, block: () -> Unit) {
        tickTasks.add(TickTask(ticks, block))
    }

    fun loop(delayProvider: () -> Number, stopCondition: () -> Boolean = { false }, block: () -> Unit) {
        val taskWrapper = object: Runnable {
            override fun run() {
                safeRun(block)
                if (! stopCondition()) {
                    scheduler.schedule(this, delayProvider().toLong(), TimeUnit.MILLISECONDS)
                }
            }
        }
        scheduler.execute(taskWrapper)
    }

    fun loop(delay: Number, stopCondition: () -> Boolean = { false }, block: () -> Unit) {
        loop({ delay }, stopCondition, block)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || tickTasks.isEmpty()) return

        tickTasks.removeIf { entry ->
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

    private inline fun safeRun(block: () -> Unit) {
        try {
            block()
        }
        catch (e: Throwable) {
            if (DevOptions.devMode) e.printStackTrace()
            else Logger.error("Error in ThreadUtils task: ${e.message}")
        }
    }
}