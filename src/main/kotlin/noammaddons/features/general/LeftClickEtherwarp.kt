package noammaddons.features.general

import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.rightClick


object LeftClickEtherwarp: Feature() {
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (! config.LeftClickEtherwarp) return
        if (! Player !!.isSneaking) return
        if (! isHoldingEtherwarpItem()) return

        event.isCanceled = true
        rightClick()
        (Player as EntityLivingBase).apply {
            swingProgressInt = - 1
            isSwingInProgress = true
        }
    }
}
