package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.features.Feature
import noammaddons.utils.ItemUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket


object LeftClickEtherwarp: Feature() {
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (! config.LeftClickEtherwarp) return
        if (! Player !!.isSneaking) return
        if (! isHoldingEtherwarpItem()) return

        sendRightClickAirPacket()
    }
}
