package noammaddons.features.impl.general

import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostRenderEntityEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawString

object ItemEntity: Feature("Show the name of items on the ground") {
    private val showName = ToggleSetting("Show Name", false)
    private val onlyBooks = ToggleSetting("Only books", false).addDependency(showName)
    override fun init() = addSettings(showName, onlyBooks)

    private val blackListItems = listOf<Any>(
        Items.gold_ingot,
        Items.golden_apple,
        Items.golden_carrot,
        Items.golden_horse_armor,
        Items.gold_nugget,
        Items.bone,
        Items.dye,

        Blocks.gold_block,
        Blocks.gold_ore,
        Blocks.ice,
        Blocks.tnt,
        Blocks.stone
    )

    @SubscribeEvent
    fun renderName(event: PostRenderEntityEvent) {
        if (! showName.value) return
        if (! inSkyblock) return
        val entity = event.entity as? EntityItem ?: return
        val itemStack = entity.entityItem
        if (itemStack.item in blackListItems) return
        val name = itemStack.displayName
        if (name.lowercase().contains("330b74f-2e3b-3fb6-9143-a1f0e63fad59")) return

        val nameStr = when {
            name.removeFormatting() == "Enchanted Book" -> itemStack.lore[0]
            onlyBooks.value -> return
            else -> name
        }

        drawString(nameStr, entity.renderVec.add(y = 1.3), scale = 0.6f)
    }
}