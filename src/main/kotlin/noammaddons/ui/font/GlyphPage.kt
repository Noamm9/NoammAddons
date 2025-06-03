package noammaddons.ui.font

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import noammaddons.noammaddons
import org.lwjgl.opengl.GL11
import java.awt.*
import java.awt.image.BufferedImage
import kotlin.math.*

/*
class GlyphPage(private val font: Font, private val antiAliasing: Boolean, private val fractionalMetrics: Boolean) {
    private var imgSize = 0
    var maxFontHeight: Int = - 1
        private set
    private val glyphCharacterMap = HashMap<Char, Glyph>()

    private var bufferedImage: BufferedImage? = null
    private var loadedTexture: DynamicTexture? = null

    fun generateGlyphPage(chars: CharArray) {
        var maxWidth = - 1.0
        var maxHeight = - 1.0

        val affineTransform = AffineTransform()
        val fontRenderContext = FontRenderContext(affineTransform, antiAliasing, fractionalMetrics)

        for (ch in chars) {
            val bounds = font.getStringBounds(ch.toString(), fontRenderContext)

            if (maxWidth < bounds.width) maxWidth = bounds.width
            if (maxHeight < bounds.height) maxHeight = bounds.height
        }

        maxWidth += 2.0
        maxHeight += 2.0

        imgSize = ceil(max(
            ceil(sqrt(maxWidth * maxWidth * chars.size) / maxWidth),
            ceil(sqrt(maxHeight * maxHeight * chars.size) / maxHeight)
        ) * max(maxWidth, maxHeight)
        ).toInt() + 1

        bufferedImage = BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB)

        val g = bufferedImage !!.graphics as Graphics2D

        g.font = font
        // Set Color to Transparent
        g.color = Color(255, 255, 255, 0)
        // Set the image background to transparent
        g.fillRect(0, 0, imgSize, imgSize)

        g.color = Color.white

        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, if (fractionalMetrics) RenderingHints.VALUE_FRACTIONALMETRICS_ON else RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, if (antiAliasing) RenderingHints.VALUE_ANTIALIAS_OFF else RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, if (antiAliasing) RenderingHints.VALUE_TEXT_ANTIALIAS_ON else RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)

        val fontMetrics = g.fontMetrics

        var currentCharHeight = 0
        var posX = 0
        var posY = 1

        for (ch in chars) {
            val glyph = Glyph()

            val bounds = fontMetrics.getStringBounds(ch.toString(), g)

            glyph.width = bounds.bounds.width + 8
            glyph.height = bounds.bounds.height

            if (posX + glyph.width >= imgSize) {
                posX = 0
                posY += currentCharHeight
                currentCharHeight = 0
            }

            glyph.x = posX
            glyph.y = posY

            if (glyph.height > maxFontHeight) maxFontHeight = glyph.height
            if (glyph.height > currentCharHeight) currentCharHeight = glyph.height

            g.drawString(ch.toString(), posX + 2, posY + fontMetrics.ascent)

            posX += glyph.width

            glyphCharacterMap[ch] = glyph
        }
    }

    fun setupTexture() {
        loadedTexture = DynamicTexture(bufferedImage)
    }

    fun bindTexture() = GlStateManager.bindTexture(loadedTexture !!.glTextureId)
    fun unbindTexture() = GlStateManager.bindTexture(0)

    fun drawChar(ch: Char, x: Float, y: Float): Float {
        val glyph = glyphCharacterMap[ch] ?: throw IllegalArgumentException("'$ch' wasn't found")

        val pageX = glyph.x / imgSize.toFloat()
        val pageY = glyph.y / imgSize.toFloat()

        val pageWidth = glyph.width / imgSize.toFloat()
        val pageHeight = glyph.height / imgSize.toFloat()

        val width = glyph.width.toFloat()
        val height = glyph.height.toFloat()

        GL11.glBegin(GL11.GL_TRIANGLES)

        GL11.glTexCoord2f(pageX + pageWidth, pageY)
        GL11.glVertex2f(x + width, y)

        GL11.glTexCoord2f(pageX, pageY)
        GL11.glVertex2f(x, y)

        GL11.glTexCoord2f(pageX, pageY + pageHeight)
        GL11.glVertex2f(x, y + height)

        GL11.glTexCoord2f(pageX, pageY + pageHeight)
        GL11.glVertex2f(x, y + height)

        GL11.glTexCoord2f(pageX + pageWidth, pageY + pageHeight)
        GL11.glVertex2f(x + width, y + height)

        GL11.glTexCoord2f(pageX + pageWidth, pageY)
        GL11.glVertex2f(x + width, y)


        GL11.glEnd()

        return width - 8
    }

    fun getWidth(ch: Char): Float {
        return glyphCharacterMap[ch]?.width?.toFloat() ?: throw IllegalArgumentException("'$ch' wasn't found")
    }

    internal class Glyph {
        var x: Int = 0
        var y: Int = 0
        var width: Int = 0
        var height: Int = 0
    }
}
*/

