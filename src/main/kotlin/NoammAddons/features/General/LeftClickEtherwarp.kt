package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.ClickEvent
import NoammAddons.utils.ItemUtils.isHoldingEtherwarpItem
import NoammAddons.utils.PlayerUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.Display

object LeftClickEtherwarp {
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (!config.LeftClickEtherwarp || !isHoldingEtherwarpItem() || !Display.isActive()) return

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
