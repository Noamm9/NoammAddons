package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

object DoorKeys: Feature("ESP box & Tracer for wither doors and blood door.") {
    private val highlightWither by ToggleSetting("Wither Key").section("Keys")
    private val highlightBlood by ToggleSetting("Blood Key")
    private val witherColor by ColorSetting("Wither Key Color", Color.BLACK.withAlpha(60)).showIf { highlightWither.value }.section("Colors")
    private val bloodColor by ColorSetting("Blood Key Color", Color.RED.withAlpha(60)).showIf { highlightBlood.value }

    private var doorKey: Pair<Entity, Color>? = null

    override fun init() {
        register<WorldChangeEvent> { doorKey = null }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            val entity = mc.level?.getEntity(packet.id) as? ArmorStand ?: return@register

            doorKey = Pair(entity, when (entity.customName?.unformattedText) {
                "Wither Key" if highlightWither.value -> witherColor.value
                "Blood Key" if highlightBlood.value -> bloodColor.value
                else -> return@register
            })
        }

        register<RenderWorldEvent> {
            doorKey?.let { (entity, color) ->
                if (! entity.isAlive) {
                    doorKey = null
                    return@register
                }

                Render3D.renderTracer(event.ctx, entity.renderVec.add(y = 1.7), color, 2)
                Render3D.renderBox(
                    event.ctx,
                    entity.x,
                    entity.y + 1.2f,
                    entity.z,
                    width = 0.8, height = 0.8,
                    color,
                    outline = true,
                    fill = true,
                    phase = true,
                )
            }
        }
    }
}