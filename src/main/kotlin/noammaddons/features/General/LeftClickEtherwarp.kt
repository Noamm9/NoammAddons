package noammaddons.features.General

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ItemUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.rightClick


object LeftClickEtherwarp {
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
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
        if (Player!!.isSneaking) {
	        rightClick()
        }
    }

}
