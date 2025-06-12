package noammaddons.features.impl.general

import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.Utils.send
import noammaddons.utils.WebUtils.fetchJsonWithRetry

object ChatEmojis: Feature("Lets you use [MVP++] emojis in chat") {
    private var emojiMap: Map<String, String>? = null

    init {
        fetchJsonWithRetry<Map<String, String>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/emojiMap.json"
        ) { emojiMap = it }
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        val packet = event.packet as? C01PacketChatMessage ?: return
        val msg = packet.message
        if (emojiMap !!.keys.none { it in msg }) return

        val escapedKeys = emojiMap !!.keys.joinToString("|") { Regex.escape(it) }.toRegex()

        val newMessage = escapedKeys.replace(msg) { match ->
            emojiMap !![match.value] ?: match.value
        }

        event.isCanceled = true
        C01PacketChatMessage(newMessage).send()
    }
}
