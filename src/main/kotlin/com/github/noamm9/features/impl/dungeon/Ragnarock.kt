package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.TextInputSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.USound
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

object Ragnarock: Feature("Ragnarock alerts") {
    private val alertCancelled by ToggleSetting("Alert Cancelled", true).withDescription("plays a sound when the Ragnarock is cancelled")
    private val strengthGainedMessage by ToggleSetting("Strength Gained", true).withDescription("Prints in chat how much strength you gained from the Ragnarock")
    private val m7Alert by ToggleSetting("M7 Dragon Alert").withDescription("Shows on screen when to use Ragnarock in M7-P5")
    private val m7AlertText by TextInputSetting("M7 Alert Text", "rag").showIf { m7Alert.value }.withDescription("The text that shows on screen. Supports color codes")

    private const val m7RagMessage = "[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you."
    private val cancelRegex = Regex("Ragnarock was cancelled due to (?:being hit|taking damage)!")
    private val strengthRegex = Regex("Strength: \\+(\\d+)")

    private val sounds = listOf(
        0L to 1.22f, 120L to 1.13f, 240L to 1.29f,
        400L to 1.60f, 520L to 1.60f, 640L to 1.72f,
        780L to 1.89f
    )

    private var m7AlertTicks = 0

    override fun init() {
        hudElement(
            "Ragnarock Alert",
            enabled = { m7Alert.value },
            shouldDraw = { ticker.isActive },
        ) { ctx, _ ->
            val text = m7AlertText.value
            ctx.drawString(text, 0, 0)
            return@hudElement text.width() to 9f
        } defaults {
            x = Resolution.width / 2f
            y = Resolution.height.let { it / 2f - (it * 0.056f) }
            scale = 2f
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! strengthGainedMessage.value) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            if (packet.sound.value().location.path != "entity.wolf.death") return@register
            if (packet.pitch.toDouble() == 1.4920635) return@register
            if (player.mainHandItem.skyblockId != "RAGNAROCK_AXE") return@register
            val strengthLine = player.mainHandItem.lore.map { it.removeFormatting() }.find { it.startsWith("Strength:") } ?: return@register
            val match = strengthRegex.find(strengthLine) ?: return@register
            val baseStrength = match.groupValues[1].toIntOrNull() ?: return@register
            ChatUtils.modMessage("&fGained strength: &c${(baseStrength * 1.5).roundToInt()}")
        }

        register<ChatMessageEvent> {
            if (m7Alert.value && LocationUtils.F7Phase == 5 && event.unformattedText == m7RagMessage) {
                if (DungeonListener.thePlayer?.clazz.equalsOneOf(DungeonClass.Tank, DungeonClass.Healer)) return@register
                m7AlertTicks = 40
                ticker.register()
                for ((delay, pitch) in sounds) ThreadUtils.setTimeout(delay) {
                    USound.playSoundStatic(SoundEvents.NOTE_BLOCK_PLING, 0.25f, pitch)
                }
            }
            else if (alertCancelled.value && event.unformattedText.matches(cancelRegex)) {
                ChatUtils.showTitle(subtitle = "&cRagnarock Cancelled")
                USound.playSoundStatic(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
            }
        }
    }

    private val ticker = EventBus.listener<TickEvent.Start> {
        m7AlertTicks --
        if (m7AlertTicks <= 0) listener.unregister()
    }
}