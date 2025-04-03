package noammaddons.utils

import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Actionbar
import noammaddons.noammaddons.Companion.mc
import kotlin.math.roundToInt


object ActionBarParser {
    val HP_REGEX = Regex("§[c6]([\\d,]+)/([\\d,]+)❤") // §c1389/1390❤ , §62181/1161❤
    val DEF_REGEX = Regex("§a([\\d,]+)§a❈ Defense") // §a593§a❈ Defense
    val MANA_REGEX = Regex("§b([\\d,]+)/([\\d,]+)✎( Mana)?") // §b550/550✎ Mana§r
    val OVERFLOW_REGEX = Regex("§3([\\d,]+)ʬ") // §3100ʬ
    val STACKS_REGEX = Regex("§6([0-9]+[ᝐ⁑Ѫ])") // §610⁑
    val SALVATION_REGEX = Regex("T([1-3])!")
    val MANA_USAGE_REGEX = Regex("§b-[\\d,]+ Mana \\(§6.+?§b\\)|§c§lNOT ENOUGH MANA") // §b-50 Mana (§6Speed Boost§b) , §c§lNOT ENOUGH MANA
    val SECRETS_REGEX = Regex("\\s*§7(\\d+)/(\\d+) Secrets") // §76/10 Secrets§r


    val currentSpeed get() = (mc.thePlayer.capabilities.walkSpeed * 1000).roundToInt()
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onActionBarPacket(event: Actionbar) {
        val msg = event.component.formattedText
        extractPlayerStats(msg)
        effectiveHP = (currentHealth * (1 + currentDefense / 100))
    }

    private fun extractPlayerStats(input: CharSequence) {
        HP_REGEX.find(input)?.let { match ->
            currentHealth = match.groupValues[1].replace(",", "").toIntOrNull() ?: currentHealth
            maxHealth = match.groupValues[2].replace(",", "").toIntOrNull() ?: maxHealth
        }

        DEF_REGEX.find(input)?.let { match ->
            currentDefense = match.groupValues[1].replace(",", "").toIntOrNull() ?: currentDefense
        }

        MANA_REGEX.find(input)?.let { match ->
            currentMana = match.groupValues[1].replace(",", "").toIntOrNull() ?: currentMana
            maxMana = match.groupValues[2].replace(",", "").toIntOrNull() ?: maxMana
        }

        OVERFLOW_REGEX.find(input).let { match ->
            overflowMana = match?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
        }

        MANA_USAGE_REGEX.find(input)?.let { match ->
            currentMana -= match.groupValues.first().replace(",", "").toIntOrNull() ?: 0
        }

        STACKS_REGEX.find(input)?.let { match ->
            netherArmorStacks = match.groupValues[1].replace(",", "").toIntOrNull() ?: netherArmorStacks
        }

        SALVATION_REGEX.find(input)?.let { match ->
            salvation = match.groupValues[1].replace(",", "").toIntOrNull() ?: salvation
        }

        SECRETS_REGEX.find(input)?.let { match ->
            secrets = match.groupValues[1].replace(",", "").toIntOrNull() ?: secrets
            maxSecrets = match.groupValues[2].replace(",", "").toIntOrNull() ?: maxSecrets
            return
        }
        maxSecrets = null
        secrets = null
    }
}
