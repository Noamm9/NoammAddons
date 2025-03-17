package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.SoundUtils.notificationSound


object ThunderBottle: Feature() {
    private const val FULL_THUNDER_BOTTLE = "&e&l⚠ &9&lTHUNDER BOTTLE FULL &e&l⚠"

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.FullThunderBottleAlert) return
        if (event.component.noFormatText != "> Your bottle of thunder has fully charged!") return

        showTitle(FULL_THUNDER_BOTTLE)
        modMessage(FULL_THUNDER_BOTTLE)
        notificationSound.start()
    }
}
