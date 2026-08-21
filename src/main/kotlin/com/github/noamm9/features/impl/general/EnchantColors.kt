package com.github.noamm9.features.impl.general

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.mcColor
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils.inSkyblock
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.awt.Color
import java.util.regex.*

object EnchantColors: Feature("Changes the color of enchantments in items lore.") {
    private val showNumbers by ToggleSetting("Levels as Numbers").withDescription("Show levels as numbers instead of roman numerals")
    private val boldMaxLevel by ToggleSetting("Bold Max Level", true).withDescription("Make max level bold")
    private val rainbowMaxLevel by ToggleSetting("Rainbow Max Level").withDescription("Animate max level enchants with a rainbow effect")
    private val rainbowSpeed by SliderSetting("Rainbow Speed", 1.0, 0.1, 3.0, 0.1).showIf { rainbowMaxLevel.value }
    private val rainbowSaturation by SliderSetting("Rainbow Saturation", 1.0, 0.0, 1.0, 0.05).showIf { rainbowMaxLevel.value }

    private val maxLevelColor by ColorSetting("Max Level Color", Color(255, 170, 0), false).hideIf { rainbowMaxLevel.value }.section("Colors")
    private val highLevelColor by ColorSetting("High Level Color", Color(255, 170, 0), false)
    private val normalLevelColor by ColorSetting("Normal Level Color", Color(0, 170, 170), false)
    private val badLevelColor by ColorSetting("Bad Level Color", Color(170, 170, 170), false)

    private val ENCHANTMENT_PATTERN = Pattern.compile("(?<enchant>[A-Za-z][A-Za-z '\\-]+?) (?<level>[IVXLCDM]+|\\d+)(?=,\\s*|\\)|$| [\\d,]+$)")

    private val enchantments by lazy {
        DataDownloader.loadJson<Map<String, Map<String, Map<String, Any>>>>("enchants.json").flatMap { (type, enchantList) ->
            enchantList.map { (key, enchant) ->
                key to Enchantment(
                    type,
                    enchant["loreName"] as String,
                    (enchant["goodLevel"] as Number).toInt(),
                    (enchant["maxLevel"] as Number).toInt(),
                )
            }
        }.toMap()
    }

    override fun init() {
        register<ContainerEvent.Render.Tooltip> {
            if (! inSkyblock) return@register
            if (event.stack.skyblockId.isEmpty()) return@register
            val iterator = event.lore.listIterator()
            if (iterator.hasNext()) iterator.next() // Item name

            var lineIndex = 0
            while (iterator.hasNext()) {
                val originalComponent = iterator.next()
                val plainText = originalComponent.unformattedText

                if (plainText.isEmpty() || "◆" in plainText) {
                    lineIndex ++
                    continue
                }

                val parsed = parseLine(plainText) ?: run {
                    lineIndex ++
                    continue
                }

                iterator.set(buildLineComponent(parsed, lineIndex))
                lineIndex ++
            }
        }
    }

    private fun parseLine(plainText: String): List<Segment>? {
        val matcher = ENCHANTMENT_PATTERN.matcher(plainText)
        if (! matcher.find()) return null

        val segments = ArrayList<Segment>()
        var lastEnd = 0

        do {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastEnd) {
                segments.add(Segment(plainText.substring(lastEnd, start), null, 0, ""))
            }

            val enchant = enchantments[matcher.group("enchant").lowercase().remove("'")]
            val levelStr = matcher.group("level")

            if (enchant != null) {
                val level = levelStr.toIntOrNull() ?: levelStr.romanToDecimal()
                val displayLevel = if (showNumbers.value) level.toString() else levelStr
                segments.add(Segment("${enchant.loreName} $displayLevel", enchant, level, displayLevel))
            }
            else segments.add(Segment(plainText.substring(start, end), null, 0, ""))

            lastEnd = end
        } while (matcher.find())

        if (lastEnd < plainText.length) segments.add(Segment(plainText.substring(lastEnd), null, 0, ""))

        return segments.ifEmpty { null }
    }

    private fun buildLineComponent(segments: List<Segment>, lineIndex: Int): Component {
        val newLine = Component.empty().withStyle(ChatFormatting.GRAY)
        var pixelPos = 0f

        for (seg in segments) {
            if (seg.enchant == null) {
                newLine.append(seg.text)
                pixelPos += seg.text.width()
                continue
            }

            if (rainbowMaxLevel.value && seg.enchant.isMaxed(seg.level) && ! seg.enchant.isUltimate()) {
                newLine.append(buildRainbowComponent(seg.text, boldMaxLevel.value, pixelPos, lineIndex))
            }
            else newLine.append(
                Component.literal("${seg.enchant.loreName} ${seg.displayLevel}").withStyle(seg.enchant.getStyle(seg.level))
            )

            pixelPos += seg.text.width()
        }

        return newLine
    }

    private fun buildRainbowComponent(text: String, bold: Boolean, startPixelPos: Float, lineIndex: Int): Component {
        var pixelPos = startPixelPos
        val diagonalOffset = lineIndex * 100L
        val speedScaledNow = (System.currentTimeMillis() * rainbowSpeed.value).toLong()
        val result = Component.empty()

        for (char in text) {
            val charStr = char.toString()
            val time = speedScaledNow - (pixelPos * 15).toLong() - diagonalOffset
            val hue = 1f - (time % 4000L).toFloat() / 4000L
            val color = TextColor.fromRgb(Color.HSBtoRGB(hue, rainbowSaturation.value.toFloat(), 1f) and 0xFFFFFF)
            result.append(Component.literal(charStr).withStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(false)))
            pixelPos += charStr.width()
        }

        return result
    }

    private class Segment(val text: String, val enchant: Enchantment?, val level: Int, val displayLevel: String)
    private class Enchantment(val type: String, val loreName: String, val goodLevel: Int, val maxLevel: Int) {
        fun isMaxed(level: Int) = level >= maxLevel
        fun isUltimate() = type == "ULTIMATE"

        fun getStyle(level: Int): Style {
            return if (isUltimate()) Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF)).withBold(true)
            else Style.EMPTY.withColor(when {
                level >= maxLevel -> maxLevelColor.value.mcColor
                level > goodLevel -> highLevelColor.value.mcColor
                level == goodLevel -> normalLevelColor.value.mcColor
                else -> badLevelColor.value.mcColor
            }).withBold(boldMaxLevel.value && isMaxed(level))
        }
    }
}
