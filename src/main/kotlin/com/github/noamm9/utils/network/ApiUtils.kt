package com.github.noamm9.utils.network

import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.JsonUtils.getArray
import com.github.noamm9.utils.JsonUtils.getBoolean
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.floor

object ApiUtils {
    private val xpRequirements = listOf(
        50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385, 6275, 8940, 12700, 17960, 25340, 35640, 50040, 70040,
        97640, 135640, 188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1683640, 2284640, 3084640, 4149640,
        5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640, 51559640, 66559640, 85559640,
        109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640,
    )

    private val requiredRegex = Regex("^[^A-Za-z]*Requires .+\\.$")

    fun getCatacombsLevel(totalXp: Double): Int {
        if (totalXp < 0) return 0
        for (i in xpRequirements.indices) {
            if (totalXp < xpRequirements[i].toDouble()) {
                return i
            }
        }

        val lastLevelInList = xpRequirements.size
        val xpRequiredForLastLevelInList = xpRequirements.last().toDouble()
        val xpBeyondLastLevelInList = totalXp - xpRequiredForLastLevelInList
        val levelsAboveLastLevel = (xpBeyondLastLevelInList / 200_000_000.0).toInt()
        return lastLevelInList + levelsAboveLastLevel
    }

    fun getMagicalPower(talismanBag: Collection<ItemStack>, profileInfo: JsonObject): Int {
        return talismanBag.map {
            val itemId = it.skyblockId.let { id -> if (id.startsWith("PARTY_HAT_")) "PARTY_HAT" else id }
            val unusable = it.lore.any { line -> requiredRegex.matches(line.removeFormatting()) }
            val rarity = ItemUtils.getRarity(it)

            val mp = if (unusable) 0
            else when (rarity) {
                ItemRarity.MYTHIC -> 22
                ItemRarity.LEGENDARY -> 16
                ItemRarity.EPIC -> 12
                ItemRarity.RARE -> 8
                ItemRarity.UNCOMMON -> 5
                ItemRarity.COMMON -> 3
                ItemRarity.SPECIAL -> 3
                ItemRarity.VERY_SPECIAL -> 5
                else -> 0
            }

            val bonus = when (itemId) {
                "HEGEMONY_ARTIFACT" -> mp
                "ABICASE" -> {
                    val contacts = profileInfo.getArray("abiphone_contacts")?.size ?: 0
                    floor(contacts / 2.0).toInt()
                }

                else -> 0
            }

            itemId to (mp + bonus)
        }.groupBy { it.first }.mapValues { entry ->
            entry.value.maxBy { it.second }
        }.values.fold(0) { acc, pair ->
            acc + pair.second
        }.let {
            if (profileInfo.getBoolean("consumed_rift_prism") == true) it + 11 else it
        }
    }

    fun decodeBase64ItemList(encoded: String): MutableList<ItemStack> {
        if (encoded.isEmpty()) return mutableListOf()

        return try {
            val root = NbtIo.readCompressed(ByteArrayInputStream(Base64.getDecoder().decode(encoded)), NbtAccounter.unlimitedHeap())
            val itemNbtList = root.getList("i").getOrNull() ?: return mutableListOf()

            itemNbtList.indices.mapNotNull { index ->
                val compound = itemNbtList.getCompound(index).getOrNull()?.takeIf { it.size() > 0 } ?: return@mapNotNull null
                val tag = compound.get("tag")?.asCompound()?.getOrNull() ?: return@mapNotNull null
                val extraAttributes = tag.get("ExtraAttributes")?.asCompound()?.getOrNull() ?: return@mapNotNull null
                val id = extraAttributes.get("id")?.asString()?.getOrNull().orEmpty()
                val display = tag.get("display")?.asCompound()?.getOrNull() ?: return@mapNotNull null
                val name = display.get("Name")?.asString()?.getOrNull().orEmpty()
                val lore = display.get("Lore")?.asList()?.getOrNull()?.mapNotNull { it.asString().getOrNull() }.orEmpty()
                val count = compound.getByte("Count").getOrNull()?.toInt()?.coerceAtLeast(1) ?: 1

                ItemStack(Items.PAPER, count).apply {
                    if (name.isNotBlank()) set(DataComponents.CUSTOM_NAME, Component.literal(name))
                    if (lore.isNotEmpty()) set(DataComponents.LORE, ItemLore(lore.map(Component::literal)))
                    val customData = extraAttributes.copy().also { if (! it.contains("id") && id.isNotBlank()) it.putString("id", id) }
                    CustomData.set(DataComponents.CUSTOM_DATA, this, customData)
                }
            }.toMutableList()
        }
        catch (_: Exception) {
            mutableListOf()
        }
    }
}