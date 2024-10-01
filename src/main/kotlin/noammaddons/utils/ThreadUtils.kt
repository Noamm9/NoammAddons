package noammaddons.utils

import java.util.*
import kotlin.concurrent.schedule


object ThreadUtils {
	fun setTimeout(delay: Long, callback: () -> Unit) {
	    Timer().schedule(delay) {
	        callback()
	    }
	}
	

	fun runEvery(delay: Long, stopWhen: () -> Boolean = { false }, task: () -> Unit) {
		val timer = Timer()
		timer.schedule(object : TimerTask() {
			override fun run() {
				task()
				if (stopWhen()) return timer.cancel()
			}
		}, 0, delay)
	}
}