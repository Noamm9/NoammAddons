package noammaddons.utils

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type
import gg.essential.universal.ChatColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.Constants
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.ItemRarity.Companion.PET_PATTERN
import noammaddons.utils.ItemUtils.ItemRarity.Companion.RARITY_PATTERN
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.Utils.isNull
import java.awt.Color


object ItemUtils {
    private val textureCache = mutableMapOf<NBTTagCompound, ResourceLocation?>()

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

    @Serializable
    data class APISBItem(
        @SerialName("id")
        val id: String,
        @SerialName("material")
        val material: String,
        @SerialName("motes_sell_price")
        val motesSellPrice: Double? = null,
        @SerialName("name")
        val name: String,
        @SerialName("npc_sell_price")
        val npcSellPrice: Double? = null,
    )

    data class bzitem(
        val buyPrice: Double,
        val buyVolume: Double,
        val id: String,
        val name: String,
        val price: Double,
        val sellPrice: Double,
        val sellVolume: Double,
        val tag: String?
    )

    val ItemStack?.SkyblockID: String
        get() = this?.getSubCompound("ExtraAttributes", false)?.getString("id") ?: ""

    val ItemStack.lore: List<String>
        get() = this.tagCompound?.getCompoundTag("display")?.getTagList("Lore", 8)?.let {
            val list = mutableListOf<String>()
            for (i in 0 until it.tagCount()) {
                list.add(it.getStringTagAt(i))
            }
            list
        } ?: emptyList()


    fun getHotbar(): Array<ItemStack?> {
        return Player?.inventory?.mainInventory
            ?.filterIndexed { index, _ -> index in 0 .. 8 }
            ?.toTypedArray() ?: arrayOfNulls(9)
    }


    fun getItemIndexInHotbar(name: String): Int? {
        getHotbar().forEachIndexed { index, stack ->
            when {
                stack == null -> return@forEachIndexed
                stack.displayName.removeFormatting().lowercase().contains(name) -> return index
            }
        }
        return null
    }

    /**
     * Returns the rarity of a given Skyblock item
     * @author SkytilsMod
     * @param ItemStack the Skyblock item to check
     * @return the rarity of the item if a valid rarity is found, `ItemRarity.NONE` if no rarity is found
     */
    fun getRarity(item: ItemStack?): ItemRarity {
        if (item == null || ! item.hasTagCompound()) return ItemRarity.NONE
        val display = item.getSubCompound("display", false)
        if (display == null || ! display.hasKey("Lore")) return ItemRarity.NONE

        val lore = display.getTagList("Lore", Constants.NBT.TAG_STRING)
        val name = display.getString("Name")

        for (i in (lore.tagCount() - 1) downTo 0) {
            val currentLine = lore.getStringTagAt(i)
            val rarityMatcher = RARITY_PATTERN.find(currentLine) ?: continue
            val rarity = rarityMatcher.groups["rarity"]?.value ?: continue

            ItemRarity.entries.find {
                it.rarityName == rarity.removeFormatting().substringAfter("SHINY ")
            }?.let {
                return it
            }
        }

        val petRarityMatcher = PET_PATTERN.find(name) ?: return ItemRarity.NONE
        val color = petRarityMatcher.groupValues.getOrNull(1) ?: return ItemRarity.NONE
        return ItemRarity.byBaseColor(color) ?: ItemRarity.NONE
    }

    fun ItemStack.getItemId(): Int = Item.getIdFromItem(item)

    fun getHeadSkinTexture(itemStack: ItemStack): ResourceLocation? {
        if (itemStack.item == Items.skull && itemStack.metadata == 3) {
            val nbt = itemStack.tagCompound ?: return null
            if (! nbt.hasKey("SkullOwner", 10)) return null

            val skullOwner = nbt.getCompoundTag("SkullOwner")
            val profile = NBTUtil.readGameProfileFromNBT(skullOwner)
            if (profile.isNull()) return null

            if (textureCache.containsKey(nbt)) return textureCache[nbt]

            val textureManager = mc.skinManager
            val textures = textureManager.loadSkinFromCache(profile)
            val skin = textures[Type.SKIN] ?: return null

            val texture = textureManager.loadSkin(skin, Type.SKIN)
            textureCache[nbt] = texture

            return texture
        }
        return null
    }
}