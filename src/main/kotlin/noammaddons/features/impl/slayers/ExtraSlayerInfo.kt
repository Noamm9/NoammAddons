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
    // https://regex101.com/r/Rm0FR3/1 what the fuck is this
    private val regex = Regex("^\\s* (.*) Slayer LVL (.+) - Next LVL in (.+) XP!$")
    private val slayerXpMap = mapOf(1 to 5, 2 to 25, 3 to 100, 4 to 500, 5 to 1500)


    private val slayerCumulativeXpRequirements = mapOf(
        "Zombie" to mapOf(
            1 to 5, 2 to 15, 3 to 200, 4 to 1000, 5 to 5000,
            6 to 20000, 7 to 100000, 8 to 400000, 9 to 1000000
        ),
        "Spider" to mapOf(
            1 to 5, 2 to 25, 3 to 200, 4 to 1000, 5 to 5000,
            6 to 20000, 7 to 100000, 8 to 400000, 9 to 1000000
        ),
        "Wolf" to mapOf(
            1 to 10, 2 to 30, 3 to 250, 4 to 1500, 5 to 5000,
            6 to 20000, 7 to 100000, 8 to 400000, 9 to 1000000
        ),
        "Enderman" to mapOf(
            1 to 10, 2 to 30, 3 to 250, 4 to 1500, 5 to 5000,
            6 to 20000, 7 to 100000, 8 to 400000, 9 to 1000000
        ),
        "Blaze" to mapOf(
            1 to 10, 2 to 30, 3 to 250, 4 to 1500, 5 to 5000,
            6 to 20000, 7 to 100000, 8 to 400000, 9 to 1000000
        ),
        "Vampire" to mapOf(
            1 to 20, 2 to 75, 3 to 240, 4 to 840, 5 to 2400,
            6 to 7500, 7 to 20000, 8 to 50000, 9 to 1000000
        )
    )

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText
        val match = regex.find(msg)?.destructured ?: return

        val slayerName = match.component1().trim()
        val level = match.component2().toInt()
        val expToNextLevel = match.component3().remove(",").toDouble()
        val bossesLeft = ceil(expToNextLevel / (bossEXP() ?: return)).toInt()
        val message = createMessage(slayerName, level, bossesLeft, match.component3(), expToNextLevel)

        setTimeout(1000L) {
            chat(message)
        }
    }

    @JvmStatic
    fun bossEXP(): Int? {
        val lines = sidebarLines.map { cleanSB(it) }
        val slayerQuestIndex = lines.indexOf("Slayer Quest")
        if (slayerQuestIndex == -1) return null
        val level = lines[slayerQuestIndex - 1].substringAfterLast(" ").romanToDecimal()
        val baseXp = slayerXpMap[level] ?: return null
        val hasAatroxBuff = mayorData?.let { (it.mayor.perks + it.minister.perk).any { it.name == "Slayer XP Buff" } } ?: false
        val bossEXP = if (hasAatroxBuff) (baseXp * 1.25).roundToInt() else baseXp

        return bossEXP
    }

    private fun createMessage(slayerName: String, level: Int, bossesLeft: Int, missingXP: String, expToNextLevel: Double): String {
        val grammar = if (bossesLeft == 1) "boss" else "bosses"

        val xpToMaxLevel = calculateXpToMaxLevel(slayerName, level, expToNextLevel)
        val bossesToMaxLevel = if (xpToMaxLevel > 0) ceil(xpToMaxLevel / (bossEXP()?.toDouble() ?: 1.0)).toInt() else 0
        val maxLevelGrammar = if (bossesToMaxLevel == 1) "boss" else "bosses"

        return listOf(
            "&b&m${getChatBreak("-")?.drop(20)}",
            "   &b&nCurrent $slayerName &a&nLevel: &6&l&n$level",
            "",
            "   &eNeed &b$bossesLeft $grammar &eTo reach &aLevel: &6&l${level + 1}",
            "   &aMissing XP: &d$missingXP",
            "",
            "   &6Need &b$bossesToMaxLevel $maxLevelGrammar &6To reach &c&lMAX LEVEL &6(9)",
            "   &cTotal XP to Max: &e${formatNumber(xpToMaxLevel)}",
            "&b&m${getChatBreak("-")?.drop(20)}"
        ).joinToString("\n") { it.addColor() }
    }

    private fun calculateXpToMaxLevel(slayerName: String, currentLevel: Int, expToNextLevel: Double): Int {
        if (currentLevel >= 9) return 0

        val slayerRequirements = slayerCumulativeXpRequirements[slayerName] ?: return 0


        var xpNeeded = expToNextLevel.toInt()


        for (level in (currentLevel + 2)..9) {
            val levelXp = slayerRequirements[level] ?: 0
            val prevLevelXp = slayerRequirements[level - 1] ?: 0
            xpNeeded += (levelXp - prevLevelXp)
        }

        return xpNeeded
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
            number >= 1000 -> String.format("%.1fK", number / 1000.0)
            else -> number.toString()
        }
    }
}
