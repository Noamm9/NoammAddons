package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.section
import com.github.noamm9.ui.clickgui.componnents.showIf
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
import com.github.noamm9.utils.render.RenderHelper.renderX
import com.github.noamm9.utils.render.RenderHelper.renderY
import com.github.noamm9.utils.render.RenderHelper.renderZ
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.*

object DoorKeys: Feature("ESP box & Tracer for wither doors and blood door") {
    private val highlightWither by ToggleSetting("Wither Key").section("Keys")
    private val highlightBlood by ToggleSetting("Blood Key")
    private val witherColor by ColorSetting("Wither Key Color", Color.BLACK.withAlpha(60)).showIf { highlightWither.value }.section("Colors")
    private val bloodColor by ColorSetting("Blood Key Color", Color.RED.withAlpha(60)).showIf { highlightBlood.value }

    private val witherKeyId = UUID.fromString("2865274b-3097-394e-8149-ec629c72d850")
    private val bloodKeyId = UUID.fromString("73f6d1f9-df41-3d1d-b98c-e1442d915885")
    private var doorKey: Pair<Entity, Color>? = null

    override fun init() {
        register<WorldChangeEvent> { doorKey = null }

        register<MainThreadPacketReceivedEvent.Post> {
            val packet = event.packet as? ClientboundSetEquipmentPacket ?: return@register
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            val entity = mc.level?.getEntity(packet.entity) as? ArmorStand ?: return@register
            if (packet.slots.size != 1) return@register
            val entry = packet.slots.firstOrNull() ?: return@register
            if (entry.first !== EquipmentSlot.HEAD) return@register

            val item = entry.second ?: return@register
            if (item.item !== Items.PLAYER_HEAD) return@register

            val profile = item.get(DataComponents.PROFILE) ?: return@register
            val id = profile.partialProfile().id ?: return@register

            doorKey = Pair(entity, when (id) {
                witherKeyId -> witherColor.value
                bloodKeyId -> bloodColor.value
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
                    entity.renderX,
                    entity.renderY + 1.5f,
                    entity.renderZ,
                    width = 0.8, height = 0.8,
                    color,
                    outline = true,
                    fill = true,
                    phase = true,
                )
            }

            register<WorldChangeEvent> {
                doorKey = null
            }
        }
    }
}