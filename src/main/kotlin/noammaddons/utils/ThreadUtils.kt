package noammaddons.utils

import java.util.*
import kotlin.concurrent.schedule


object ThreadUtils {
    fun setTimeout(delay: Long, callback: () -> Unit) {
        Timer().schedule(delay) {
            callback()
        }
    }

    fun loop(delay: Long, stop: () -> Boolean = { false }, func: () -> Unit) {
        Timer().run {
            schedule(object: TimerTask() {
                override fun run() {
                    try {
                        func()
                        if (stop()) cancel()
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 0, delay)
        }
    }
}