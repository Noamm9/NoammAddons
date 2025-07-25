package noammaddons.features.impl.general

import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.RenderHelper.getColorCode
import noammaddons.utils.WebUtils.fetchJson
import java.awt.Color
import java.util.regex.Pattern


object EnchantmentsColors: Feature() {
    private val ENCHANTMENT_PATTERN: Pattern = Pattern.compile("(?<enchant>[A-Za-z][A-Za-z -]+) (?<levelNumeral>[IVXLCDM]+)(?=, |$| [\\d,]+$)")
    val enchantments = mutableListOf<Enchantment>()

    private val showNumbers by ToggleSetting("Show Enchantment Levels as Numbers")
    private val maxLevelColor by ColorSetting("Max Level Enchantment (Bold) Color", Color(255, 170, 0), false)
    private val highLevelColor by ColorSetting("High Level Enchantment Color", Color(255, 170, 0), false)
    private val normalLevelColor by ColorSetting("Normal Level Enchantment Color", Color(0, 170, 170), false)
    private val badLevelColor by ColorSetting("Bad Level Enchantment Color", Color(170, 170, 170), false)

    override fun init() {
        fetchJson<Map<String, Map<String, Map<String, Any?>>>>("https://raw.githubusercontent.com/Fix3dll/SkyblockAddons/refs/heads/main/src/main/resources/enchants.json") { enchantmentMap ->
            runCatching {
                val mappedList = enchantmentMap.flatMap { (type, innerMap) ->
                    innerMap.map { (key, rawDataMap) ->
                        val goodLevel = (rawDataMap["goodLevel"] as Double).toInt()
                        val loreName = rawDataMap["loreName"] as String
                        val maxLevel = (rawDataMap["maxLevel"] as Double).toInt()
                        val nbtName = rawDataMap["nbtName"] as String
                        val nbtNum = rawDataMap["nbtNum"] as? String
                        val statLabel = rawDataMap["statLabel"] as? String
                        val stackLevel = (rawDataMap["stackLevel"] as? List<*>)?.mapNotNull { (it as? Double)?.toInt() }

                        Enchantment(key, type, goodLevel, loreName, maxLevel, nbtName, nbtNum, stackLevel, statLabel)
                    }
                }

                enchantments.clear()
                enchantments.addAll(mappedList)
                Logger.info("Successfully loaded ${enchantments.size} enchantments data.")
            }.onFailure {
                Logger.error("Failed to parse fetched enchantment data.", it)
            }
        }
    }


    @SubscribeEvent
    fun onToolTipEvent(event: ItemTooltipEvent) {
        if (! inSkyblock) return
        val lore = event.toolTip.map { it.removeFormatting() }.withIndex()

        for ((i, line) in lore) {
            if ("◆" in line) continue
            val m = ENCHANTMENT_PATTERN.matcher(line)
            var str = line
            while (m.find()) {
                val name = m.group("enchant")
                val level = m.group("levelNumeral")
                val enchantData = enchantments.find { it.key == name.lowercase() } ?: continue
                str = str.replace(
                    "$name $level",
                    "&r${
                        enchantData.getFormat(level.romanToDecimal()) + enchantData.loreName
                    } ${
                        if (! showNumbers) level else level.romanToDecimal()
                    }"
                )
            }
            if (str != line) event.toolTip[i] = str.addColor()
        }
    }


    data class Enchantment(
        val key: String,
        val type: String,
        val goodLevel: Int,
        val loreName: String,
        val maxLevel: Int,
        val nbtName: String,
        val nbtNum: String? = null,
        val stackLevel: List<Int>? = null,
        val statLabel: String? = null
    ) {
        fun getFormat(level: Int) = when {
            type == "ULTIMATE" -> "§d§l"
            level >= maxLevel -> "${getColorCode(maxLevelColor)}&l"
            level > goodLevel -> getColorCode(highLevelColor)
            level == goodLevel -> getColorCode(normalLevelColor)
            else -> getColorCode(badLevelColor)
        }
    }
}