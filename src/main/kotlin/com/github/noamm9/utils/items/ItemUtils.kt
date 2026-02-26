package com.github.noamm9.utils.items

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.items.ItemRarity.Companion.PET_PATTERN
import com.github.noamm9.utils.items.ItemRarity.Companion.RARITY_PATTERN
import com.github.noamm9.utils.items.ItemRarity.Companion.rarityCache
import com.github.noamm9.utils.network.WebUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import kotlin.jvm.optionals.getOrNull


object ItemUtils {
    private val idToNameMap = mutableMapOf<String, String>()
    private val nameToIdMap = mutableMapOf<String, String>()

    fun getNameById(id: String) = idToNameMap[id]
    fun getIdByName(name: String) = nameToIdMap[name]

    fun init() = NoammAddons.scope.launch {
        WebUtils.get<JsonObject>("https://api.hypixel.net/v2/resources/skyblock/items")
            .onSuccess { obj ->
                val itemsArray = obj["items"]?.jsonArray ?: return@onSuccess

                for (element in itemsArray) {
                    val item = element.jsonObject
                    val id = item["id"]?.jsonPrimitive?.content ?: continue
                    val name = item["name"]?.jsonPrimitive?.content ?: continue

                    idToNameMap[id] = name
                    nameToIdMap[name] = id
                }
            }
            .onFailure { NoammAddons.logger.error("Error fetching Skyblock items", it) }
    }

    val ItemStack.customData: CompoundTag get() = getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()

    val ItemStack?.skyblockId: String
        get() {
            if (this == null || this.isEmpty) return ""
            val customData = this.customData
            var sbItemID: String? = null

            if (customData.contains("id")) sbItemID = customData.getString("id").getOrNull()?.replace(":", "-")
            if (sbItemID == "PET") {
                val petInfo = customData.getString("petInfo").getOrNull()?.takeIf { it.isNotEmpty() } ?: return sbItemID
                val petInfoObject = JsonUtils.stringToJson(petInfo).jsonObject
                val type = petInfoObject["type"]?.jsonPrimitive?.content
                val tier = petInfoObject["tier"]?.jsonPrimitive?.content
                if (type != null && tier != null) sbItemID += "-$type-$tier"
            }

            return sbItemID.orEmpty()
        }

    val ItemStack.itemUUID: String get() = customData.getString("uuid").orElse("")
    val ItemStack.lore: List<String> get() = getOrDefault(DataComponents.LORE, ItemLore.EMPTY).styledLines().map { it.formattedText }

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

    fun ItemStack.hasGlint() = componentsPatch.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)?.isPresent == true

    fun getRarity(item: ItemStack?): ItemRarity {
        item ?: return ItemRarity.NONE
        if (item.isEmpty) return ItemRarity.NONE
        rarityCache.getIfPresent(item)?.let { return it }

        val rarity = run {
            val lore = item.lore.takeUnless { it.isEmpty() } ?: return@run ItemRarity.NONE

            for (i in lore.indices) {
                val idx = lore.lastIndex - i
                val line = lore[idx]

                val rarityName = RARITY_PATTERN.find(line)?.groups?.get("rarity")?.value?.removeFormatting()?.substringAfter("SHINY ")

                ItemRarity.entries.find { it.rarityName == rarityName }?.let { return@run it }
            }

            PET_PATTERN.find(item.hoverName.formattedText)?.groupValues?.getOrNull(1)?.let(ItemRarity::byBaseColor) ?: ItemRarity.NONE
        }

        rarityCache.put(item, rarity)
        return rarity
    }
}