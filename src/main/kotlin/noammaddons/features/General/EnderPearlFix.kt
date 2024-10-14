package noammaddons.features.General


import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.PlayerUtils.Player

object EnderPearlFix {
    @SubscribeEvent
    fun FixEnderPearl(event: PlayerInteractEvent) {
       // if (!config.enderPearlFix) return
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
        if (Player?.heldItem?.displayName.removeFormatting() != "Ender Pearl") return
        event.isCanceled = true
    }
}
