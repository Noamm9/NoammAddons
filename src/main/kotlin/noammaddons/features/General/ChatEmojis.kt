package noammaddons.features.General

import noammaddons.events.MessageSentEvent
import noammaddons.utils.ChatUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config

object ChatEmojis {
    private val emojiMap = mapOf(
        "<3" to "❤",
        ":star:" to "✮",
        ":yes:" to "✔",
        ":no:" to "✖",
        ":java:" to "☕",
        ":arrow:" to "➜",
        ":shrug:" to "¯\\_(ツ)_/¯",
        ":tableflip:" to "(╯°□°）╯︵ ┻━┻",
        "o/" to "( ﾟ◡ﾟ)/",
        ":totem:" to "☉_☉",
        ":typing:" to "✎...",
        ":maths:" to "√(π+x)=L",
        ":snail:" to "@'-'",
        ":thinking:" to "(0.o?)",
        ":gimme:" to "༼つ◕_◕༽つ",
        ":wizard:" to "('-')⊃━☆ﾟ.*･｡ﾟ",
        ":pvp:" to "⚔",
        ":peace:" to "✌",
        ":puffer:" to "<('O')>",
        ":yey:" to "ヽ (◕◡◕) ﾉ",
        ":cat:" to "= ＾● ⋏ ●＾ =",
        ":dab:" to "<o/",
        ":dj:" to "ヽ(⌐■_■)ノ♬",
        ":snow:" to "☃",
        "h/" to "ヽ(^◇^*)/",
        ":sloth:" to "(・⊝・)",
        ":cute:" to "(✿◠‿◠)",
        ":dog:" to "(ᵔᴥᵔ)"
    )

    private fun includesAnyKey(message: String): Boolean = emojiMap.keys.any { message.contains(it) }

    @SubscribeEvent
    fun onMessageSent(event: MessageSentEvent) {
        if (!includesAnyKey(event.message) || !config.ChatEmojis) return

        var newMessage = event.message
        emojiMap.forEach { (key, value) ->
            newMessage = newMessage.replace(Regex(Regex.escape(key)), value)
        }

        event.isCanceled = true
        ChatUtils.sendChatMessage(newMessage)
    }
}
