package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.utils.ColorUtils.mcColor
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils.inSkyblock
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.awt.Color
import java.util.regex.Pattern

object EnchantColors: Feature() {
    private val showNumbers by ToggleSetting("Levels as Numbers").withDescription("Show levels as numbers instead of roman numerals")
    private val maxLevelColor by ColorSetting("Max Level Color", Color(255, 170, 0), false)
    private val highLevelColor by ColorSetting("High Level Color", Color(255, 170, 0), false)
    private val normalLevelColor by ColorSetting("Normal Level Color", Color(0, 170, 170), false)
    private val badLevelColor by ColorSetting("Bad Level Color", Color(170, 170, 170), false)
    private val boldMaxEnchants by ToggleSetting("Bold Max Enchants")

    private val ENCHANTMENT_PATTERN = Pattern.compile("(?<enchant>[A-Za-z][A-Za-z -]+) (?<levelNumeral>[IVXLCDM]+)(?=, |$| [\\d,]+$)")

    val enchantments by lazy {
        DataDownloader.loadJson<Map<String, Map<String, Map<String, Any?>>>>("enchants.json").flatMap { (type, innerMap) ->
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
        }.associateBy { it.key }
    }

    override fun init() {
        register<ContainerEvent.Render.Tooltip> {
            if (! inSkyblock) return@register
            if (event.stack.skyblockId.isEmpty()) return@register
            val iterator = event.lore.listIterator()

            while (iterator.hasNext()) {
                val originalComponent = iterator.next()
                val plainText = originalComponent.string

                if (plainText.isEmpty() || "◆" in plainText) continue

                val matcher = ENCHANTMENT_PATTERN.matcher(plainText)
                if (! matcher.find()) continue

                val newLine = Component.empty().withStyle(ChatFormatting.GRAY)
                var lastEnd = 0

                do {
                    val start = matcher.start()
                    val end = matcher.end()

                    if (start > lastEnd) {
                        newLine.append(plainText.substring(lastEnd, start))
                    }

                    val nameKey = matcher.group("enchant").lowercase()
                    val levelStr = matcher.group("levelNumeral")

                    val enchantData = enchantments[nameKey]

                    if (enchantData != null) {
                        val level = levelStr.romanToDecimal()
                        val style = enchantData.getStyle(level)

                        newLine.append(Component.literal(enchantData.loreName).withStyle(style))
                        newLine.append(" ")

                        val displayLevel = if (showNumbers.value) level.toString() else levelStr
                        newLine.append(Component.literal(displayLevel).withStyle(style))
                    }
                    else newLine.append(plainText.substring(start, end))

                    lastEnd = end
                } while (matcher.find())

                if (lastEnd < plainText.length) {
                    newLine.append(plainText.substring(lastEnd))
                }

                iterator.set(newLine)
            }
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
        fun getStyle(level: Int): Style {
            return if (type == "ULTIMATE") Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF)).withBold(true)
            else {
                val color = when {
                    level >= maxLevel -> maxLevelColor.value.mcColor
                    level > goodLevel -> highLevelColor.value.mcColor
                    level == goodLevel -> normalLevelColor.value.mcColor
                    else -> badLevelColor.value.mcColor
                }

                Style.EMPTY.withColor(color).withBold(level >= maxLevel && boldMaxEnchants.value)
            }
        }
    }
}