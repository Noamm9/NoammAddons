package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.ColorUtils.code
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.ChatFormatting

class ColorCodeSetting(name: String, defaultValue: ChatFormatting = ChatFormatting.WHITE): ConfigHolder<ChatFormatting>(name, defaultValue), Savable {
    companion object {
        val COLORS = listOf(
            ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_AQUA,
            ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.GOLD, ChatFormatting.GRAY,
            ChatFormatting.DARK_GRAY, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, ChatFormatting.WHITE
        )
    }

    override fun write() = JsonPrimitive(value.code.toString())
    override fun read(element: JsonElement) {
        element.asString?.firstOrNull()?.let { char ->
            COLORS.firstOrNull { it.code == char }?.let { value = it }
        }
    }
}