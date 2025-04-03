package noammaddons.features.general.teleport

import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils
import noammaddons.utils.PlayerUtils.rightClick
import noammaddons.utils.ServerPlayer


object LeftClickEtherwarp: Feature() {
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (! config.LeftClickEtherwarp) return
        if (! ServerPlayer.player.sneaking) return
        val item = ServerPlayer.player.getHeldItem() ?: return
        if (! PlayerUtils.isHoldingEtherwarpItem(item)) return
        event.isCanceled = true

        swinghand()
        rightClick()
    }

    private fun swinghand() {
        (mc.thePlayer as EntityLivingBase).apply {
            swingProgressInt = - 1
            isSwingInProgress = true
        }
    }
}
