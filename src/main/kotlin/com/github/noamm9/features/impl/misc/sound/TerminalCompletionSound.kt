package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.features.Feature
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.utils.ChatUtils
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import com.github.noamm9.NoammAddons

object TerminalCompletionSound : Feature() {

    private val soundConfig = createSoundSettings("Sound", SoundEvents.NOTE_BLOCK_HARP.value())

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (!enabled) return@register

            val packet = event.packet as? ClientboundSystemChatPacket
                ?: return@register

            val playerName = NoammAddons.mc.player?.name?.string
                ?: return@register

            val message = packet.content().string

            if (message.contains("$playerName activated a terminal")) {
                soundConfig.invoke()
            }
        }
    }
}