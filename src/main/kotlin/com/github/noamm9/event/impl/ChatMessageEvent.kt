package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.unformattedText
import net.minecraft.network.chat.Component

class ChatMessageEvent(val component: Component): Event(cancelable = true) {
    val formattedText by lazy { component.formattedText }
    val unformattedText by lazy { component.unformattedText }

    fun modify(new: String) = modify(Component.literal(new.addColor()))
    fun modify(new: Component) { cancel(); ChatUtils.chat(new) }
}
