package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.ClickEvent
import noammaddons.utils.ItemUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LeftClickEtherwarp {
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (!config.LeftClickEtherwarp || !isHoldingEtherwarpItem()) return

       /* if (Settings().AutoSneak && !Player.isSneaking()) {
            PlayerUtils.Sneak(true)
            setTimeout(() => {
                PlayerUtils.Click(`right`)
                PlayerUtils.Sneak(false)
            }, 100)
        }
        else */
        if (mc.thePlayer.isSneaking) sendRightClickAirPacket()
    }

}
