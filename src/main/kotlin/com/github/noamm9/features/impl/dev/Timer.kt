package com.github.noamm9.features.impl.dev

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand

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

        register<DungeonEvent.NecronDeathEvent> { start("Relics") }

        register<TickEvent.Start> {
            if (! ticker.isActive) return@register

            val relicStands = level.entitiesForRendering().filterIsInstance<ArmorStand>().any {
                it.getItemBySlot(EquipmentSlot.HEAD).hoverName.string.contains("Relic")
            }

            if (relicStands) end()
            // necron// 54t // 59t // 63t // 50t 51
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