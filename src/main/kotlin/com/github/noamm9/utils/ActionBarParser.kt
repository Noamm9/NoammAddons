package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.Utils.remove
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import kotlin.math.roundToInt

object ActionBarParser {
    val HP_REGEX = Regex("§[c6]([\\d,]+)\\/([\\d,]+)❤") // §c1389/1390❤ , §62181/1161❤
    val DEF_REGEX = Regex("§a([\\d,]+)§a❈ Defense") // §a593§a❈ Defense
    val MANA_REGEX = Regex("§b([\\d,]+)/([\\d,]+)✎( Mana)?") // §b550/550✎ Mana§r
    val OVERFLOW_REGEX = Regex("§3([\\d,]+)ʬ") // §3100ʬ
    val STACKS_REGEX = Regex("§6([0-9]+[ᝐ⁑Ѫ])") // §610⁑
    val SALVATION_REGEX = Regex("T([1-3])!")
    val MANA_USAGE_REGEX = Regex("§b-[\\d,]+ Mana \\(§6.+?§b\\)|§c§lNOT ENOUGH MANA") // §b-50 Mana (§6Speed Boost§b) , §c§lNOT ENOUGH MANA
    val SECRETS_REGEX = Regex("\\s*§7(\\d+)/(\\d+) Secrets") // §76/10 Secrets§r

    val currentSpeed get() = ((mc.player?.abilities?.walkingSpeed ?: 0.1f) * 1000).roundToInt()
    var currentHealth = 0
    var maxHealth = 0
    var currentDefense = 0
    var currentMana = 0
    var maxMana = 0
    var overflowMana = 0
    var effectiveHP = 0
    var netherArmorStacks = 0
    var salvation = 0
    var secrets: Int? = 0
    var maxSecrets: Int? = 0

    fun init() {
        register<MainThreadPacketReceivedEvent.Post>(EventPriority.HIGHEST) {
            if (! LocationUtils.inSkyblock) return@register
            if (event.packet is ClientboundSystemChatPacket) {
                if (event.packet.overlay()) {
                    extractPlayerStats(event.packet.content.formattedText)
                    effectiveHP = (currentHealth * (1 + currentDefense / 100))
                }
            }
            else if (event.packet is ClientboundSetActionBarTextPacket) {
                extractPlayerStats(event.packet.text.formattedText)
                effectiveHP = (currentHealth * (1 + currentDefense / 100))
            }
        }
    }

    private fun extractPlayerStats(input: String) {
        HP_REGEX.find(input)?.let { match ->
            currentHealth = match.groupValues[1].remove(",").toIntOrNull() ?: currentHealth
            maxHealth = match.groupValues[2].remove(",").toIntOrNull() ?: maxHealth
        }

        DEF_REGEX.find(input)?.let { match ->
            currentDefense = match.groupValues[1].remove(",").toIntOrNull() ?: currentDefense
        }

        MANA_REGEX.find(input)?.let { match ->
            currentMana = match.groupValues[1].remove(",").toIntOrNull() ?: currentMana
            maxMana = match.groupValues[2].remove(",").toIntOrNull() ?: maxMana
        }

        OVERFLOW_REGEX.find(input).let { match ->
            overflowMana = match?.groupValues?.get(1)?.remove(",")?.toIntOrNull() ?: 0
        }

        MANA_USAGE_REGEX.find(input)?.let { match ->
            currentMana -= match.groupValues.first().remove(",").toIntOrNull() ?: 0
        }

        STACKS_REGEX.find(input)?.let { match ->
            netherArmorStacks = match.groupValues[1].remove(",").toIntOrNull() ?: netherArmorStacks
        }

        SALVATION_REGEX.find(input)?.let { match ->
            salvation = match.groupValues[1].remove(",").toIntOrNull() ?: salvation
        }

        SECRETS_REGEX.takeIf { LocationUtils.inDungeon }?.find(input)?.let { match ->
            secrets = match.groupValues[1].remove(",").toIntOrNull() ?: secrets
            maxSecrets = match.groupValues[2].remove(",").toIntOrNull() ?: maxSecrets


            ScanUtils.getRoomGraf(mc.player !!.position()).let { (gx, gy) ->
                val room = DungeonInfo.dungeonList[gy * 11 + gx] as? Room ?: return
                if (room.data.name == "Unknown") return

                if (room.uniqueRoom?.foundSecrets != secrets && room.data.secrets == maxSecrets) {
                    room.uniqueRoom?.foundSecrets = secrets !!
                }
            }

            return
        }

        maxSecrets = null
        secrets = null
    }
}