package noammaddons.features.impl.general


import net.minecraft.item.ItemEnderPearl
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature

object EnderPearlFix: Feature(
    "Disables Hypixel's stupid Ender Pearls throw block when you are too close to a wall/floor/ceiling."
) {
    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
        if (mc.thePlayer.heldItem?.item !is ItemEnderPearl) return
        event.isCanceled = true
    }
}
