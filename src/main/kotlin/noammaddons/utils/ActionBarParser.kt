package noammaddons.utils

import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Actionbar
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.PlayerUtils.Player
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.roundToInt


/**
 * Normal:                     §c1390/1390❤     §a720§a❈ Defense     §b183/171✎ Mana§r
 * Normal with Skill XP:       §c1390/1390❤     §3+10.9 Combat (313,937.1/600,000)     §b183/171✎ Mana§r
 * Zombie Sword:               §c1390/1390❤     §a725§a❈ Defense     §b175/233✎ Mana    §a§lⓩⓩⓩⓩ§2§l§r
 * Zombie Sword with Skill XP: §c1390/1390❤     §3+10.9 Combat (313,948/600,000)     §b187/233✎ Mana    §a§lⓩⓩⓩⓩ§2§l§r
 * Normal with Wand:           §c1390/1390❤+§c30▅     §a724§a❈ Defense     §b97/171✎ Mana§r
 * Normal with Absorption:     §61181/1161❤     §a593§a❈ Defense     §b550/550✎ Mana§r
 * Normal with Absorp + Wand:  §61181/1161❤+§c20▆     §a593§a❈ Defense     §b501/550✎ Mana§r
 * End Race:                   §d§lTHE END RACE §e00:52.370            §b147/147✎ Mana§r
 * Woods Race:                 §A§LWOODS RACING §e00:31.520            §b147/147✎ Mana§r
 * Trials of Fire:             §c1078/1078❤   §610 DPS   §c1 second     §b421/421✎ Mana§r
 * Soulflow:                   §b421/421✎ §3100ʬ
 * Tethered + Alignment:      §a1039§a❈ Defense§a |||§a§l  T3!
 * Five stages of healing wand:     §62151/1851❤+§c120▆
 * §62151/1851❤+§c120▅
 * §62151/1851❤+§c120▄
 * §62151/1851❤+§c120▃
 * §62151/1851❤+§c120▂
 * §62151/1851❤+§c120▁
 *
 *
 * To add something new to parse, add an else-if case in [.parseActionBar] to call a method that
 * parses information from that section.
 */

object ActionBarParser {
    private const val REGEX: String =
        "((?<health>[0-9,.]+)/(?<maxHealth>[0-9,.]+)❤(?<wand>\\+(?<wandHeal>[0-9,.]+)[▆▅▄▃▂▁])?)|((?<currentDefense>[0-9,.]+)❈ Defense(?<other>( (?<align>\\|\\|\\|))?( {2}(?<Term>T[0-9,.]+!?))?.*)?)"
    private const val ManaRegex: String = """((?<num>[0-9,.]+)/(?<den>[0-9,.]+)✎ (Mana|(?<overflowMana>-?[0-9,.]+)ʬ))"""
    val currentSpeed get() = (Player !!.capabilities.walkSpeed * 1000).roundToInt()
    var currentHealth = 0
    var maxHealth = 0
    var wand: String? = null
    var currentDefense: Int = 0
    var currentMana: Int = 0
    var maxMana: Int = 0
    var overflowMana: Int = 0
    var effectiveHP: Int = 0


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onActionBarPacket(event: Actionbar) {
        val msg = event.component.unformattedText.removeFormatting()

        extractPlayerStats(msg)
        extractPlayerManaStats(msg)

        effectiveHP = (currentHealth * (1 + currentDefense / 100))
    }

    private fun extractPlayerStats(input: CharSequence) {
        val pattern = Pattern.compile(REGEX)
        val matcher: Matcher = pattern.matcher(input)

        while (matcher.find()) {
            if (matcher.group("health") != null) {
                currentHealth = matcher.group("health").replace(",", "").toIntOrNull() ?: currentHealth
                maxHealth = matcher.group("maxHealth").replace(",", "").toIntOrNull() ?: maxHealth
                wand = matcher.group("wand")
            }

            if (matcher.group("currentDefense") != null) {
                currentDefense = matcher.group("currentDefense").replace(",", "").toIntOrNull() ?: currentDefense
            }
        }
    }

    private fun extractPlayerManaStats(input: String) {
        val pattern = Pattern.compile(ManaRegex)
        val matcher = pattern.matcher(input)

        while (matcher.find()) {
            currentMana = matcher.group("num").replace(",", "").toIntOrNull() ?: currentMana
            maxMana = matcher.group("den").replace(",", "").toIntOrNull() ?: maxMana
            overflowMana = if (matcher.group("overflowMana") != null) matcher.group("overflowMana").replace(",", "").toInt() else 0
        }
    }
}