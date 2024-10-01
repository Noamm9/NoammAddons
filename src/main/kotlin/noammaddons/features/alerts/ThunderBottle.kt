package noammaddons.features.alerts

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.sounds.mariolikesound
import noammaddons.sounds.notificationsound
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object ThunderBottle {
    private val noThunderBottle = "&e&l⚠ &4No Thunder Bottle &e&l⚠ ".addColor()
    private val fullThunderBottle = "&e&l⚠ &9&lTHUNDER BOTTLE FULL &e&l⚠ ".addColor()
    private val regex = Regex("-+\\n.+entered (MM |)The Catacombs, Floor VII!\\n-+")
    // https://regex101.com/r/8dYYOL/1


    @SubscribeEvent
    fun onChat(event: Chat) {
		val msg = event.component.unformattedText.removeFormatting()
	    when {
		    msg.matches(regex) && config.NoThunderBottleAlert -> {
			    if (!(mc.thePlayer.inventory.mainInventory.any { it?.displayName?.removeFormatting() == "Empty Thunder Bottle" })) {
				    showTitle(noThunderBottle)
				    mariolikesound.play()
				    return
			    }
			}
		    msg == "> Your bottle of thunder has fully charged!" && config.FullThunderBottleAlert -> {
			    showTitle(fullThunderBottle)
			    notificationsound.play()
			    return
			}
		}
    }
}