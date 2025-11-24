package noammaddons.features.impl.slayers

import gg.essential.universal.UChat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.ScoreboardUtils.cleanSB
import noammaddons.utils.ScoreboardUtils.sidebarLines
import noammaddons.utils.ThreadUtils
import noammaddons.utils.Utils.remove
import kotlin.math.ceil
import kotlin.math.roundToInt

object ExtraSlayerInfo: Feature() {
    private val regex = Regex("^\\s* (.*) Slayer LVL (.+) - Next LVL in (.+) XP!$")
    private val slayerXpMap = listOf(0, 5, 25, 100, 500, 1500)

    private val slayerCumulativeXpRequirements = mapOf(
        "Zombie" to listOf(0, 5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000),
        "Spider" to listOf(0, 5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000),
        "Wolf" to listOf(0, 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000),
        "Enderman" to listOf(0, 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000),
        "Blaze" to listOf(0, 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000),
        "Vampire" to listOf(0, 20, 75, 240, 840, 2400, 7500, 20000, 50000, 1000000)
    )

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText
        val match = regex.find(msg)?.destructured ?: return
        val slayerName = match.component1().trim()
        val level = match.component2().toInt().takeUnless { it >= 9 } ?: return
        val expToNextLevel = match.component3().remove(",").toInt()
        val message = createMessage(slayerName, level, expToNextLevel) ?: return

        ThreadUtils.scheduledTask(20) {
            message.forEach(UChat::chat)
        }
    }

    @JvmStatic
    fun bossEXP(): Int? {
        val lines = sidebarLines.map { cleanSB(it) }
        val slayerQuestIndex = lines.indexOf("Slayer Quest").takeUnless { it == - 1 } ?: return null
        val level = lines[slayerQuestIndex - 1].substringAfterLast(" ").romanToDecimal()
        val baseXp = slayerXpMap[level]
        val hasAatroxBuff = mayorData.let { (it.mayor.perks + it.minister.perk).any { it.name == "Slayer XP Buff" } }
        val bossEXP = if (hasAatroxBuff) (baseXp * 1.25).roundToInt() else baseXp

        return bossEXP
    }

    private fun createMessage(slayerName: String, level: Int, expToNextLevel: Int): List<String>? {
        if (level > 8) return null
        val bossExpValue = bossEXP() ?: return null

        val bossesLeft = ceil(expToNextLevel / bossExpValue.toDouble()).toInt()
        val grammar = if (bossesLeft == 1) "boss" else "bosses"
        val separator = "&b&m${getChatBreak("-")?.drop(20) ?: ""}"
        val lines = ArrayList<String>()

        lines.add(separator)
        lines.add("   &b&nCurrent $slayerName &a&nLevel: &6&l&n$level")
        lines.add("  ")
        lines.add("   &eNeed &b$bossesLeft $grammar &eTo reach &aLevel: &6&l${level + 1}")
        lines.add("   &aMissing XP: &d${format(expToNextLevel)}")

        if (level <= 7) calculateXpToMaxLevel(slayerName, level, expToNextLevel)?.let { xpToMaxLevel ->
            val bossesToMax = if (xpToMaxLevel > 0) ceil(xpToMaxLevel / bossExpValue.toDouble()).toInt() else 0
            val maxGrammar = if (bossesToMax == 1) "boss" else "bosses"

            lines.add(" ")
            lines.add("   &6Need &b$bossesToMax $maxGrammar &6To reach &aLevel: &6&l9")
            lines.add("   &cMissing XP: &e${format(xpToMaxLevel)}")
        }

        lines.add(separator)

        return lines.map { it.addColor() }
    }

    private fun calculateXpToMaxLevel(slayerName: String, currentLevel: Int, expToNextLevel: Int): Int? {
        val slayerRequirements = slayerCumulativeXpRequirements[slayerName] ?: return null
        var xpNeeded = expToNextLevel

        for (level in (currentLevel + 2) .. 9) {
            val levelXp = slayerRequirements[level]
            val prevLevelXp = slayerRequirements[level - 1]
            xpNeeded += (levelXp - prevLevelXp)
        }

        return xpNeeded
    }
}
