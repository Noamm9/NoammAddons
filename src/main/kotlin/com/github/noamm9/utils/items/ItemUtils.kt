package com.github.noamm9.utils.items

import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.items.ItemRarity.Companion.PET_PATTERN
import com.github.noamm9.utils.items.ItemRarity.Companion.RARITY_PATTERN
import com.github.noamm9.utils.items.ItemRarity.Companion.rarityCache
import com.github.noamm9.utils.network.data.DungeonStats
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import kotlin.jvm.optionals.getOrNull


object ItemUtils {
    val ItemStack.customData get() = getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
    val ItemStack.lore get() = getOrDefault(DataComponents.LORE, ItemLore.EMPTY).styledLines().map { it.formattedText }
    val ItemStack.itemUUID get() = customData.getString("uuid").getOrNull() ?: ""
    val ItemStack.skyblockId: String
        get() {
            if (isEmpty) return ""
            val customData = customData
            var sbItemID: String? = null
            val name = hoverName.unformattedText

            if (customData.contains("id")) sbItemID = customData.getString("id").getOrNull()?.replace(":", "-")

            if (sbItemID == "PET") {
                val petInfoRaw = customData.getString("petInfo").getOrNull()?.takeIf { it.isNotEmpty() } ?: return sbItemID
                val petInfo = JsonUtils.json.decodeFromString<DungeonStats.PetSummary>(petInfoRaw)
                return "PET-${petInfo.type}-${petInfo.tier}"
            }

            if (sbItemID == "ENCHANTED_BOOK") {
                val lore = lore
                val bookName = lore[0].takeIf { it != "§8Combinable in Anvil" } ?: lore[2]
                val enchantName = bookName.substringBeforeLast(" ")
                val levelStr = bookName.substringAfterLast(" ").removeFormatting()

                val name = enchantName.removeFormatting().uppercase().replace(" ", "_")
                val level = levelStr.toIntOrNull() ?: levelStr.romanToDecimal()
                val isUltimate = enchantName.startsWithOneOf("§9§d§l", "§d§l", "§7§l") && ! name.contains("ULTIMATE_")

                return "ENCHANTMENT_${if (isUltimate) "ULTIMATE_" else ""}${name}_$level"
            }

            if (sbItemID.equalsOneOf("RUNE", "UNIQUE_RUNE")) {
                val runes = customData.getCompound("runes").getOrNull() ?: return ""
                val runeId = runes.keySet().singleOrNull() ?: return ""
                val level = runes.getIntOr(runeId, 0)
                if (level <= 0) return ""
                return "RUNE-${runeId.uppercase()}-$level"
            }

            if (sbItemID == "POTION") {
                val potion = customData.getString("potion").getOrNull()?.takeIf(String::isNotEmpty) ?: return ""
                val level = customData.getIntOr("potion_level", 0)
                if (level <= 0) return ""

                return "POTION-${potion.uppercase()}-$level${if (customData.getBooleanOr("enhanced", false)) "-ENHANCED" else ""}"
            }

            if ((sbItemID == null || sbItemID == "ATTRIBUTE_SHARD") && name.endsWith(" Shard")) {
                val cleanName = name.removeFormatting().uppercase().remove(" SHARD").replace(" ", "_").remove("_X1")
                return "SHARD_$cleanName"
            }

            return sbItemID.orEmpty()
        }

    fun getSkullTexture(stack: ItemStack): String? {
        if (stack.isEmpty) return null
        val profile = stack.get(DataComponents.PROFILE) ?: return null
        val properties = profile.partialProfile().properties
        return properties["textures"].firstOrNull()?.value
    }

    fun getSkullId(stack: ItemStack): String? {
        if (stack.isEmpty) return null
        val profile = stack.get(DataComponents.PROFILE) ?: return null
        return profile.partialProfile().id.toString()
    }

    fun ItemStack.hasGlint() = get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true

    fun getRarity(item: ItemStack?): ItemRarity {
        item ?: return ItemRarity.NONE
        if (item.isEmpty) return ItemRarity.NONE
        rarityCache[item]?.let { return it }

        val rarity = run {
            val lore = item.lore.takeUnless(List<*>::isEmpty) ?: return@run ItemRarity.NONE

            for (i in lore.indices) {
                val line = lore[lore.lastIndex - i]
                val rarityName = RARITY_PATTERN.find(line)?.groups?.get("rarity")?.value?.removeFormatting()?.substringAfter("SHINY ")
                ItemRarity.entries.find { it.loreName == rarityName }?.let { return@run it }
            }

            PET_PATTERN.find(item.hoverName.formattedText)?.groupValues?.getOrNull(1)?.let(ItemRarity::byBaseColor) ?: ItemRarity.NONE
        }

        rarityCache[item] = rarity
        return rarity
    }
}