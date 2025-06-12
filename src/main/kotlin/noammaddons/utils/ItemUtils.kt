package noammaddons.utils

import com.mojang.authlib.minecraft.MinecraftProfileTexture.*
import gg.essential.universal.ChatColor
import net.minecraft.entity.Entity
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.nbt.NBTUtil.*
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.Constants
import noammaddons.features.impl.dungeons.ChestProfit
import noammaddons.noammaddons.Companion.bzData
import noammaddons.noammaddons.Companion.itemIdToNameLookup
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.ItemRarity.Companion.PET_PATTERN
import noammaddons.utils.ItemUtils.ItemRarity.Companion.RARITY_PATTERN
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.NumbersUtils.toRoman
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.startsWithOneOf
import java.awt.Color


object ItemUtils {
    private val essenceRegex = Regex("§d(?<type>\\w+) Essence §8x(?<count>\\d+)")
    val rarityCache = mutableMapOf<NBTTagCompound, ItemRarity>()
    val textureCache = mutableMapOf<NBTTagCompound, ResourceLocation?>()

    enum class ItemRarity(val baseColor: ChatColor, val color: Color = baseColor.color !!) {
        NONE(ChatColor.GRAY),
        COMMON(ChatColor.WHITE, Color(255, 255, 255)),
        UNCOMMON(ChatColor.GREEN, Color(77, 231, 77)),
        RARE(ChatColor.BLUE, Color(85, 85, 255)),
        EPIC(ChatColor.DARK_PURPLE, Color(151, 0, 151)),
        LEGENDARY(ChatColor.GOLD, Color(255, 170, 0)),
        MYTHIC(ChatColor.LIGHT_PURPLE, Color(255, 85, 255)),
        DIVINE(ChatColor.AQUA, Color(85, 255, 255)),
        SUPREME(ChatColor.DARK_RED, Color(170, 0, 0)),
        ULTIMATE(ChatColor.DARK_RED, Color(170, 0, 0)),
        SPECIAL(ChatColor.RED, Color(255, 85, 85)),
        VERY_SPECIAL(ChatColor.RED, Color(170, 0, 0));

        val rarityName by lazy {
            name.replace("_", " ").uppercase()
        }

        companion object {
            val RARITY_PATTERN by lazy {
                Regex("(?:§[\\da-f]§l§ka§r )?(?<rarity>${entries.joinToString("|") { "(?:${it.baseColor}§l)+(?:SHINY )?${it.rarityName}" }})")
            }

            val PET_PATTERN by lazy {
                "§7\\[Lvl \\d+](?: §8\\[.*])? (?<color>§[0-9a-fk-or]).+".toRegex()
            }

            fun byBaseColor(color: String) = entries.find { rarity -> rarity.baseColor.toString() == color }
        }
    }

    val ItemStack?.extraAttributes: NBTTagCompound?
        get() = this?.getSubCompound("ExtraAttributes", false)

    val ItemStack?.SkyblockID: String?
        get() = this?.extraAttributes?.getString("id")

    val ItemStack?.skyblockUUID: String
        get() = this?.extraAttributes?.getString("uuid") ?: ""
    

    val ItemStack.lore: List<String>
        get() = tagCompound?.getCompoundTag("display")?.getTagList("Lore", 8)?.let {
            List(it.tagCount()) { i -> it.getStringTagAt(i) }
        }.orEmpty()

    fun setItemLore(item: ItemStack, newLore: List<String>) {
        val compound = item.tagCompound ?: return
        val loreList = NBTTagList()

        for (line in newLore) {
            loreList.appendTag(NBTTagString(line.addColor()))
        }

        compound.getCompoundTag("display").setTag("Lore", loreList)
    }


    fun getHotbar() = mc.thePlayer?.inventory?.let { inv ->
        Array(9) { inv.getStackInSlot(it) }
    } ?: arrayOfNulls(9)


    fun getItemIndexInHotbar(name: String): Int? {
        getHotbar().forEachIndexed { index, stack ->
            when {
                stack == null -> return@forEachIndexed
                stack.displayName.removeFormatting().lowercase().contains(name.lowercase().removeFormatting()) -> return index
            }
        }
        return null
    }

