package noammaddons.features.General

import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.util.Vec3
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ItemUtils
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawString
import noammaddons.utils.RenderUtils.getRenderX
import noammaddons.utils.RenderUtils.getRenderY
import noammaddons.utils.RenderUtils.getRenderZ
import java.awt.Color


object CustomItemEntity {
	private val itemBlackList = listOf<Item>(
		Items.gold_ingot,
		Items.golden_apple,
		Items.golden_carrot,
		Items.golden_horse_armor,
		Items.gold_nugget,
		Items.bone,
		Items.spawn_egg,
		Items.dye
	)
	
	private val blockBlackList = listOf<Block>(
		Blocks.gold_block,
		Blocks.gold_ore,
		Blocks.ice,
		Blocks.tnt
	)
	
	
    fun customItemEntity(entity: EntityItem): Boolean {
        if (!config.CustomItemEntity) return false
	    if (inDungeons) {
	        if (itemBlackList.contains(entity.entityItem.item)) return false
	        if (entity.entityItem.item is ItemBlock) {
			    val block = (entity.entityItem.item as ItemBlock).block
			    if (blockBlackList.contains(block)) return false
	        }
	    }
	    
        val color = ItemUtils.getRarity(entity.entityItem).color
	    
        drawBox(
            entity.getRenderX() -0.275f, entity.getRenderY(), entity.getRenderZ() -0.275f,
            Color(color.red, color.green, color.blue, 85),
            outline = true, fill = true,
            width = 0.55f, height = 0.55f,
        )

        drawString(
	        entity.entityItem.displayName,
	        Vec3(
		        entity.getRenderX().toDouble(),
		        entity.getRenderY() + 1.3,
		        entity.getRenderZ().toDouble()
	        ),
	        Color(255, 255, 255, 255),
	        0.8f
        )
        return true
    }
}