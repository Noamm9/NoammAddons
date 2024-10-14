package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.noammaddons.Companion.mc
import noammaddons.config.EditGui.HudElement
import noammaddons.config.KeyBinds
import noammaddons.events.ClickEvent.RightClickEvent
import noammaddons.events.PacketEvent
import noammaddons.events.RenderOverlay
import noammaddons.utils.BlockUtils.blacklist
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toAir
import noammaddons.utils.ItemUtils.getItemId
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.PlayerUtils.Player


object GhostPick {
    private val GhostPickElement = HudElement("&b&lGhostPick: &a&lEnabled", dataObj = hudData.getData().GhostPick)
    private fun isAllowedTool(itemStack: ItemStack) = itemStack.item is ItemTool
    var featureState = false


    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (!featureState) return
        val heldItem = Player?.heldItem ?: return

        if (event.packet is C07PacketPlayerDigging) {
	        if (config.LegitGhostPick) {
                if (heldItem.getItemId().equalsOneOf(261, 46)) return
		        event.isCanceled = true
	        }
	        if (config.MimicEffi10) {
				if (!isAllowedTool(heldItem)) return
		        val blockPos = mc.objectMouseOver?.blockPos
		        toAir(blockPos)
			}
		}
    }

	
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun enable(event: Tick) {
        if (!config.GhostPick) featureState = false

        if (KeyBinds.GhostPick.isPressed) featureState = !featureState
    }

	
    @SubscribeEvent
    fun ghostBlocks(event: RightClickEvent) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown && config.GhostBlocks && featureState) {
            if (!isAllowedTool(Player?.heldItem ?: return)) return
            val blockpos = Player!!.rayTrace(100.0, .0f)?.blockPos ?: return
            toAir(blockpos)
            if (mc.theWorld.getBlockAt(blockpos) !in blacklist) event.isCanceled = true
        }
    }

	
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun draw(event: RenderOverlay) {
        if (!featureState) return
	    
        GhostPickElement.draw()
    }
}
