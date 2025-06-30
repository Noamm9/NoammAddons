package noammaddons.ui.font

import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.utils.ChatUtils.addColor
import java.awt.Color

class TextRenderer(val fr: GlyphPageFontRenderer) {
    val FONT_HEIGHT = fr.fontHeight
    val HALF_FONT_HEIGHT = fr.fontHeight / 2

    fun getStringWidth(text: String, scale: Number = 1): Float {
        return if (text.contains("\n")) text.split("\n").maxOf { fr.getStringWidth(it.addColor()) } * scale.toFloat()
        else fr.getStringWidth(text.addColor()) * scale.toFloat()
    }

    fun getStringWidth(text: List<String>, scale: Number = 1): Float {
        return text.maxOf { fr.getStringWidth(it.addColor()) } * scale.toFloat()
    }

    fun getStringHeight(lines: List<String>, scale: Number = 1) = lines.size * FONT_HEIGHT * scale.toFloat()
    fun getStringHeight(text: String, scale: Number = 1) = text.split("\n").size * FONT_HEIGHT * scale.toFloat()

    @Suppress("NAME_SHADOWING")
    fun drawText(text: String, x: Number, y: Number, color: Color = Color.WHITE, shadow: Boolean = false) {
        val text = text.addColor()
        val (x, y) = (x.toFloat() to y.toFloat())

        if ("\n" in text) text.split("\n").forEachIndexed { i, line ->
            fr.drawString(line, x - 1, y - 3 + i * getStringHeight(line), color.rgb, shadow)
        }
        else fr.drawString("$text§r", x - 1, y - 3, color.rgb, shadow)
    }

    fun drawText(text: List<String>, x: Number, y: Number, color: Color = Color.WHITE, shadow: Boolean = false) {
        val totalHeight = getStringHeight(text)
        val startY = y.toFloat() - (totalHeight / 2)

        text.forEachIndexed { i, line ->
            val lineY = startY + (i * fr.fontHeight)
            drawText(line, x, lineY, color, shadow)
        }
    }

    fun drawCenteredText(text: String, x: Number, y: Number, color: Color = Color.WHITE, shadow: Boolean = false) {
        drawText(
            text,
            x.toFloat() - (getStringWidth(text) / 2),
            y.toFloat(),
            color,
            shadow
        )
    }

    fun drawCenteredText(text: List<String>, x: Number, y: Number, color: Color = Color.WHITE, shadow: Boolean = false) {
        val totalHeight = getStringHeight(text)
        val startY = y.toFloat() - (totalHeight / 2) // Center vertically

        text.forEachIndexed { i, line ->
            drawText(
                line,
                x.toFloat() - (getStringWidth(line) / 2),
                startY + (i * fr.fontHeight),
                color,
                shadow
            )
        }
    }

    private val padding = 2.0

    fun getWrappedLines(text: String, maxWidth: Double): List<String> {
        if (text.isEmpty() || maxWidth <= 0) return emptyList()

        val words = text.split(' ').filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()

        for (word in words) {
            val wordWidth = getStringWidth(word)
            if (wordWidth > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString().trim())
                    currentLine.clear()
                }
                var subWord = word
                while (subWord.isNotEmpty()) {
                    var k = 0
                    while (k < subWord.length && getStringWidth(subWord.substring(0, k + 1)) <= maxWidth) {
                        k ++
                    }
                    if (k == 0 && subWord.isNotEmpty()) {
                        lines.add(subWord.substring(0, 1))
                        subWord = subWord.substring(1)
                    }
                    else if (k > 0) {
                        lines.add(subWord.substring(0, k))
                        subWord = subWord.substring(k)
                    }
                    else {
                        break
                    }
                }
                continue
            }
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (getStringWidth(testLine) <= maxWidth) {
                if (currentLine.isNotEmpty()) {
                    currentLine.append(" ")
                }
                currentLine.append(word)
            }
            else {
                lines.add(currentLine.toString().trim())
                currentLine.clear()
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString().trim())
        }
        return lines
    }

    fun drawWrappedText(
        text: String,
        x: Number,
        y: Number,
        maxWidth: Number,
        color: Color = Color.WHITE,
        shadow: Boolean = false
    ): Double {
        val (dX, dY, dMaxWidth) = Triple(x.toDouble(), y.toDouble(), maxWidth.toDouble())

        if (text.isBlank() || dMaxWidth <= 0) return 0.0

        val lines = getWrappedLines(text, dMaxWidth)
        if (lines.isEmpty()) return 0.0
        val effectiveLineHeight = textRenderer.fr.fontHeight + padding
        var currentY = dY

        for (line in lines) {
            textRenderer.drawText(line, x, currentY, color, shadow)
            currentY += effectiveLineHeight
        }

        return currentY - dY
    }

    fun warpText(
        text: String,
        maxWidth: Number,
    ): Double {
        if (text.isBlank() || maxWidth.toDouble() <= 0) return 0.0
        val lines = getWrappedLines(text, maxWidth.toDouble())
        if (lines.isEmpty()) return 0.0
        return lines.size * (fr.fontHeight + padding)
    }
}