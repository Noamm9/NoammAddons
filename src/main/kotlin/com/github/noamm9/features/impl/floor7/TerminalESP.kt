package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.lerpColor
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

object TerminalESP: Feature(
    "Highlights the interactable hitboxes of the terminals in F7/M7",
    //? if !cheat {
    /*name = "Terminal Highlight",*/
    //? }
    jsonName = "Terminal ESP"
) {
    private val mode by DropdownSetting("Mode", 1, listOf("Outline", "Fill", "Filled Outline"))
    private val phase by ToggleSetting("Phase", false)
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor).hideIf { mode.value == 1 }

    private val flashOnHit by ToggleSetting("Flash On Click", true)
    private val flashColor by ColorSetting("Flash Color", Color.RED).hideIf { ! flashOnHit.value }

    private val terminalPositions = listOf(
        listOf(vec(110, 113, 73), vec(110, 119, 79), vec(90, 112, 92), vec(90, 122, 101)),
        listOf(vec(68, 109, 122), vec(59, 119, 123), vec(47, 109, 122), vec(39, 108, 142), vec(40, 124, 123)),
        listOf(vec(- 2, 109, 112), vec(- 2, 119, 93), vec(18, 123, 93), vec(- 2, 109, 77)),
        listOf(vec(41, 109, 30), vec(44, 121, 30), vec(67, 109, 30), vec(72, 114, 47))
    )

    private val terminals = mutableMapOf<ArmorStand, HitboxInfo>()

    override fun init() {
        register<WorldChangeEvent> { terminals.clear() }

        register<MainThreadPacketReceivedEvent.Post> {
            if (LocationUtils.dungeonFloorNumber != 7 || LocationUtils.F7Phase != 3) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            val entity = level.getEntity(packet.id) as? ArmorStand ?: return@register
            val name = entity.customName?.unformattedText

            if (name == "Inactive Terminal") {
                for ((section, posList) in terminalPositions.withIndex()) {
                    if (posList.none { entity.distanceToSqr(it) <= 1.5 }) continue
                    terminals.putIfAbsent(entity, HitboxInfo(section + 1))
                }
            }
            else if (name == "Terminal Active") {
                terminals.remove(entity)
            }
        }

        register<PacketEvent.Sent> {
            if (! flashOnHit.value) return@register
            val packet = event.packet as? ServerboundInteractPacket ?: return@register
            val entity = level.getEntity(packet.entityId) as? ArmorStand ?: return@register
            terminals[entity]?.hit()
        }

        register<PacketEvent.Sent> {
            if (! flashOnHit.value) return@register
            val packet = event.packet as? ServerboundAttackPacket ?: return@register
            val entity = level.getEntity(packet.entityId) as? ArmorStand ?: return@register
            terminals[entity]?.hit()
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon || LocationUtils.F7Phase != 3) return@register
            val section = LocationUtils.P3Section ?: return@register

            val drawFill = mode.value == 1 || mode.value == 2
            val drawOutline = mode.value == 0 || mode.value == 2

            for ((entity, info) in terminals) {
                if (info.section != section) continue
                val progress = if (flashOnHit.value) info.update() else 0f

                val currentOutline = if (progress > 0f) lerpColor(outlineColor.value, flashColor.value, progress) else outlineColor.value
                val currentFill = if (progress > 0f) lerpColor(fillColor.value, flashColor.value, progress) else fillColor.value

                event.ctx.renderBoxBounds(
                    entity.renderBoundingBox,
                    currentOutline.withAlpha(outlineColor.value.alpha),
                    currentFill.withAlpha(fillColor.value.alpha),
                    outline = drawOutline,
                    fill = drawFill,
                    phase = phase.value,
                    lineWidth = 2.0
                )
            }
        }
    }

    private data class HitboxInfo(val section: Int) {
        private val flashAnimation = Animation(FLASH_ANIM_MS)
        private var lastHitTime = 0L

        companion object {
            const val FLASH_ANIM_MS = 200L
            const val FLASH_HOLD_MS = 250L
        }

        fun hit() {
            lastHitTime = System.currentTimeMillis()
        }

        fun update(): Float {
            val target = if (System.currentTimeMillis() - lastHitTime < FLASH_HOLD_MS) 1f else 0f
            flashAnimation.update(target)
            return flashAnimation.value
        }
    }
}