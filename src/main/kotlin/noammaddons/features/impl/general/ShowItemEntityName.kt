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
import java.awt.Color


object ShowItemEntityName: Feature("Show the name of items on the ground") {
    private val onlyBooks = ToggleSetting("Only books", false)
    override fun init() = addSettings(onlyBooks)

    private val BlackList = listOf<Any>(
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
        if (! inSkyblock) return
        val entity = event.entity as? EntityItem ?: return
        val item = entity.entityItem.item
        if (item in BlackList) return
        val name = entity.entityItem.displayName
        if (name.lowercase().contains("330b74f-2e3b-3fb6-9143-a1f0e63fad59")) return

        drawString(
            if (name.removeFormatting() == "Enchanted Book") entity.entityItem.lore[0]
            else if (! onlyBooks.value) name
            else return,
            entity.renderVec.add(y = 1.3),
            Color.WHITE, 0.6f
        )
    }
}