package noammaddons.features.impl.misc

import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants
import noammaddons.features.Feature
import noammaddons.utils.ItemUtils.skyblockID


object ArrowFix: Feature("Disables Bow Pullback on shortbows. &bCredit: @Ze") {
    private val bowCache = mutableSetOf<String>()

    @JvmStatic
    fun isShortbow(item: ItemStack?): Boolean {
        if (item == null || ! item.hasTagCompound()) return false
        if (item.item != Items.bow) return false
        if (item.skyblockID in bowCache) return true

        val display = item.tagCompound.getCompoundTag("display")
        if (! display.hasKey("Lore", Constants.NBT.TAG_LIST)) return false

        val loreNBT = display.getTagList("Lore", Constants.NBT.TAG_STRING)

        for (i in 0 until loreNBT.tagCount()) {
            val line = loreNBT.getStringTagAt(i)
            if (line.contains("Shortbow: Instantly shoots!")) {
                item.skyblockID?.let(bowCache::add)
                return true
            }
        }

        return false
    }
}
