package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.interfaces.IChatComponent
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.DataDownloader
import net.minecraft.client.GuiMessage
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object Chat: Feature("Useful tweaks for the chat such as Ctrl + Click to copy messages.") {
    private val uselessMessages by lazy { DataDownloader.loadJson<List<String>>("uselessMessages.json").map(::Regex) }

    private val ctrlClickToCopy by ToggleSetting("Ctrl Click to Copy", true).withDescription("Ctrl + Left Click a message to copy it to your clipboard.")
    private val removeUselessMessages by ToggleSetting("Remove useless messages", true).withDescription("Removes a lot of useless messages from the chat.")

    private var lastMessageBlank = false

    override fun init() {
        register<MouseClickEvent> {
            if (! ctrlClickToCopy.value) return@register
            if (mc.screen !is ChatScreen) return@register
            if (event.button != 0) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_CONTROL) != GLFW.GLFW_PRESS) return@register
            val message = getHoveredMsg().takeUnless { it.isBlank() } ?: return@register

            NotificationManager.push("Message copied to clipboard", message)
            mc.keyboardHandler.clipboard = message
            event.isCanceled = true
        }
    }

    @JvmStatic
    fun addMassageHook(comp: Component, ci: CallbackInfo) {
        if (! enabled) return
        if (! removeUselessMessages.value) return
        val msg = comp.unformattedText

        if (msg.isBlank()) {
            if (lastMessageBlank) return ci.cancel()
            else {
                lastMessageBlank = true
                return
            }
        }

        if (uselessMessages.any { it.matches(msg) }) return ci.cancel()
        lastMessageBlank = false
    }

    private fun getHoveredMsg(): String {
        val chatHud = (mc.gui.chat as? IChatComponent) ?: return ""

        val x = chatHud.mouseXtoChatX
        val y = chatHud.mouseYtoChatY
        val i = chatHud.getLineIndex(x, y)

        if (i < 0 || i >= chatHud.visibleMessages.size) return ""

        val builder = StringBuilder()
        val lines = ArrayList<GuiMessage.Line>()

        for (j in i.toInt() + 1 until chatHud.visibleMessages.size) {
            val line = chatHud.visibleMessages[j]
            if (line.endOfEntry()) break
            lines.add(0, line)
        }

        for (j in i.toInt() downTo 0) {
            val line = chatHud.visibleMessages[j]
            lines.add(line)
            if (line.endOfEntry()) break
        }

        for (line in lines) {
            line.content().accept { _, _, codePoint ->
                builder.appendCodePoint(codePoint)
                true
            }
        }

        return builder.toString().removeFormatting()
    }
}