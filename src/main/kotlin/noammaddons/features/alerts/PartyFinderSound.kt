package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.SoundUtils.Pling

object PartyFinderSound: Feature() {
    private val regex = Regex("Party Finder > .+ joined the dungeon group! \\(.+ Level \\d+\\)")

    @SubscribeEvent
    fun onChat(e: Chat) {
        if (! config.PartyFinderSound) return
        if (! e.component.unformattedText.removeFormatting().matches(regex)) return

        Pling.start()
    }
}
