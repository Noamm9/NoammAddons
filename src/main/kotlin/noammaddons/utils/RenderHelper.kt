package noammaddons.utils

import net.minecraft.client.LoadingScreenRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.inventory.Slot
import net.minecraft.util.Vec3
import noammaddons.NoammAddons.Companion.mc
import noammaddons.mixins.accessor.AccessorMinecraft
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.RenderUtils.drawRect
import org.lwjgl.opengl.GL11.*
import java.awt.Color


object RenderHelper {
    private val colorMap = (('0' .. '9') + ('a' .. 'f')).associateWith {
        Color(mc.fontRendererObj.getColorCode(it))
    }

    fun getRainbowColor(hueOffset: Float): Color = Color.getHSBColor(((System.currentTimeMillis() % 4500L) / 4500.0f + hueOffset) % 1.0f, 1.0f, 1.0f)

    @JvmStatic
    val partialTicks get() = (mc as AccessorMinecraft).timer.renderPartialTicks

    @JvmStatic
    val Entity.renderX: Double get() = lerp(lastTickPosX, posX, partialTicks)

    @JvmStatic
    val Entity.renderY: Double get() = lerp(lastTickPosY, posY, partialTicks)

    @JvmStatic
    val Entity.renderZ: Double get() = lerp(lastTickPosZ, posZ, partialTicks)

    @JvmStatic
    val Entity.renderVec: Vec3 get() = Vec3(renderX, renderY, renderZ)

    @JvmStatic
    fun Minecraft.getWidth(): Int = ScaledResolution(this).scaledWidth

    @JvmStatic
    fun Minecraft.getHeight(): Int = ScaledResolution(this).scaledHeight

    @JvmStatic
    fun Minecraft.getScaleFactor(): Int = ScaledResolution(this).scaleFactor


    @JvmStatic
    fun bindColor(color: Color, alpha: Number = color.alpha) = GlStateManager.color(
        color.red / 255f,
        color.green / 255f,
        color.blue / 255f,
        alpha.toFloat() / 255f
    )

    @JvmStatic
    fun glBindColor(color: Color, alpha: Number = color.alpha) = glColor4f(
        color.red / 255f,
        color.green / 255f,
        color.blue / 255f,
        alpha.toFloat() / 255f
    )

    @JvmStatic
    fun Color.destructured(withAlpha: Boolean = false): List<Int> {
        return if (! withAlpha) listOf(red, green, blue)
        else listOf(red, green, blue, alpha)
    }

    fun colorFromHSB(hue: Float, saturation: Float, brightness: Float): Color {
        val rgb = Color.HSBtoRGB(hue, saturation, brightness)
        return Color(
            (rgb shr 16) and 0xFF,
            (rgb shr 8) and 0xFF,
            rgb and 0xFF
        )
    }

    @JvmStatic
    fun enableChums() {
        glEnable(GL_POLYGON_OFFSET_FILL)
        glPolygonOffset(1f, - 1000000f)
    }

    @JvmStatic
    fun disableChums() {
        glPolygonOffset(1f, 1000000f)
        glDisable(GL_POLYGON_OFFSET_FILL)
    }

    @JvmStatic
    fun getStringWidth(text: String, scale: Number = 1): Float {
        return if (text.contains("\n")) text.split("\n").maxOf { mc.fontRendererObj.getStringWidth(it.addColor()) } * scale.toFloat()
        else mc.fontRendererObj.getStringWidth(text.addColor()) * scale.toFloat()
    }

    @JvmStatic
    fun getStringWidth(text: List<String>, scale: Number = 1): Float {
        return text.maxOf { mc.fontRendererObj.getStringWidth(it.addColor()) } * scale.toFloat()
    }

    fun getStringHeight(lines: List<String>, scale: Number = 1) = lines.size * 9 * scale.toFloat()

    fun getStringHeight(text: String, scale: Number = 1) = text.split("\n").size * 9 * scale.toFloat()

    @JvmStatic
    fun Slot.highlight(color: Color) = drawRect(color, xDisplayPosition, yDisplayPosition, 16, 16)

    fun colorByPresent(value: Number, maxValue: Number, reversed: Boolean = false): Color {
        val max = maxValue.toFloat().coerceAtLeast(1f)
        val current = value.toFloat().coerceIn(0f, max)
        val percentage = (current / max) * 100f

        return when {
            percentage > 75 -> if (reversed) Color.RED else Color.GREEN
            percentage > 50 -> if (reversed) Color.ORANGE else Color.YELLOW
            percentage > 25 -> if (reversed) Color.YELLOW else Color.ORANGE
            else -> if (reversed) Color.GREEN else Color.RED
        }
    }

    fun colorCodeByPresent(value: Number, maxValue: Number, reversed: Boolean = false): String {
        val max = maxValue.toFloat().coerceAtLeast(1f)
        val current = value.toFloat().coerceIn(0f, max)

        val percentage = (current / max) * 100f

        return when {
            percentage > 75 -> if (reversed) "§c" else "§a"
            percentage > 50 -> if (reversed) "§6" else "§e"
            percentage > 25 -> if (reversed) "§e" else "§6"
            else -> if (reversed) "§a" else "§c"
        }
    }

    fun optifineFastRender(bl: Boolean) {
        if (getOFfastRender() == bl) return debugMessage("already off")

        try {
            val gameSettings = mc.gameSettings
            val fastRender = gameSettings::class.java.getField("ofFastRender")
            fastRender.set(gameSettings, bl)

            mc.framebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight)
            mc.entityRenderer?.updateShaderGroupSize(mc.displayWidth, mc.displayHeight)
            mc.loadingScreen = LoadingScreenRenderer(mc)
        }
        catch (e: NoSuchFieldException) {
            debugMessage(e.message)
        }
    }

    fun getOFfastRender() = try {
        val gameSettings = mc.gameSettings
        val fastRender = gameSettings::class.java.getField("ofFastRender")
        fastRender.getBoolean(gameSettings)
    }
    catch (_: NoSuchFieldException) {
        false
    }

    fun getColorCode(color: Color): String {
        var minDistanceSquared = Int.MAX_VALUE
        var closestCode = 'f'

        for ((code, mcColor) in colorMap) {
            val rDiff = color.red - mcColor.red
            val gDiff = color.green - mcColor.green
            val bDiff = color.blue - mcColor.blue
            val distanceSquared = rDiff * rDiff + gDiff * gDiff + bDiff * bDiff

            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared
                closestCode = code
            }
        }

        return "&$closestCode"
    }
}
