package noammaddons.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3
import noammaddons.mixins.AccessorMinecraft
import noammaddons.noammaddons.Companion.mc
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color


object RenderHelper {
    fun getRainbowColor(hueOffset: Float): Color = Color.getHSBColor(((System.currentTimeMillis() % 4500L) / 4500.0f + hueOffset) % 1.0f, 1.0f, 1.0f)
    fun Color.applyAlpha(alpha: Number): Color = Color(this.red, this.green, this.blue, alpha.toInt())

    fun getPartialTicks() = (mc as AccessorMinecraft).timer.renderPartialTicks

    fun Entity.getRenderX(): Double = interpolate(lastTickPosX, posX, getPartialTicks())
    fun Entity.getRenderY(): Double = interpolate(lastTickPosY, posY, getPartialTicks())
    fun Entity.getRenderZ(): Double = interpolate(lastTickPosZ, posZ, getPartialTicks())
    fun Entity.getRenderVec(): Vec3 = Vec3(getRenderX(), getRenderY(), getRenderZ())

    fun Minecraft.getWidth(): Int = ScaledResolution(this).scaledWidth
    fun Minecraft.getHeight(): Int = ScaledResolution(this).scaledHeight
    fun Minecraft.getScaleFactor(): Int = ScaledResolution(this).scaleFactor


    fun bindColor(color: Color, alpha: Number = color.alpha) = GlStateManager.color(
        color.red / 255f,
        color.green / 255f,
        color.blue / 255f,
        alpha.toFloat() / 255f
    )

    fun glBindColor(color: Color, alpha: Number = color.alpha) = glColor4f(
        color.red / 255f,
        color.green / 255f,
        color.blue / 255f,
        alpha.toFloat() / 255f
    )

    fun interpolate(prev: Number, newPos: Number, partialTicks: Number): Double {
        return prev.toDouble() + (newPos.toDouble() - prev.toDouble()) * partialTicks.toDouble()
    }

    fun interpolateColor(color1: Color, color2: Color, value: Float): Color {
        return Color(
            interpolate(color1.red, color2.red, value).toInt(),
            interpolate(color1.green, color2.green, value).toInt(),
            interpolate(color1.blue, color2.blue, value).toInt()
        )
    }

    fun enableChums(color: Color) {
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        bindColor(color)
        GlStateManager.enablePolygonOffset()
        GlStateManager.doPolygonOffset(1f, - 1000000f)
    }

    fun disableChums() {
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
        GlStateManager.doPolygonOffset(1f, 1000000f)
        GlStateManager.disablePolygonOffset()
    }
}
