package noammaddons.features.general

import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderItemEntityEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderHelper
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
    fun renderName(event: RenderItemEntityEvent.Pre) {
        if (! config.ShowItemEntityName) return
        val entity = event.entity as? EntityItem ?: return

        if (entity.entityItem.displayName.removeFormatting().lowercase().contains("330b74f-2e3b-3fb6-9143-a1f0e63fad59")) return
        if (inDungeons) {
            if (entity.entityItem.item in BlackList) return
            val block = event.entity.entityItem.item as? ItemBlock ?: return
            if (block in BlackList) return
        }

        drawString(
            entity.entityItem.displayName,
            entity.getRenderVec().add(Vec3(.0, 1.3, .0)),
            Color.WHITE, 0.6f
        )

        RenderHelper.enableChums(Color.WHITE)
    }


    @SubscribeEvent
    fun renderaaName(event: RenderItemEntityEvent.Post) {
        if (! config.ShowItemEntityName) return
        val entity = event.entity as? EntityItem ?: return

        if (entity.entityItem.displayName.removeFormatting().lowercase().contains("330b74f-2e3b-3fb6-9143-a1f0e63fad59")) return
        if (inDungeons) {
            if (entity.entityItem.item in BlackList) return
            val block = event.entity.entityItem.item as? ItemBlock ?: return
            if (block in BlackList) return
        }
        
        RenderHelper.disableChums()
    }
}