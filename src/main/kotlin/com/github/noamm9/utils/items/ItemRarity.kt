package com.github.noamm9.utils.items

import gg.essential.universal.ChatColor
import net.minecraft.world.item.ItemStack
import java.awt.Color
import java.util.*

enum class ItemRarity(val baseColor: ChatColor) {
    NONE(ChatColor.GRAY),
    COMMON(ChatColor.WHITE),
    UNCOMMON(ChatColor.GREEN),
    RARE(ChatColor.BLUE),
    EPIC(ChatColor.DARK_PURPLE),
    LEGENDARY(ChatColor.GOLD),
    MYTHIC(ChatColor.LIGHT_PURPLE),
    DIVINE(ChatColor.AQUA),
    SUPREME(ChatColor.DARK_RED),
    ULTIMATE(ChatColor.DARK_RED),
    SPECIAL(ChatColor.RED),
    VERY_SPECIAL(ChatColor.RED);

    val color = baseColor.color !!
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

        fun getHypixelColor(rarity: ItemRarity) = when (rarity) {
            COMMON -> Color(0xFFFFFF)
            UNCOMMON -> Color(0x21FF2A)
            RARE -> Color(0x459BFF)
            EPIC -> Color(0xA335EE)
            LEGENDARY -> Color(0xFFA216)
            MYTHIC -> Color(0xFF55FF)
            DIVINE, SUPREME -> Color(0x55FFFF)
            ULTIMATE, VERY_SPECIAL -> Color(0xD13228)
            SPECIAL -> Color(0xFF5555)
            NONE -> rarity.color
        }
    }
}