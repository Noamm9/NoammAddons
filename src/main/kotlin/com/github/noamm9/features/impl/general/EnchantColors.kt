package com.github.noamm9.features.impl.general

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ColorUtils.mcColor
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.NumbersUtils.times
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils.inSkyblock
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.awt.Color
import java.util.regex.*

object EnchantColors: Feature("Changes the color of enchantments in items lore.") {
    private val showNumbers by ToggleSetting("Levels as Numbers").withDescription("Show levels as numbers instead of roman numerals")
    private val rainbowMaxLevel by ToggleSetting("Rainbow Max Level").withDescription("Animate max level enchants with a rainbow effect")
    private val rainbowSpeed by SliderSetting("Rainbow Speed", 1.5, 0.1, 3, 0.1).showIf { rainbowMaxLevel.value }
    private val transitionSpeed by SliderSetting("Color Transition",15,5,30,1).withDescription("Controls how quickly colors transitions across the enchants, Lower values create slower transitions").showIf { rainbowMaxLevel.value }
    private val maxLevelColor by ColorSetting("Max Level Color", Color(255, 170, 0), false).hideIf { rainbowMaxLevel.value }
    private val highLevelColor by ColorSetting("High Level Color", Color(255, 170, 0), false)
    private val normalLevelColor by ColorSetting("Normal Level Color", Color(0, 170, 170), false)
    private val badLevelColor by ColorSetting("Bad Level Color", Color(170, 170, 170), false)
    private val boldMaxLevel by ToggleSetting("Bold Max Level", true).withDescription("Make max level bold")

    private const val BASE_CYCLE_MS = 4000L
    private const val DIAGONAL_LINE_MS = 100L

    private val ENCHANTMENT_PATTERN = Pattern.compile("(?<enchant>[A-Za-z][A-Za-z '\\-]+?) (?<level>[IVXLCDM]+|\\d+)(?=,\\s*|\\)|$| [\\d,]+$)")

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
            if (iterator.hasNext()) iterator.next() // Item name

            val rainbowNow = System.currentTimeMillis()
            val font = NoammAddons.mc.font
            var lineIndex = 0

            while (iterator.hasNext()) {
                val originalComponent = iterator.next()
                val plainText = originalComponent.string.removeFormatting()
                if (plainText.isEmpty() || "◆" in plainText) {
                    lineIndex ++; continue
                }

                val matcher = ENCHANTMENT_PATTERN.matcher(plainText)
                if (! matcher.find()) {
                    lineIndex ++; continue
                }

                val newLine = Component.empty().withStyle(ChatFormatting.GRAY)
                var lastEnd = 0
                var foundEnchantment = false
                var rainbowPixelPos = 0f

                do {
                    val start = matcher.start()
                    val end = matcher.end()

                    if (start > lastEnd) {
                        val skipped = plainText.substring(lastEnd, start)
                        newLine.append(skipped)
                        rainbowPixelPos += font.width(skipped)
                    }

                    val nameKey = matcher.group("enchant").lowercase().replace("'", "")
                    val levelStr = matcher.group("level")

                    val enchantData = enchantments[nameKey]
                    if (enchantData != null) {
                        foundEnchantment = true
                        val level = levelStr.toIntOrNull() ?: levelStr.romanToDecimal()
                        val displayLevel = if (showNumbers.value) level.toString() else levelStr
                        val isMax = level >= enchantData.maxLevel

                        if (isMax && rainbowMaxLevel.value && enchantData.type != "ULTIMATE") {
                            val combined = "${enchantData.loreName} $displayLevel"
                            newLine.append(buildRainbowComponent(combined, boldMaxLevel.value, rainbowPixelPos, lineIndex, rainbowNow, font))
                            rainbowPixelPos += font.width(combined)
                        }
                        else {
                            val style = enchantData.getStyle(level)
                            newLine.append(Component.literal(enchantData.loreName).withStyle(style))
                            newLine.append(" ")
                            newLine.append(Component.literal(displayLevel).withStyle(style))
                            rainbowPixelPos += font.width("${enchantData.loreName} $displayLevel")
                        }
                    }
                    else {
                        val skipped = plainText.substring(start, end)
                        newLine.append(skipped)
                        rainbowPixelPos += font.width(skipped)
                    }

                    lastEnd = end
                } while (matcher.find())

                if (lastEnd < plainText.length) {
                    newLine.append(plainText.substring(lastEnd))
                }

                if (foundEnchantment) iterator.set(newLine)
                lineIndex ++
            }
        }
    }

    private fun colorAt(time: Long): Int {
        val hue = 1f - (time % BASE_CYCLE_MS).toFloat() / BASE_CYCLE_MS
        return Color.HSBtoRGB(hue, 1f, 1f) and 0xFFFFFF
    }

    private fun buildRainbowComponent(text: String, bold: Boolean, startPixelPos: Float, lineIndex: Int, now: Long, font: net.minecraft.client.gui.Font): MutableComponent {
        val result: MutableComponent = Component.empty()
        var pixelPos = startPixelPos
        val diagonalOffset = lineIndex * DIAGONAL_LINE_MS
        val speedScaledNow = (now * rainbowSpeed.value).toLong()

        text.forEach { char ->
            val charStr = char.toString()
            val time = speedScaledNow - (pixelPos * transitionSpeed.value).toLong() - diagonalOffset
            val rgb = colorAt(time)
            result.append(
                Component.literal(charStr)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(bold).withItalic(false))
            )
            pixelPos += font.width(charStr)
        }
        return result
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
                Style.EMPTY.withColor(color).withBold(level >= maxLevel && boldMaxLevel.value)
            }
        }
    }
}