class GlyphPage(
    private val font: Font,
    private val antiAliasing: Boolean,
    private val fractionalMetrics: Boolean
) {
    private var imgSize = 256
    var calculatedMaxFontHeight: Int = 0
        private set

    private val glyphCharacterMap = HashMap<Int, Glyph>()
    private var bufferedImage: BufferedImage? = null
    private var loadedTexture: DynamicTexture? = null

    fun getActualMaxFontHeight(): Int = calculatedMaxFontHeight

    fun generateGlyphPage(codepointsToRender: Collection<Int>) {
        if (codepointsToRender.isEmpty()) {
            imgSize = 32
            calculatedMaxFontHeight = font.size // Estimate
            bufferedImage = BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB)
            val g = bufferedImage !!.graphics as Graphics2D
            g.color = Color(0, 0, 0, 0)
            g.fillRect(0, 0, imgSize, imgSize)
            g.dispose()
            return
        }

        var maxGlyphWidth = 0.0
        var maxGlyphHeight = 0.0

        val dummyGraphics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
        dummyGraphics.font = font
        val fontMetricsInitial = dummyGraphics.fontMetrics
        val frc = dummyGraphics.fontRenderContext
        dummyGraphics.dispose()


        for (codepoint in codepointsToRender) {
            val charStr = codepoint.toChar().toString()
            val bounds = font.getStringBounds(charStr, frc)
            if (bounds.width > maxGlyphWidth) maxGlyphWidth = bounds.width
            if (bounds.height > maxGlyphHeight) maxGlyphHeight = bounds.height
        }

        val glyphPaddingWidth = GlyphPageFontRenderer.DEFAULT_CHAR_SPACING
        val glyphPaddingHeight = 2

        maxGlyphWidth += glyphPaddingWidth
        maxGlyphHeight += glyphPaddingHeight

        if (maxGlyphWidth <= glyphPaddingWidth) maxGlyphWidth = fontMetricsInitial.charWidth('?').toDouble() + glyphPaddingWidth
        if (maxGlyphHeight <= glyphPaddingHeight) maxGlyphHeight = fontMetricsInitial.height.toDouble() + glyphPaddingHeight

        val numChars = codepointsToRender.size
        val totalArea = maxGlyphWidth * maxGlyphHeight * numChars
        val dimension = ceil(sqrt(totalArea)).toInt()

        imgSize = max(256, dimension)
        var testSize = 1
        while (testSize < dimension) testSize *= 2
        imgSize = max(testSize, 128)

        if (maxGlyphWidth > imgSize) {
            imgSize *= ceil(maxGlyphWidth / imgSize).toInt()
            if (maxGlyphWidth > imgSize) imgSize *= 2
        }


        bufferedImage = BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB)
        val g = bufferedImage !!.graphics as Graphics2D

        g.font = font
        g.color = Color(255, 255, 255, 0)
        g.fillRect(0, 0, imgSize, imgSize)
        g.color = Color.white

        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, if (fractionalMetrics) RenderingHints.VALUE_FRACTIONALMETRICS_ON else RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, if (antiAliasing) RenderingHints.VALUE_ANTIALIAS_ON else RenderingHints.VALUE_ANTIALIAS_OFF)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, if (antiAliasing) RenderingHints.VALUE_TEXT_ANTIALIAS_ON else RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)

        val fontMetrics = g.fontMetrics
        var currentLineHeight = 0
        var posX = 0
        var posY = 0

        this.calculatedMaxFontHeight = 0

        for (codepoint in codepointsToRender) {
            val charStr = codepoint.toChar().toString()
            val bounds = fontMetrics.getStringBounds(charStr, g)
            val glyph = Glyph()

            glyph.width = ceil(bounds.width).toInt() + glyphPaddingWidth
            glyph.height = ceil(bounds.height).toInt()

            if (glyph.height == 0) glyph.height = fontMetrics.height

            if (posX + glyph.width >= imgSize) {
                posX = 0
                posY += currentLineHeight
                currentLineHeight = 0
            }

            if (posY + glyph.height >= imgSize) {
                noammaddons.Logger.error("GlyphPage: Character '$charStr' (cp: $codepoint) will not fit on texture. Texture too small or glyph too large.")
                continue
            }

            glyph.x = posX
            glyph.y = posY

            val yDrawOffset = 1
            g.drawString(charStr, posX + (glyphPaddingWidth / 2), posY + fontMetrics.ascent + yDrawOffset)

            if (glyph.height > this.calculatedMaxFontHeight) this.calculatedMaxFontHeight = glyph.height
            if (glyph.height > currentLineHeight) currentLineHeight = glyph.height

            glyphCharacterMap[codepoint] = glyph
            posX += glyph.width
        }
        g.dispose()
    }

    fun setupTexture() {
        if (bufferedImage == null) return noammaddons.Logger.error("GlyphPage: setupTexture called with null bufferedImage!")
        loadedTexture = DynamicTexture(bufferedImage)
        bufferedImage = null // Keep it if you might need to re-upload or debug
    }

    fun bindTexture() = loadedTexture?.let { GlStateManager.bindTexture(it.glTextureId) }
    fun unbindTexture() = GlStateManager.bindTexture(0)

    fun drawChar(codepoint: Int, x: Float, y: Float): Float {
        val glyph = glyphCharacterMap[codepoint]
        if (glyph == null) {
            val replacementGlyph = glyphCharacterMap['?'.code]
            if (replacementGlyph != null) return drawSingleGlyph(replacementGlyph, x, y)
            return - 1f
        }
        return drawSingleGlyph(glyph, x, y)
    }

    private fun drawSingleGlyph(glyph: Glyph, x: Float, y: Float): Float {
        if (imgSize == 0 || loadedTexture == null) return 0f

        val texX = glyph.x / imgSize.toFloat()
        val texY = glyph.y / imgSize.toFloat()
        val texWidth = glyph.width / imgSize.toFloat()
        val texHeight = glyph.height / imgSize.toFloat()

        val renderWidth = glyph.width.toFloat()
        val renderHeight = glyph.height.toFloat()

        GL11.glBegin(GL11.GL_TRIANGLES)

        GL11.glTexCoord2f(texX, texY)
        GL11.glVertex2f(x, y)
        GL11.glTexCoord2f(texX, texY + texHeight)
        GL11.glVertex2f(x, y + renderHeight)
        GL11.glTexCoord2f(texX + texWidth, texY + texHeight)
        GL11.glVertex2f(x + renderWidth, y + renderHeight)

        GL11.glTexCoord2f(texX + texWidth, texY + texHeight)
        GL11.glVertex2f(x + renderWidth, y + renderHeight)
        GL11.glTexCoord2f(texX + texWidth, texY)
        GL11.glVertex2f(x + renderWidth, y)
        GL11.glTexCoord2f(texX, texY)
        GL11.glVertex2f(x, y)

        GL11.glEnd()

        return renderWidth - GlyphPageFontRenderer.DEFAULT_CHAR_SPACING
    }

    fun getWidth(codepoint: Int): Float {
        val glyph = glyphCharacterMap[codepoint]
        if (glyph == null) {
            val replacementGlyph = glyphCharacterMap['?'.code]
            if (replacementGlyph != null) return replacementGlyph.width.toFloat() - GlyphPageFontRenderer.DEFAULT_CHAR_SPACING
            return 0f
        }
        return glyph.width.toFloat() - GlyphPageFontRenderer.DEFAULT_CHAR_SPACING
    }

    internal class Glyph {
        var x: Int = 0
        var y: Int = 0
        var width: Int = 0
        var height: Int = 0
    }
}