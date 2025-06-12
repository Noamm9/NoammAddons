package noammaddons.ui.font

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import noammaddons.noammaddons.Companion.MOD_ID
import org.lwjgl.opengl.GL11
import java.awt.Font


class GlyphPageFontRenderer(
    private val fontName: String,
    private val fontSize: Int,
    private val supportBold: Boolean,
    private val supportItalic: Boolean,
    private val supportBoldItalic: Boolean
) {
    private var posX = 0f
    private var posY = 0f

    private val colorCode = IntArray(32)
    private var red = 0f
    private var blue = 0f
    private var green = 0f
    private var alpha = 0f

    private var boldStyle = false
    private var italicStyle = false
    private var underlineStyle = false
    private var strikethroughStyle = false

    private val regularGlyphPages = mutableMapOf<Int, GlyphPage>()
    private val boldGlyphPages = mutableMapOf<Int, GlyphPage>()
    private val italicGlyphPages = mutableMapOf<Int, GlyphPage>()
    private val boldItalicGlyphPages = mutableMapOf<Int, GlyphPage>()

    private var activeExternalScale = 1.0f
    private var preferNearestForActiveUpscale = false

    private var activeGlyphPage: GlyphPage? = null

    companion object {
        const val CHARS_PER_PAGE = 256
        const val DEFAULT_CHAR_SPACING = 8

        fun create(fontName: String, size: Int, bold: Boolean = false, italic: Boolean = false, boldItalic: Boolean = false): GlyphPageFontRenderer {
            val renderer = GlyphPageFontRenderer(fontName, size, bold, italic, boldItalic)
            renderer.getOrGenerateGlyphPage(0, Font.PLAIN)
            if (bold) renderer.getOrGenerateGlyphPage(0, Font.BOLD)
            if (italic) renderer.getOrGenerateGlyphPage(0, Font.ITALIC)
            if (boldItalic) renderer.getOrGenerateGlyphPage(0, Font.BOLD or Font.ITALIC)
            return renderer
        }
    }

    init {
        for (i in 0 .. 31) {
            val j = (i shr 3 and 1) * 85
            var k = (i shr 2 and 1) * 170 + j
            var l = (i shr 1 and 1) * 170 + j
            var i1 = (i and 1) * 170 + j

            if (i == 6) k += 85

            if (i >= 16) {
                k /= 4
                l /= 4
                i1 /= 4
            }

            colorCode[i] = (k and 255) shl 16 or ((l and 255) shl 8) or (i1 and 255)
        }
    }

    private fun getGlyphPageCacheForStyle(style: Int): MutableMap<Int, GlyphPage> = when (style) {
        Font.PLAIN -> regularGlyphPages
        Font.BOLD -> if (supportBold) boldGlyphPages else regularGlyphPages
        Font.ITALIC -> if (supportItalic) italicGlyphPages else regularGlyphPages
        Font.BOLD or Font.ITALIC -> if (supportBoldItalic) boldItalicGlyphPages
        else if (supportBold) boldGlyphPages else if (supportItalic) italicGlyphPages else regularGlyphPages

        else -> regularGlyphPages
    }

    private fun getOrGenerateGlyphPage(codepoint: Int, style: Int): GlyphPage {
        val pageId = codepoint / CHARS_PER_PAGE
        val cache = getGlyphPageCacheForStyle(style)

        return cache.computeIfAbsent(pageId) {
            val awtFont = getFont(fontName, fontSize, style)
            val newPage = GlyphPage(awtFont, antiAliasing = true, fractionalMetrics = true)

            val codepointsForPage = mutableListOf<Int>()
            val startCodepoint = pageId * CHARS_PER_PAGE
            val endCodepoint = startCodepoint + CHARS_PER_PAGE - 1

            for (cp in startCodepoint .. endCodepoint) {
                if (awtFont.canDisplay(cp)) {
                    codepointsForPage.add(cp)
                }
            }
            if (codepointsForPage.isEmpty() && awtFont.canDisplay(codepoint)) {
                codepointsForPage.add(codepoint)
            }

            newPage.generateGlyphPage(codepointsForPage)
            newPage.setupTexture()
            newPage
        }
    }

    fun drawScaledString(
        text: String?,
        x: Float, y: Float,
        color: Int,
        dropShadow: Boolean,
        scale: Float,
        useNearestTexForUpscale: Boolean = false
    ): Int {
        if (scale == 0f) return 0
        if (text.isNullOrEmpty()) return 0

        val prevScale = this.activeExternalScale
        val prevNearest = this.preferNearestForActiveUpscale

        this.activeExternalScale = scale
        this.preferNearestForActiveUpscale = useNearestTexForUpscale && scale > 1.0f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        GlStateManager.scale(scale, scale, 1.0f)
        GlStateManager.translate(- x, - y, 0f)

        val unscaledWidth = drawString(text, x, y, color, dropShadow)

        GlStateManager.popMatrix()

        this.activeExternalScale = prevScale
        this.preferNearestForActiveUpscale = prevNearest

        return (unscaledWidth * scale).toInt()
    }

    fun drawString(text: String?, x: Float, y: Float, color: Int, dropShadow: Boolean): Int {
        if (text == null) return 0

        GlStateManager.enableAlpha()
        resetStyles()

        val roundedX = (kotlin.math.round(x * 2) / 2.0).toFloat()
        val roundedY = (kotlin.math.round(y * 2) / 2.0).toFloat()

        val finalWidth: Int
        if (dropShadow) {
            val shadowColor = (color and 0xFCFCFC) shr 2 or (color and 0xFF000000.toInt())
            renderString(text, roundedX + 1.0f, roundedY + 1.0f, shadowColor, true)
            finalWidth = renderString(text, roundedX, roundedY, color, false)
        }
        else {
            finalWidth = renderString(text, roundedX, roundedY, color, false)
        }
        activeGlyphPage?.unbindTexture()
        activeGlyphPage = null
        return finalWidth
    }

    private fun renderString(text: String, x: Float, y: Float, color: Int, shadow: Boolean): Int {
        var effectiveColor = color
        if ((effectiveColor and - 67108864) == 0) {
            effectiveColor = effectiveColor or - 16777216
        }

        val initialRed = (effectiveColor shr 16 and 255).toFloat() / 255.0f
        val initialGreen = (effectiveColor shr 8 and 255).toFloat() / 255.0f
        val initialBlue = (effectiveColor and 255).toFloat() / 255.0f
        val initialAlpha = (effectiveColor shr 24 and 255).toFloat() / 255.0f

        this.red = initialRed
        this.green = initialGreen
        this.blue = initialBlue
        this.alpha = initialAlpha

        GlStateManager.color(this.red, this.green, this.blue, this.alpha)

        val originalPosX = x * 2.0f
        this.posX = originalPosX
        this.posY = y * 2.0f

        renderStringAtPos(text, shadow, initialRed, initialGreen, initialBlue)

        val totalAdvanceIn2xSpace = this.posX - originalPosX
        return (totalAdvanceIn2xSpace / 2.0f).toInt()
    }

    private fun getCurrentFontStyle(): Int {
        return if (boldStyle && italicStyle && supportBoldItalic) Font.BOLD or Font.ITALIC
        else if (boldStyle && supportBold) Font.BOLD
        else if (italicStyle && supportItalic) Font.ITALIC
        else Font.PLAIN
    }

    private fun renderStringAtPos(text: String, shadow: Boolean, initialR: Float, initialG: Float, initialB: Float) {
        GL11.glPushMatrix()
        GL11.glScaled(0.5, 0.5, 0.5)

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableTexture2D()

        var i = 0
        while (i < text.length) {
            val charCode = text[i].code
            var codepoint: Int

            if (charCode == 167 && i + 1 < text.length) {
                when (val formatCode = "0123456789abcdefklmnor".indexOf(text.lowercase()[i + 1])) {
                    in 0 .. 15 -> {
                        resetFormattingStyles()
                        val newColor = colorCode[formatCode + if (shadow) 16 else 0]
                        this.red = (newColor shr 16 and 255).toFloat() / 255.0f
                        this.green = (newColor shr 8 and 255).toFloat() / 255.0f
                        this.blue = (newColor and 255).toFloat() / 255.0f
                    }

                    17 -> boldStyle = true
                    18 -> strikethroughStyle = true
                    19 -> underlineStyle = true
                    20 -> italicStyle = true
                    21 -> {
                        resetFormattingStyles()
                        this.red = initialR
                        this.green = initialG
                        this.blue = initialB
                    }
                }
                GlStateManager.color(this.red, this.green, this.blue, this.alpha)
                i += 2
                continue
            }
            else codepoint = text.codePointAt(i)


            val currentStyle = getCurrentFontStyle()
            val glyphPage = getOrGenerateGlyphPage(codepoint, currentStyle)

            if (glyphPage !== activeGlyphPage) {
                activeGlyphPage?.unbindTexture()
                glyphPage.bindTexture()

                if (this.preferNearestForActiveUpscale) GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
                else GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
                activeGlyphPage = glyphPage
            }

            val charWidthDevice = glyphPage.drawChar(codepoint, posX, posY)

            if (charWidthDevice >= 0) {
                doDrawDecorations(charWidthDevice, glyphPage)
                posX += charWidthDevice
            }

            i += Character.charCount(codepoint)
        }

        GL11.glPopMatrix()
    }

    private fun resetFormattingStyles() {
        boldStyle = false
        italicStyle = false
        underlineStyle = false
        strikethroughStyle = false
    }

    private fun doDrawDecorations(charTrueWidth: Float, glyphPage: GlyphPage) {
        val decorationThickness = 2.0f

        if (strikethroughStyle) {
            val tessellator = Tessellator.getInstance()
            val worldrenderer = tessellator.worldRenderer
            GlStateManager.disableTexture2D()
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            val y1 = posY + (glyphPage.getActualMaxFontHeight() / 2f) - (decorationThickness / 2f)
            val y2 = y1 + decorationThickness
            worldrenderer.pos(posX.toDouble(), y1.toDouble(), 0.0).endVertex()
            worldrenderer.pos((posX + charTrueWidth).toDouble(), y1.toDouble(), 0.0).endVertex()
            worldrenderer.pos((posX + charTrueWidth).toDouble(), y2.toDouble(), 0.0).endVertex()
            worldrenderer.pos(posX.toDouble(), y2.toDouble(), 0.0).endVertex()
            tessellator.draw()
            GlStateManager.enableTexture2D()
        }

        if (underlineStyle) {
            val tessellator = Tessellator.getInstance()
            val worldrenderer = tessellator.worldRenderer
            GlStateManager.disableTexture2D()
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            val y1 = posY + glyphPage.getActualMaxFontHeight() - decorationThickness
            val y2 = y1 + decorationThickness
            worldrenderer.pos(posX.toDouble(), y1.toDouble(), 0.0).endVertex()
            worldrenderer.pos((posX + charTrueWidth).toDouble(), y1.toDouble(), 0.0).endVertex()
            worldrenderer.pos((posX + charTrueWidth).toDouble(), y2.toDouble(), 0.0).endVertex()
            worldrenderer.pos(posX.toDouble(), y2.toDouble(), 0.0).endVertex()
            tessellator.draw()
            GlStateManager.enableTexture2D()
        }
    }

    private fun resetStyles() {
        resetFormattingStyles()
        this.red = 1.0f
        this.green = 1.0f
        this.blue = 1.0f
        this.alpha = 1.0f
        GlStateManager.color(red, green, blue, alpha)
    }

    val fontHeight: Int
        get() {
            val page = regularGlyphPages[0] ?: getOrGenerateGlyphPage('A'.code, Font.PLAIN)
            return page.getActualMaxFontHeight() / 2
        }


    fun getStringWidth(text: String?): Int {
        if (text.isNullOrEmpty()) return 0

        var totalWidth = 0f
        var tempBold = false
        var tempItalic = false

        var i = 0
        while (i < text.length) {
            val charCode = text[i].code
            var codepoint: Int

            if (charCode == 167 && i + 1 < text.length) { // '§'
                val formatChar = text.lowercase()[i + 1]
                val formatCode = "0123456789abcdefklmnor".indexOf(formatChar)
                when (formatCode) {
                    in 0 .. 15 -> {
                        tempBold = false
                        tempItalic = false
                    }

                    17 -> tempBold = true // l for bold
                    20 -> tempItalic = true // o for italic
                    21 -> { // r for reset
                        tempBold = false
                        tempItalic = false
                    }
                }
                i += 2
                continue
            }
            else codepoint = text.codePointAt(i)

            val style = when {
                tempBold && tempItalic && supportBoldItalic -> Font.BOLD or Font.ITALIC
                tempBold && supportBold -> Font.BOLD
                tempItalic && supportItalic -> Font.ITALIC
                else -> Font.PLAIN
            }

            val glyphPage = getOrGenerateGlyphPage(codepoint, style)
            totalWidth += glyphPage.getWidth(codepoint)
            i += Character.charCount(codepoint)
        }
        return (totalWidth / 2f).toInt()
    }

    private fun getFont(fontName: String, size: Int, style: Int): Font {
        try {
            val inputStream = this::class.java.getResourceAsStream("/assets/$MOD_ID/font/$fontName.ttf")
            var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            awtClientFont = awtClientFont.deriveFont(style, size.toFloat())
            inputStream !!.close()
            return awtClientFont
        }
        catch (e: Exception) {
            e.printStackTrace()
            return Font("default", Font.PLAIN, size)
        }
    }
}