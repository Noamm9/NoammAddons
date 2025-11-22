package noammaddons.features.impl.slayers


import gg.essential.universal.UChat.chat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.ScoreboardUtils.cleanSB
import noammaddons.utils.ScoreboardUtils.sidebarLines
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.remove
import kotlin.math.ceil
import kotlin.math.roundToInt

object ExtraSlayerInfo: Feature() {
    // https://regex101.com/r/Rm0FR3/1
    private val regex = Regex("^\\s* (.*) Slayer LVL (.+) - Next LVL in (.+) XP!$")
    private val slayerXpMap = mapOf(1 to 5, 2 to 25, 3 to 100, 4 to 500, 5 to 1500)

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText
        val match = regex.find(msg)?.destructured ?: return

        val slayerName = match.component1().trim()
        val level = match.component2().toInt()
        val exp = match.component3().remove(",").toDouble()
        val bossesLeft = ceil(exp / (bossEXP() ?: return)).toInt()
        val message = createMessage(slayerName, level, bossesLeft, match.component3())

        setTimeout(1000L) {
            chat(message)
        }
    }

    @JvmStatic
    fun bossEXP(): Int? {
        val lines = sidebarLines.map { cleanSB(it) }
        val slayerQuestIndex = lines.indexOf("Slayer Quest")
        if (slayerQuestIndex == - 1) return null
        val level = lines[slayerQuestIndex - 1].substringAfterLast(" ").romanToDecimal()
        val baseXp = slayerXpMap[level] ?: return null
        val hasAatroxBuff = mayorData?.let { (it.mayor.perks + it.minister.perk).any { it.name == "Slayer XP Buff" } } ?: false
        val bossEXP = if (hasAatroxBuff) (baseXp * 1.25).roundToInt() else baseXp

        return bossEXP
    }

    private fun createMessage(slayerName: String, level: Int, bossesLeft: Int, missingXP: String): String {
        val grammar = if (bossesLeft == 1) "boss" else "bosses"
        return listOf(
            "&b&m${getChatBreak("-")?.drop(20)}",
            "   &b&nCurrent&r &n$slayerName&r &a&nLevel&r: &6&l&n$level&r",
            "",
            "   &eNeed &b$bossesLeft $grammar &eTo reach &aLevel: &6&l${level + 1}",
            "   &aMissing XP: &d$missingXP",
            "&b&m${getChatBreak("-")?.drop(20)}"
        ).joinToString("\n") { it.addColor() }
    }
}
