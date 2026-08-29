package com.github.noamm9.event

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.remove

abstract class Event(val cancelable: Boolean = false) {
    @Volatile
    open var isCanceled = false
        set(value) {
            if (! cancelable && value) throw RuntimeException("tried to cancel an uncancelable event")
            if (value && ! field && "cancel" in NoammAddons.debugFlags) captureSource()
            field = value
        }

    open fun cancel() {
        isCanceled = true
    }

    private fun captureSource() {
        val stack = Thread.currentThread().stackTrace

        for (i in 3 until stack.size) {
            val element = stack[i]
            val className = element.className

            if (className.startsWith("com.github.noamm9.event") ||
                className.startsWith("java.lang") ||
                className.startsWith("kotlin.") ||
                className.contains("EventBus")
            ) continue

            val eventName = this.javaClass.name.remove("com.github.noamm9.event.impl.")
            val fileName = element.fileName ?: "Unknown File"
            val lineNumber = element.lineNumber
            val methodName = element.methodName

            val caller = "$fileName:$lineNumber ($methodName)"
            ChatUtils.modMessage("§c$eventName canceled by: §e$caller")

            break
        }
    }
}