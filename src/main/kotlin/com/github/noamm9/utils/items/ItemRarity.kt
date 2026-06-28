package com.github.noamm9.utils.items

import net.minecraft.ChatFormatting
import java.awt.Color

enum class ItemRarity(val baseColor: ChatFormatting) {
    NONE(ChatFormatting.GRAY),
    COMMON(ChatFormatting.WHITE),
    UNCOMMON(ChatFormatting.GREEN),
    RARE(ChatFormatting.BLUE),
    EPIC(ChatFormatting.DARK_PURPLE),
    LEGENDARY(ChatFormatting.GOLD),
    MYTHIC(ChatFormatting.LIGHT_PURPLE),
    DIVINE(ChatFormatting.AQUA),
    SUPREME(ChatFormatting.DARK_RED),
    ULTIMATE(ChatFormatting.DARK_RED),
    SPECIAL(ChatFormatting.RED),
    VERY_SPECIAL(ChatFormatting.RED);

    val color = Color(baseColor.color !!)
}