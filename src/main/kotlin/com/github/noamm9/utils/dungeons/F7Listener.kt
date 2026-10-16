package com.github.noamm9.utils.dungeons

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

object F7Listener: ISelfInit {
    private var necronDead = false
    private var necronStart = false

    override fun init() {
        EventBus.register<ChatMessageEvent> {
            val msg = event.unformattedText

            if (msg == "[BOSS] Necron: ARGH!") {
                necronStart = true
                return@register
            }
        }

        // thx blade for finding out the right sound
        EventBus.register<MainThreadPacketReceivedEvent.Pre> {
            if (! necronStart) return@register
            if (necronDead) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            if (packet.sound.value() != SoundEvents.WITHER_HURT) return@register
            if (packet.volume != 15f) return@register
            if (packet.pitch != 1f) return@register

            EventBus.post(DungeonEvent.NecronDeathEvent)
            necronDead = true
        }

        EventBus.register<WorldChangeEvent> {
            necronStart = false
            necronDead = false
        }
    }
}