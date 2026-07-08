package com.github.noamm9.utils.items

import net.minecraft.ChatFormatting
import net.minecraft.world.item.ItemStack
import java.awt.Color
import java.util.*

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
    val loreName by lazy { name.replace("_", " ").uppercase() }

    companion object {
        val rarityCache = WeakHashMap<ItemStack, ItemRarity>()

        val RARITY_PATTERN by lazy {
            Regex("(?:§[\\da-f]§l§ka§r )?(?<rarity>${
                entries.joinToString("|") {
                    "(?:${it.baseColor}§l)+(?:SHINY )?${it.loreName}"
                }
            })")
        }

        val PET_PATTERN by lazy {
            "§7\\[Lvl \\d+](?: §8\\[.*])? (?<color>§[0-9a-fk-or]).+".toRegex()
        }

        fun byBaseColor(color: String) = entries.find { rarity -> rarity.baseColor.toString() == color }
    }
}