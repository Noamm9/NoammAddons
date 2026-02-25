package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.world.entity.EquipmentSlot

object MaskTimers: Feature("Mask Cooldwon Timers, Invulnerability Timers and more") {
    private val onlyInDungeon by ToggleSetting("Dungeons Only")
    private val maskTimerStyle by DropdownSetting("Style", 0, listOf("NoammAddons", "Zyryon"))

    private val invulnerabilityTimers by ToggleSetting("Invulnerability Timers")
    private val procNotification by ToggleSetting("Proc Notification")
    private val readyNotification by ToggleSetting("Ready Notification")

    private enum class Mask(
        val displayName: String,
        val color: String,
        val cooldownTicks: Int,
        val invulnTicks: Int,
        val regex: Regex,
        val checkWorn: () -> Boolean
    ) {
        BONZO("Bonzo", "&9", 180 * 20, 3 * 20, Regex("Your (?:.+ )?Bonzo's Mask saved your life!"), {
            mc.player?.getItemBySlot(EquipmentSlot.HEAD)?.skyblockId?.contains("BONZO_MASK") == true
        }),
        SPIRIT("Spirit", "&f", 30 * 20, 3 * 20, Regex("Second Wind Activated! Your Spirit Mask saved your life!"), {
            mc.player?.getItemBySlot(EquipmentSlot.HEAD)?.skyblockId?.contains("SPIRIT_MASK") == true
        }),
        PHOENIX("Phoenix", "&c", 60 * 20, 4 * 20, Regex("Your Phoenix Pet saved you from certain death!"), {
            cacheData.getData()["pet"].toString().contains("Phoenix")
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

    private val hudElement = hudElement("Mask Timers") { context, example ->
        if (onlyInDungeon.value && ! LocationUtils.inDungeon && ! example) return@hudElement 0f to 0f

        var maxWidth = 0f
        var yOffset = 0f

        Mask.entries.forEach { mask ->
            val cd = if (example) mask.cooldownTicks / 2 else mask.cdLeft
            if (maskTimerStyle.value == 0 && cd <= 0 && ! example) return@forEach

            val text = if (maskTimerStyle.value == 0) {
                val time = if (example) mask.cooldownTicks / 40f else cd / 20f
                if (time > 0) "${mask.color}${mask.displayName} ${if(mask.displayName != Mask.PHOENIX.displayName) "Mask" else ""}: &a${time.toFixed(1)}"
                else "${mask.color}${mask.displayName} Mask: &aREADY"
            }
            else {
                val arrow = if (mask.isWorn || example) "&a>" else "&c>"
                if (cd > 0) "${mask.color}${mask.displayName} $arrow &e${(cd / 20.0).toFixed(2)}"
                else "${mask.color}${mask.displayName} $arrow &aReady"
            }

            Render2D.drawString(context, text, 0, yOffset.toInt())
            maxWidth = maxOf(maxWidth, text.width().toFloat())
            yOffset += 10f
        }
        maxWidth to yOffset
    }

    override fun init() {
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
                if (mask.regex.matches(msg)) {
                    mask.cdLeft = mask.cooldownTicks
                    if (invulnerabilityTimers.value) mask.invulnLeft = mask.invulnTicks
                    if (procNotification.value) ChatUtils.showTitle("${mask.color}${mask.displayName} Procced!")
                }
            }
        }

        register<RenderOverlayEvent> {
            if (! invulnerabilityTimers.value) return@register
            val active = Mask.entries.filter { it.invulnLeft > 0 }.maxByOrNull { it.invulnLeft } ?: return@register

            val color = if (active.invulnLeft < 20) "&c" else "&a"
            val name = active.displayName.replace("Pet", "").replace("Mask", "").trim()
            val str = "${active.color}$name: $color${(active.invulnLeft / 20.0).toFixed(1)}"

            Render2D.drawCenteredString(
                event.context, str,
                mc.window.guiScaledWidth / 2f,
                mc.window.guiScaledHeight / 3f,
                scale = 1.5f
            )
        }

        register<WorldChangeEvent> { Mask.entries.forEach { it.reset() } }
    }
}