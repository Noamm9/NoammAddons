package noammaddons.features.impl.general

import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.DataDownloader
import noammaddons.utils.Utils.send

object ChatEmojis: Feature("Lets you use [MVP++] emojis in chat") {
    private var emojiMap = DataDownloader.loadJson<Map<String, String>>("emojiMap.json")

    private var isReplacing = false

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        if (isReplacing) return
        val packet = event.packet as? C01PacketChatMessage ?: return
        val msg = packet.message.takeUnless { it.isEmpty() } ?: return
        val escapedKeys = emojiMap.keys.joinToString("|", transform = Regex::escape).toRegex()
        if (! escapedKeys.containsMatchIn(msg)) return
        val newMessage = escapedKeys.replace(msg) { emojiMap[it.value] ?: it.value }.takeUnless { it == msg } ?: return

        event.isCanceled = true
        isReplacing = true
        C01PacketChatMessage(newMessage).send()
        isReplacing = false
    }
}