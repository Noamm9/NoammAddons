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
}