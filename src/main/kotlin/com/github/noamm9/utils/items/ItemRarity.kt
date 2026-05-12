package com.github.noamm9.utils.items

import net.minecraft.ChatFormatting
import net.minecraft.world.item.ItemStack
import java.awt.Color
import java.util.*

enum class ItemRarity(val baseColor: ChatFormatting, val color: Color = Color(baseColor.color !!)) {
    NONE(ChatFormatting.GRAY),
    COMMON(ChatFormatting.WHITE, Color(255, 255, 255)),
    UNCOMMON(ChatFormatting.GREEN, Color(77, 231, 77)),
    RARE(ChatFormatting.BLUE, Color(85, 85, 255)),
    EPIC(ChatFormatting.DARK_PURPLE, Color(151, 0, 151)),
    LEGENDARY(ChatFormatting.GOLD, Color(255, 170, 0)),
    MYTHIC(ChatFormatting.LIGHT_PURPLE, Color(255, 85, 255)),
    DIVINE(ChatFormatting.AQUA, Color(85, 255, 255)),
    SUPREME(ChatFormatting.DARK_RED, Color(170, 0, 0)),
    ULTIMATE(ChatFormatting.DARK_RED, Color(170, 0, 0)),
    SPECIAL(ChatFormatting.RED, Color(255, 85, 85)),
    VERY_SPECIAL(ChatFormatting.RED, Color(170, 0, 0));

    val rarityName by lazy {
        name.replace("_", " ").uppercase()
    }

    companion object {
        val rarityCache = WeakHashMap<ItemStack, ItemRarity>()

        val RARITY_PATTERN by lazy {
            Regex("(?:§[\\da-f]§l§ka§r )?(?<rarity>${
                entries.joinToString("|") {
                    "(?:${it.baseColor}§l)+(?:SHINY )?${it.rarityName}"
                }
            })")
        }

        val PET_PATTERN by lazy {
            "§7\\[Lvl \\d+](?: §8\\[.*])? (?<color>§[0-9a-fk-or]).+".toRegex()
        }

        fun byBaseColor(color: String) = entries.find { rarity -> rarity.baseColor.toString() == color }
    }
}