    /**
     * Returns the rarity of a given Skyblock item.
     * @param item The Skyblock item to check.
     * @return The rarity of the item if found, otherwise `ItemRarity.NONE`.
     */
    fun getRarity(item: ItemStack?): ItemRarity {
        val nbt = item?.tagCompound ?: return ItemRarity.NONE
        return rarityCache.getOrPut(nbt) {
            val display = item.getSubCompound("display", false) ?: return@getOrPut ItemRarity.NONE
            if (! display.hasKey("Lore")) return@getOrPut ItemRarity.NONE

            display.getTagList("Lore", Constants.NBT.TAG_STRING).run {
                (tagCount() - 1 downTo 0).map { getStringTagAt(it) }.firstNotNullOfOrNull { line ->
                    val rarityName = RARITY_PATTERN.find(line)?.groups?.get("rarity")?.value?.removeFormatting()?.substringAfter("SHINY ")
                    ItemRarity.entries.find { it.rarityName == rarityName }
                } ?: PET_PATTERN.find(display.getString("Name"))?.groupValues?.getOrNull(1)?.let { color ->
                    ItemRarity.byBaseColor(color)
                } ?: ItemRarity.NONE
            }
        }
    }


    fun ItemStack.getItemId(): Int = Item.getIdFromItem(item)
    fun ItemStack.getSkullId(): String? = getSubCompound("SkullOwner", false)?.getString("Id")
    fun getSkullValue(entity: Entity?): String? = entity?.inventory?.get(4)?.let { getSkullTexture(it) }

    fun getHeadSkinTexture(itemStack: ItemStack): ResourceLocation? {
        if (itemStack.item != Items.skull || itemStack.metadata != 3) return null
        val nbt = itemStack.tagCompound ?: return null

        return textureCache.getOrPut(nbt) {
            if (! nbt.hasKey("SkullOwner", 10)) return@getOrPut null
            val skullOwner = nbt.getCompoundTag("SkullOwner")
            val profile = readGameProfileFromNBT(skullOwner) ?: return@getOrPut null
            val textures = mc.skinManager.loadSkinFromCache(profile)
            val skinUrl = textures[Type.SKIN] ?: return@getOrPut null

            mc.skinManager.loadSkin(skinUrl, Type.SKIN)
        }
    }

    fun getSkullTexture(stack: ItemStack): String? {
        val nbt = stack.tagCompound ?: return null

        val textures = nbt.getCompoundTag("SkullOwner")
            ?.getCompoundTag("Properties")
            ?.getTagList("textures", 10)

        if (textures == null || textures.tagCount() == 0) return null

        return textures.getCompoundTagAt(0)?.getString("Value")
    }

    fun getEssenceValue(text: String): Double {
        if (! ChestProfit.includeEssence.value) return .0
        val groups = essenceRegex.matchEntire(text)?.groups ?: return .0
        val type = groups["type"]?.value?.uppercase() ?: return .0
        val count = groups["count"]?.value?.toInt() ?: return .0
        return (bzData["ESSENCE_$type"]?.sellPrice ?: .0) * count
    }

    fun getIdFromName(name: String): String? {
        return if (name.startsWith("§aEnchanted Book (")) {
            val enchant = name.substring(name.indexOf("(") + 1, name.indexOf(")"))
            return enchantNameToID(enchant)
        }
        else {
            val unformatted = name.removeFormatting().remove("Shiny ")
            itemIdToNameLookup.entries.find {
                it.value == unformatted && ! it.key.contains("STARRED")
            }?.key
        }
    }

    fun enchantNameToID(enchant: String): String {
        val enchantName = enchant.substringBeforeLast(" ")
        val name = enchantName.removeFormatting().uppercase().replace(" ", "_")
        val enchantId = if (enchantName.startsWithOneOf("§9§d§l", "§d§l", "§7§l")) {
            if (! name.contains("ULTIMATE_")) "ULTIMATE_$name"
            else name
        }
        else name

        val level = enchant.substringAfterLast(" ").removeFormatting().run {
            toIntOrNull() ?: romanToDecimal()
        }
        return "ENCHANTMENT_${enchantId}_$level"
    }

    fun idToEnchantName(enchantId: String): String {
        val parts = enchantId.split("_")
        if (parts.size < 3 || parts[0] != "ENCHANTMENT") throw IllegalArgumentException("Invalid enchantment ID format")

        val isUltimate = parts[1] == "ULTIMATE"
        val enchantNameParts = if (isUltimate) parts.drop(2).dropLast(1) else parts.drop(1).dropLast(1)
        val enchantLevel = parts.last().toIntOrNull() ?: throw IllegalArgumentException("Invalid level in enchantment ID")

        val enchantName = enchantNameParts.joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
        }

        val romanLevel = enchantLevel.toRoman()
        val formattedName = if (isUltimate) "§d§l$enchantName $romanLevel§a" else "$enchantName $romanLevel§a"

        return formattedName
    }
}