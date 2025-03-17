package noammaddons.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.inventory.Slot
import net.minecraft.util.Vec3
import noammaddons.mixins.AccessorMinecraft
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.MathUtils.interpolate
import noammaddons.utils.RenderUtils.drawRect
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import java.awt.Color


object RenderHelper {
    fun getRainbowColor(hueOffset: Float): Color = Color.getHSBColor(((System.currentTimeMillis() % 4500L) / 4500.0f + hueOffset) % 1.0f, 1.0f, 1.0f)

    @JvmStatic
    fun getPartialTicks() = (mc as AccessorMinecraft).timer.renderPartialTicks

    @JvmStatic
    val Entity.renderX: Double get() = interpolate(lastTickPosX, posX, getPartialTicks())

    @JvmStatic
    val Entity.renderY: Double get() = interpolate(lastTickPosY, posY, getPartialTicks())

    @JvmStatic
    val Entity.renderZ: Double get() = interpolate(lastTickPosZ, posZ, getPartialTicks())

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
    fun enableChums(color: Color) {
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        bindColor(color)
        GlStateManager.enablePolygonOffset()
        GlStateManager.doPolygonOffset(1f, - 1000000f)
    }

    @JvmStatic
    fun disableChums() {
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
        GlStateManager.doPolygonOffset(1f, 1000000f)
        GlStateManager.disablePolygonOffset()
    }

    @JvmStatic
    fun getStringWidth(text: String, scale: Number = 1) = mc.fontRendererObj.getStringWidth(text.addColor()) * scale.toFloat()
    fun getStringHeight(lines: List<String>, scale: Number = 1) = lines.size * 9 * scale.toFloat()

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

}
