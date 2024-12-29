package noammaddons.features.dungeons

import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.config.KeyBinds
import noammaddons.events.ClickEvent.RightClickEvent
import noammaddons.events.PacketEvent
import noammaddons.events.RenderOverlay
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.blackList
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toAir
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.isHoldingEtherwarpItem
import noammaddons.utils.Utils.equalsOneOf


object GhostPick: Feature() {
    private val GhostPickElement = TextElement("&b&lGhostPick: &a&lEnabled", dataObj = hudData.getData().GhostPick)
    private fun isAllowedTool(itemStack: ItemStack) = itemStack.item is ItemTool
    var featureState = false


    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (! featureState) return
        val heldItem = Player?.heldItem ?: return
        if (event.packet !is C07PacketPlayerDigging) return

        if (config.LegitGhostPick) {
            if (heldItem.getItemId().equalsOneOf(261, 46)) return
            event.isCanceled = true
        }

        if (config.MimicEffi10) {
            if (! isAllowedTool(heldItem)) return
            if (isHoldingEtherwarpItem()) return
            val blockPos = mc.objectMouseOver?.blockPos
            toAir(blockPos)
        }

    }

    @SubscribeEvent
    fun enable(event: Tick) {
        if (! config.GhostPick) {
            featureState = false
            return
        }

        if (KeyBinds.GhostPick.isPressed) featureState = ! featureState
    }


    @SubscribeEvent
    fun ghostBlocks(event: RightClickEvent) {
        if (! featureState) return
        if (! config.GhostBlocks) return
        if (! mc.gameSettings.keyBindUseItem.isKeyDown) return
        if (! isAllowedTool(Player?.heldItem ?: return)) return
        Player !!.rayTrace(100.0, .0f)?.blockPos?.run {
            toAir(this)
            if (getBlockAt(this) !in blackList) event.isCanceled = true
        }
    }


    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! featureState) return

        GhostPickElement.draw()
    }
}
