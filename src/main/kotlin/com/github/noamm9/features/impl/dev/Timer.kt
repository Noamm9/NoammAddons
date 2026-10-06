package com.github.noamm9.features.impl.dev

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.EntityTypes

object Timer: Feature(), ICommandProvider {
    private var ticks = 0

    override fun CommandBuilder.command() {
        setName("timer")
        requires("enable Timer feature first") { enabled }
        runs { ChatUtils.chat("Timer ticks: $ticks") }
        literal("start") { runs { start("cmd") } }
        literal("stop") { runs { end() } }
    }

    override fun init() {
        register<RenderOverlayEvent> {
            if (! ticker.isActive) return@register
            event.context.drawCenteredString(ticks.toString(), Resolution.width / 2, Resolution.height / 3, scale = 2.5f)
        }

        register<ChatMessageEvent> {
            if (event.unformattedText == "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!") start("Maxor") // 83
            if (event.unformattedText == "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!") start("Storm") // 28
            if (event.unformattedText == "[BOSS] Storm: I should have known that I stood no chance.") start("Goldor") // Timer ticks: 17
            if (event.unformattedText == "[BOSS] Goldor: Who dares trespass into my domain?") end() // Timer ticks: 17
            if (event.unformattedText == "[BOSS] Necron: You went further than any human before, congratulations.") start("Necron") // 60
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundAddEntityPacket && event.packet.type == EntityTypes.WITHER_SKELETON) end()
            if (event.packet is ClientboundRemoveEntitiesPacket && event.packet.entityIds.contains(player.vehicle?.id ?: 67)) end()
            if (event.packet is ClientboundSetPassengersPacket) {
                val a = player.vehicle?.id ?: return@register
                if (event.packet.passengers.contains(player.id)) return@register
                if (event.packet.vehicle == a) end()
            }

        }
    }

    private val ticker = EventBus.listener<TickEvent.Server> { ticks ++ }


    fun start(string: String) {
        ticks = 0
        ticker.register()
        ChatUtils.chat("Timer started ($string)")
    }

    fun end() {
        if (! ticker.isActive) return
        ChatUtils.chat("Timer ticks: $ticks")
        ticker.unregister()
        ticks = 0
    }
}