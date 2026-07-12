package com.github.noamm9.ui.notification

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.utils.Animation
import net.minecraft.network.chat.Component

class Notification(val title: String, val message: String, val duration: Long) {
    val anim = Animation(350L)
    var elapsedTime = 0L
    var isDead = false

    val wrappedLines = mc.font.split(Component.literal(message), 150)
    val height = 22f + (wrappedLines.size * (mc.font.lineHeight + 1f)) + 4f

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Notification) return false
        if (other.title != title) return false
        if (other.message != message) return false
        return true
    }

    override fun hashCode(): Int {
        var result = duration.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }
}