package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

object TerminalHitboxes: Feature("Highlights the interactable hitboxes of the terminals in F7/M7") {
    private val mode by DropdownSetting("Mode", 1, listOf("Outline", "Fill", "Filled Outline"))
    private val phase by ToggleSetting("Phase", false)
    private val fillColor by ColorSetting("Fill Color", Color.orange).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor).hideIf { mode.value == 1 }

    private val terminalPositions = listOf(
        listOf(vec(110, 113, 73), vec(110, 119, 79), vec(90, 112, 92), vec(90, 122, 101)),
        listOf(vec(68, 109, 122), vec(59, 119, 123), vec(47, 109, 122), vec(39, 108, 142), vec(40, 124, 123)),
        listOf(vec(- 2, 109, 112), vec(- 2, 119, 93), vec(18, 123, 93), vec(- 2, 109, 77)),
        listOf(vec(41, 109, 30), vec(44, 121, 30), vec(67, 109, 30), vec(72, 114, 47))
    )

    private val cachedTerminals = mutableMapOf<Int, MutableSet<ArmorStand>>()

    override fun init() {
        register<WorldChangeEvent> { cachedTerminals.clear() }

        register<MainThreadPacketReceivedEvent.Post> {
            if (LocationUtils.dungeonFloorNumber != 7 || LocationUtils.F7Phase != 3) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            val entity = level.getEntity(packet.id) as? ArmorStand ?: return@register
            val name = entity.customName?.unformattedText

            if (name == "Inactive Terminal") {
                for ((section, posList) in terminalPositions.withIndex()) {
                    if (posList.none { entity.distanceToSqr(it) <= 1.5 }) continue
                    cachedTerminals.getOrPut(section + 1) { mutableSetOf() }.add(entity)
                }
            }
            else if (name == "Terminal Active") {
                for (i in terminalPositions.indices) {
                    cachedTerminals[i + 1]?.remove(entity)
                }
            }
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon || LocationUtils.F7Phase != 3) return@register
            val section = LocationUtils.P3Section ?: return@register
            val terminalsToRender = cachedTerminals[section]?.takeUnless(Collection<*>::isEmpty) ?: return@register

            val drawFill = mode.value == 1 || mode.value == 2
            val drawOutline = mode.value == 0 || mode.value == 2

            for (entity in terminalsToRender) {
                event.ctx.renderBoxBounds(
                    entity.renderBoundingBox,
                    outlineColor.value,
                    fillColor.value,
                    outline = drawOutline,
                    fill = drawFill,
                    phase = phase.value,
                    lineWidth = 2.0
                )
            }
        }
    }
}