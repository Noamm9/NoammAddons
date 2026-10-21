package com.github.noamm9.utils.dungeons

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.dungeons.enums.TerminalSection
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

object F7Listener: ISelfInit {
    private var necronDead = false
    private var necronStart = false

    var section = TerminalSection.UNKNOWN
        private set

    override fun init() {
        EventBus.register<ChatMessageEvent> {
            if (LocationUtils.dungeonFloorNumber != 7) return@register
            if (! LocationUtils.inBoss) return@register
            when (event.unformattedText) {
                "[BOSS] Necron: ARGH!" -> necronStart = true
                "[BOSS] Goldor: Who dares trespass into my domain?" -> section = TerminalSection.S1.start()
                "The Core entrance is opening!" -> section = TerminalSection.GOLDOR
            }

            if (section.ordinal in 1 .. 4) section = section.process(event.unformattedText)
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
            TerminalSection.resetAll()
            section = TerminalSection.UNKNOWN
        }
    }
}