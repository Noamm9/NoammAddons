package NoammAddons.features.General

import net.minecraftforge.client.event.MouseEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.ClickEvent
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.ItemUtils.SkyblockID
import NoammAddons.utils.ItemUtils.isHoldingEtherwarpItem
import NoammAddons.utils.PlayerUtils
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
        if (mc.thePlayer.isSneaking) PlayerUtils.rightClick()
    }

}
