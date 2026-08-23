package com.github.noamm9.ui.notification

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import gg.essential.universal.UGraphics
import gg.essential.universal.UMouse
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import java.awt.Color
import java.util.concurrent.*

object NotificationManager: ISelfInit {
    private val notifications = CopyOnWriteArrayList<Notification>()
    private var lastFrameTime = System.currentTimeMillis()

    fun push(title: String, message: String, duration: Long = 3000L) {
        val t = title.addColor()
        val m = message.addColor()
        if (notifications.any { it.title == t && it.message == m && it.duration == duration }) return
        mc.execute { notifications.add(Notification(t, m, duration)) }
    }

    override fun init() {
        EventBus.register<ScreenEvent.PostRender> {
            val window = mc.window
            val ctx = event.context

            val activeScale = window.screenWidth.toFloat() / ctx.guiWidth().toFloat()
            val normalScale = window.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode).toFloat()
            val correction = normalScale / activeScale

            ctx.pose().pushMatrix()
            ctx.pose().scale(correction)
            render(ctx, ctx.guiWidth() / correction, ctx.guiHeight() / correction)
            ctx.pose().popMatrix()
        }
    }

    @JvmStatic
    @JvmOverloads
    fun render(ctx: GuiGraphicsExtractor, w: Float? = null, h: Float? = null) {
        val now = System.currentTimeMillis()
        val delta = now - lastFrameTime
        lastFrameTime = now
        if (notifications.isEmpty()) return

        val screenW = w ?: ctx.guiWidth().toFloat()
        val screenH = h ?: ctx.guiHeight().toFloat()

        val mX = UMouse.Scaled.x.toInt()
        val mY = UMouse.Scaled.y.toInt()

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

            ctx.drawRect(x, y, width, height, Color(20, 20, 20, 240))
            ctx.drawRect(x, y, width, 2f, Style.accentColor)

            ctx.drawString("§a${notify.title}", x + 10f, y + 8f, Color.GREEN)

            var lineY = y + 20f
            notify.wrappedLines.forEach { line ->
                ctx.text(mc.font, line, (x + 10f).toInt(), lineY.toInt(), Color.GRAY.rgb, true)
                lineY += UGraphics.getFontHeight() + 1f
            }

            val progress = (notify.elapsedTime.toFloat() / notify.duration.toFloat()).coerceIn(0f, 1f)
            val barWidth = width * (1f - progress)
            if (isAlive) ctx.drawRect(x, y + height - 1.5f, barWidth, 1.5f, Style.accentColor.withAlpha(200))

            currentYOffset += (height + 5f) * notify.anim.value
        }
    }

    private class Notification(val title: String, val message: String, val duration: Long) {
        val anim = Animation(350L)
        var elapsedTime = 0L
        var isDead = false

        val wrappedLines by lazy { mc.font.split(Component.literal(message), 150) }
        val height by lazy { 22f + (wrappedLines.size * (UGraphics.getFontHeight() + 1f)) + 4f }
    }
}