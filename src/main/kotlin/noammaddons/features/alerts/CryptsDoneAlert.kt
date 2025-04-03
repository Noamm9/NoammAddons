package noammaddons.features.alerts

import noammaddons.features.Feature
import noammaddons.utils.ChatUtils
import noammaddons.utils.TablistListener

object CryptsDoneAlert: Feature() {
    init {
        TablistListener.cryptsCount.onSetValue { cryptsCount ->
            if (! config.cryptsDoneAlert) return@onSetValue
            if (cryptsCount != 5) return@onSetValue
            repeat(2) { mc.thePlayer.playSound("note.pling", 1F, 1.79F) }
            ChatUtils.modMessage("&aCrypts Done!")
            ChatUtils.showTitle("&aCrypts Done!")
        }
    }
}
