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
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color


object RenderHelper {
    fun getRainbowColor(hueOffset: Float): Color = Color.getHSBColor(((System.currentTimeMillis() % 4500L) / 4500.0f + hueOffset) % 1.0f, 1.0f, 1.0f)
    fun Color.applyAlpha(alpha: Number): Color = Color(red, green, blue, alpha.toInt())

    @JvmStatic
    fun getPartialTicks() = (mc as AccessorMinecraft).timer.renderPartialTicks

    fun Entity.getRenderX(): Double = interpolate(lastTickPosX, posX, getPartialTicks())
    fun Entity.getRenderY(): Double = interpolate(lastTickPosY, posY, getPartialTicks())
    fun Entity.getRenderZ(): Double = interpolate(lastTickPosZ, posZ, getPartialTicks())
    fun Entity.getRenderVec(): Vec3 = Vec3(getRenderX(), getRenderY(), getRenderZ())

    fun Minecraft.getWidth(): Int = ScaledResolution(this).scaledWidth
    fun Minecraft.getHeight(): Int = ScaledResolution(this).scaledHeight
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

    @JvmStatic
    fun Slot.highlight(color: Color) = drawRect(color, xDisplayPosition, yDisplayPosition, 16, 16)

}
