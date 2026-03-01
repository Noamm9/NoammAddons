package com.github.noamm9.ui.notification

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object NotificationManager {
    private val notifications = CopyOnWriteArrayList<Notification>()
    private var lastFrameTime = System.currentTimeMillis()

    fun push(title: String, message: String, duration: Long = 3000L) {
        if (notifications.any { it.title == title && it.message == message && it.duration == duration }) return
        notifications.add(Notification(title, message, duration))
    }

    @JvmStatic
    fun render(ctx: GuiGraphics) {
        val now = System.currentTimeMillis()
        val delta = now - lastFrameTime
        lastFrameTime = now
        if (notifications.isEmpty()) return

        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        val mX = mc.mouseHandler.getScaledXPos(mc.window).toInt()
        val mY = mc.mouseHandler.getScaledYPos(mc.window).toInt()

        var currentYOffset = 0f

        for (notify in notifications) {
            if (notify.isDead) {
                notifications.remove(notify)
                continue
            }

            val width = 175f
            val height = notify.height

            val isAlive = notify.elapsedTime < notify.duration
            notify.anim.update(if (isAlive) 1f else 0f)

            if (! isAlive && notify.anim.value <= 0.01f) {
                notify.isDead = true
                continue
            }

            val x = screenW - (width + 10f) * notify.anim.value
            val y = screenH - (height + 10f) - currentYOffset

            val isHovered = mX >= x && mX <= x + width && mY >= y && mY <= y + height

            if (! isHovered && isAlive) notify.elapsedTime += delta

            Render2D.drawRect(ctx, x, y, width, height, Color(20, 20, 20, 240))
            Render2D.drawRect(ctx, x, y, width, 2f, Style.accentColor)

            Render2D.drawString(ctx, "§a${notify.title}", x + 10f, y + 8f, Color.GREEN)

            var lineY = y + 20f
            notify.wrappedLines.forEach { line ->
                ctx.drawString(mc.font, line, (x + 10f).toInt(), lineY.toInt(), Color.GRAY.rgb, true)
                lineY += mc.font.lineHeight + 1f
            }

            val progress = (notify.elapsedTime.toFloat() / notify.duration.toFloat()).coerceIn(0f, 1f)
            val barWidth = width * (1f - progress)
            if (isAlive) Render2D.drawRect(ctx, x, y + height - 1.5f, barWidth, 1.5f, Style.accentColor.withAlpha(200))

            currentYOffset += (height + 5f) * notify.anim.value
        }
    }
}