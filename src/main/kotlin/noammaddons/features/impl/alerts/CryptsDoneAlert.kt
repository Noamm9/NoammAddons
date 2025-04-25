package noammaddons.features.impl.alerts

import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils
import noammaddons.utils.TablistListener

object CryptsDoneAlert: Feature() {
    private var alerted = false

    private val showTitle = ToggleSetting("Show Title", true)
    private val msg = ToggleSetting("Send Message", true)
    override fun init() = addSettings(showTitle, msg)


    init {
        TablistListener.cryptsCount.onSetValue { cryptsCount ->
            if (! enabled) return@onSetValue
            if (cryptsCount == 0) alerted = false
            if (cryptsCount < 5) return@onSetValue
            if (alerted) return@onSetValue
            alerted = true
            repeat(2) { mc.thePlayer.playSound("note.pling", 1F, 1.79F) }
            if (showTitle.value) ChatUtils.sendPartyMessage("$CHAT_PREFIX Crypts Done!")
            if (msg.value) ChatUtils.showTitle("&aCrypts Done!")
        }
    }
}
