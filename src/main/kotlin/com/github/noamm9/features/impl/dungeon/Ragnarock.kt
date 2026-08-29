package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import gg.essential.universal.USound
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

object Ragnarock: Feature("Ragnarock alerts") {
    private val alertCancelled by ToggleSetting("Alert Cancelled", true)
    private val strengthGainedMessage by ToggleSetting("Strength Gained", true)
    private val m7Alert by ToggleSetting("M7 Dragon Alert")

    private const val m7RagMessage = "[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you."
    private val cancelRegex = Regex("Ragnarock was cancelled due to (?:being hit|taking damage)!")
    private val strengthRegex = Regex("Strength: \\+(\\d+)")

    private val sounds = listOf(
        0L to 1.22f, 120L to 1.13f, 240L to 1.29f,
        400L to 1.60f, 520L to 1.60f, 640L to 1.72f,
        780L to 1.89f
    )

    override fun init() {
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
                ChatUtils.showTitle("rag")
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
}