package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.UResolution
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.minecraft.world.entity.EquipmentSlot
import kotlin.math.roundToInt

object MaskTimers: Feature("Mask Cooldown Timers, Invulnerability Timers, and more") {
    private val onlyInDungeon by ToggleSetting("Dungeons Only")
    private val maskTimerStyle by DropdownSetting("Style", 0, listOf("NoammAddons", "Zyryon"))

    private val invulnerabilityTimers by ToggleSetting("Invulnerability Timers")
    private val procNotification by ToggleSetting("Proc Notification")
    private val readyNotification by ToggleSetting("Ready Notification")

    private val loreCdRegex = Regex("^Cooldown: ([\\d.]+)s$")

    override fun init() {
        hudElement("Mask Timers") { context, example ->
            if (onlyInDungeon.value && ! LocationUtils.inDungeon && ! example) return@hudElement 0f to 0f

            var maxWidth = 0
            var yOffset = 0

            Mask.entries.forEach { mask ->
                val cd = if (example) mask.cooldownTicks / 2 else mask.cdLeft
                if (maskTimerStyle.value == 0 && cd <= 0 && ! example) return@forEach

                val text = if (maskTimerStyle.value == 0) {
                    val time = if (example) mask.cooldownTicks / 40f else cd / 20f
                    if (time > 0) "${mask.color}${mask.displayName} ${mask.suffix}: &a${time.toFixed(1)}"
                    else "${mask.color}${mask.displayName} ${mask.suffix}: &aREADY"
                }
                else {
                    val arrow = if (mask.isWorn || example) "&a>" else "&c>"
                    if (cd > 0) "${mask.color}${mask.displayName} $arrow &e${(cd / 20.0).toFixed(2)}"
                    else "${mask.color}${mask.displayName} $arrow &aReady"
                }

                context.drawString(text, 0, yOffset)
                maxWidth = maxOf(maxWidth, text.width())
                yOffset += 10
            }

            maxWidth to yOffset
        }

        register<TickEvent.Server> {
            if (! LocationUtils.inSkyblock) return@register
            val inDungeon = LocationUtils.inDungeon

            Mask.entries.forEach { mask ->
                if (maskTimerStyle.value == 1) {
                    mask.isWorn = mask.checkWorn()
                }

                if (mask.invulnLeft > 0) mask.invulnLeft --

                if (mask.cdLeft > 0) {
                    mask.cdLeft --
                    mask.notifiedReady = false
                }
                else if (! mask.notifiedReady) {
                    mask.notifiedReady = true
                    if (readyNotification.value && (! onlyInDungeon.value || inDungeon)) {
                        ChatUtils.showTitle("${mask.color}${mask.displayName} is Ready!")
                    }
                }
            }
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.inSkyblock || (onlyInDungeon.value && ! LocationUtils.inDungeon)) return@register
            val msg = event.unformattedText
            Mask.entries.forEach { mask ->
                if (! mask.regex.matches(msg)) return@forEach

                mask.cdLeft = if (mask == Mask.BONZO) player.getItemBySlot(EquipmentSlot.HEAD).lore.firstNotNullOfOrNull { line ->
                    loreCdRegex.matchEntire(line.removeFormatting().trim())?.groupValues?.get(1)?.toDoubleOrNull()
                }?.let { (it * 20).roundToInt() } ?: mask.cooldownTicks
                else mask.cooldownTicks

                if (invulnerabilityTimers.value) mask.invulnLeft = mask.invulnTicks
                if (procNotification.value) ChatUtils.showTitle("${mask.color}${mask.displayName} Procced!")
            }
        }

        register<RenderOverlayEvent> {
            if (! invulnerabilityTimers.value) return@register
            val active = Mask.entries.filter { it.invulnLeft > 0 }.maxByOrNull { it.invulnLeft } ?: return@register

            val color = if (active.invulnLeft < 20) "&c" else "&a"
            val str = "${active.color}${active.displayName}: $color${(active.invulnLeft / 20.0).toFixed(1)}"

            event.context.drawCenteredString(
                str, UResolution.scaledWidth / 2f,
                UResolution.scaledHeight / 3f,
                scale = 1.5f
            )
        }

        register<WorldChangeEvent> { Mask.entries.forEach(Mask::reset) }
    }

    private enum class Mask(
        val displayName: String,
        val suffix: String,
        val color: String,
        val cooldownTicks: Int,
        val invulnTicks: Int,
        val regex: Regex,
        val checkWorn: () -> Boolean
    ) {
        BONZO("Bonzo", "Mask", "&9", 180 * 20, 3 * 20, Regex("Your (?:.+ )?Bonzo's Mask saved your life!"), {
            "BONZO_MASK" in player.getItemBySlot(EquipmentSlot.HEAD).skyblockId
        }),
        SPIRIT("Spirit", "Mask", "&f", 30 * 20, 3 * 20, Regex("Second Wind Activated! Your Spirit Mask saved your life!"), {
            "SPIRIT_MASK" in player.getItemBySlot(EquipmentSlot.HEAD).skyblockId
        }),
        PHOENIX("Phoenix", "Pet", "&c", 60 * 20, 4 * 20, Regex("Your Phoenix Pet saved you from certain death!"), {
            (cacheData.get()["pet"] as? JsonPrimitive)?.contentOrNull.toString().contains("Phoenix")
        });

        var cdLeft = 0
        var invulnLeft = 0
        var isWorn = false
        var notifiedReady = true

        fun reset() {
            cdLeft = 0
            invulnLeft = 0
            isWorn = false
            notifiedReady = true
        }
    }
}