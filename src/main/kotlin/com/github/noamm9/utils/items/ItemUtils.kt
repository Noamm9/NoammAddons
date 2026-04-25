package com.github.noamm9.utils.items

import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.items.ItemRarity.Companion.PET_PATTERN
import com.github.noamm9.utils.items.ItemRarity.Companion.RARITY_PATTERN
import com.github.noamm9.utils.items.ItemRarity.Companion.rarityCache
import com.github.noamm9.utils.network.data.PetSummary
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import kotlin.jvm.optionals.getOrNull


object ItemUtils {
    val idToNameMap = mutableMapOf<String, String>()
    val nameToIdMap = mutableMapOf<String, String>()

    val ItemStack.customData get() = getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
    val ItemStack.lore get() = getOrDefault(DataComponents.LORE, ItemLore.EMPTY).styledLines().map { it.formattedText }
    val ItemStack.itemUUID get() = customData.getString("uuid").orElse("")
    val ItemStack.skyblockId: String
        get() {
            if (isEmpty) return ""
            val customData = customData
            var sbItemID: String? = null

            if (customData.contains("id")) sbItemID = customData.getString("id").getOrNull()?.replace(":", "-")
            if (sbItemID == "PET") {
                val petInfoRaw = customData.getString("petInfo").getOrNull()?.takeIf { it.isNotEmpty() } ?: return sbItemID
                val petInfo = JsonUtils.json.decodeFromString<PetSummary>(petInfoRaw)
                sbItemID += "-$${petInfo.type}-${petInfo.tier}"
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

    fun ItemStack.hasGlint() = componentsPatch.toString().contains("minecraft:enchantment_glint_override=>true")

    fun getRarity(item: ItemStack?): ItemRarity {
        item ?: return ItemRarity.NONE
        if (item.isEmpty) return ItemRarity.NONE
        rarityCache.getIfPresent(item)?.let { return it }

        val rarity = run {
            val lore = item.lore.takeUnless(List<*>::isEmpty) ?: return@run ItemRarity.NONE

            for (i in lore.indices) {
                val line = lore[lore.lastIndex - i]
                val rarityName = RARITY_PATTERN.find(line)?.groups?.get("rarity")?.value?.removeFormatting()?.substringAfter("SHINY ")
                ItemRarity.entries.find { it.rarityName == rarityName }?.let { return@run it }
            }

            PET_PATTERN.find(item.hoverName.formattedText)?.groupValues?.getOrNull(1)?.let(ItemRarity::byBaseColor) ?: ItemRarity.NONE
        }

        rarityCache.put(item, rarity)
        return rarity
    }
}