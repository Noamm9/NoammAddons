package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.MessageSentEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.Utils.isNull

object ChatEmojis : Feature() {
    private var emojiMap: Map<String, String>? = null

    init {
        fetchJsonWithRetry<Map<String, String>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/emojiMap.json"
        ) { emojiMap = it }
    }


    private fun includesAnyKey(message: String): Boolean = emojiMap !!.keys.any { message.contains(it) }

    @SubscribeEvent
    fun onMessageSent(event: MessageSentEvent) {
        if (emojiMap.isNull()) return
        if (! includesAnyKey(event.message) || ! config.ChatEmojis) return

        var newMessage = event.message
        emojiMap !!.forEach { (key, value) ->
            newMessage = newMessage.replace(Regex(Regex.escape(key)), value)
        }

        event.isCanceled = true
        ChatUtils.sendChatMessage(newMessage)
    }
}
