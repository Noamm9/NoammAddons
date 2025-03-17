package noammaddons.features.general

import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.Utils.send

object ChatEmojis: Feature() {
    private var emojiMap: Map<String, String>? = null

    init {
        fetchJsonWithRetry<Map<String, String>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/emojiMap.json"
        ) { emojiMap = it }
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        if (! config.ChatEmojis) return
        if (emojiMap == null) return
        val packet = event.packet as? C01PacketChatMessage ?: return
        val msg = packet.message
        if (emojiMap !!.keys.none { it in msg }) return


        var newMessage = msg
        emojiMap !!.forEach { (key, value) ->
            newMessage = newMessage.replace(Regex(Regex.escape(key)), value)
        }

        event.isCanceled = true
        C01PacketChatMessage(newMessage).send()
    }
}
