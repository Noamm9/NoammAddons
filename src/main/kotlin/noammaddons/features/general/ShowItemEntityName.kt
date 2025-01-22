package noammaddons.features.general

import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderUtils.drawString
import java.awt.Color


object ShowItemEntityName: Feature() {
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
    fun renderName(event: RenderEntityEvent) {
        if (! config.ShowItemEntityName) return
        if (! inSkyblock) return
        val entity = event.entity as? EntityItem ?: return
        val item = entity.entityItem.item
        val name = entity.entityItem.displayName
        if (item in BlackList) return
        if (name.removeFormatting().lowercase().contains("330b74f-2e3b-3fb6-9143-a1f0e63fad59")) return
        if (entity.entityItem.SkyblockID.isBlank()) return

        drawString(
            if (name == "Enchanted Book") entity.entityItem.lore[0] else name,
            entity.getRenderVec().add(Vec3(.0, 1.3, .0)),
            Color.WHITE, 0.6f
        )
    }
}