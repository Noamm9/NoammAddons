package noammaddons.features.General

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ItemUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.RightClickMouse


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
        if (mc.thePlayer.isSneaking) {
			RightClickMouse()
        }
    }

}
