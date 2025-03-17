package noammaddons.features.dungeons

import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.KeyBinds
import noammaddons.events.*
import noammaddons.events.ClickEvent.*
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.blackList
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toAir
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.PlayerUtils.isHoldingTpItem
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.equalsOneOf


object GhostPick: Feature() {
    private object GhostPickElement: GuiElement(hudData.getData().GhostPick) {
        private const val text = "&b&lGhostPick: &a&lEnabled"
        override val enabled get() = config.GhostPick
        override val width = getStringWidth(text)
        override val height = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    var featureState = false
    private fun isAllowedTool(itemStack: ItemStack) = itemStack.item is ItemTool

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (! featureState) return
        val heldItem = mc.thePlayer?.heldItem ?: return
        if (event.packet !is C07PacketPlayerDigging) return

        if (config.LegitGhostPick) {
            // disable on bow, tnt, blaze rod
            if (heldItem.getItemId().equalsOneOf(261, 46, 369)) return
            event.isCanceled = true
        }

        if (config.MimicEffi10) {
            if (! isAllowedTool(heldItem)) return
            if (isHoldingTpItem()) return
            val blockPos = mc.objectMouseOver?.blockPos
            toAir(blockPos)
        }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
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
        if (! isAllowedTool(mc.thePlayer?.heldItem ?: return)) return
        mc.thePlayer.rayTrace(100.0, .0f)?.blockPos?.run {
            if (getBlockAt(this) in blackList) return@run
            event.isCanceled = true
            toAir(this)
        }
    }


    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! featureState) return
        if (! GhostPickElement.enabled) return
        GhostPickElement.draw()
    }
}
