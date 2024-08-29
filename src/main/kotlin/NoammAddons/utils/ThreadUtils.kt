package NoammAddons.utils

import java.util.Timer
import kotlin.concurrent.schedule


object ThreadUtils {
    fun setTimeout(delay: Long, callback: () -> Unit) {
        Timer().schedule(delay) {
            callback()
        }
    }
}