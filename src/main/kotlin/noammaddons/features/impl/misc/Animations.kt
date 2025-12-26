package noammaddons.features.impl.misc

import net.minecraft.client.renderer.GlStateManager
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import kotlin.math.*

object Animations: Feature(desc = "Changes the appearance of the first-person held-item view model") {
    private val s by SeperatorSetting("General")
    private val x by SliderSetting("X", - 2.5f, 1.5f, 0.05f, 0f)
    private val y by SliderSetting("Y", - 1.5f, 1.5f, 0.05f, 0f)
    private val z by SliderSetting("Z", - 1.5f, 3.0f, 0.05f, 0f)
    private val yaw by SliderSetting("Yaw", - 180f, 180.0f, 1f, 0f)
    private val pitch by SliderSetting("Pitch", - 180f, 180f, 1f, 0f)
    private val roll by SliderSetting("Roll", - 180f, 180f, 1f, 0f)
    private val ss by SeperatorSetting("Extra")
    private val size by SliderSetting("Size", - 1.5f, 1.5f, 0.05f, 0f)
    val speed by SliderSetting("Speed", - 2f, 1f, 0.05f, 0f)
    val ignoreHaste by ToggleSetting("Ignore Haste", false)
    private val noEquipReset by ToggleSetting("No Equip Reset", false)
    private val noSwing by ToggleSetting("No Swing", false)
    private val scaleSwing by ToggleSetting("Scale Swing", true).hideIf { noSwing }

    private val reset by ButtonSetting("Reset") {
        configSettings.forEach {
            it.reset()
        }
    }

    @JvmStatic
    val shouldNoEquipReset get() = enabled && noEquipReset

    @JvmStatic
    val shouldStopSwing get() = enabled && noSwing

    @JvmStatic
    fun itemTransferHook(equipProgress: Float, swingProgress: Float): Boolean {
        if (! enabled) return false
        val newSize = 0.4f * exp(size)
        GlStateManager.translate(0.56f * (1 + x), - 0.52f * (1 - y), - 0.71999997f * (1 + z))
        GlStateManager.translate(0f, equipProgress * - .6f, 0f)

        GlStateManager.rotate(pitch, 1f, 0f, 0f)
        GlStateManager.rotate(yaw + 45f, 0f, 1f, 0f)
        GlStateManager.rotate(roll, 0f, 0f, 1f)

        val f1 = sin(sqrt(swingProgress) * Math.PI.toFloat())
        GlStateManager.rotate(sin(swingProgress * swingProgress * Math.PI.toFloat()) * - 20f, 0f, 1f, 0f)
        GlStateManager.rotate(f1 * - 20f, 0f, 0f, 1f)
        GlStateManager.rotate(f1 * - 80f, 1f, 0f, 0f)
        GlStateManager.scale(newSize, newSize, newSize)
        return true
    }

    @JvmStatic
    fun scaledSwing(swingProgress: Float): Boolean {
        if (! scaleSwing || ! enabled) return false
        val scale = exp(size)
        val f = - 0.4f * sin(sqrt(swingProgress) * Math.PI.toFloat()) * scale
        val f1 = 0.2f * sin(sqrt(swingProgress) * Math.PI.toFloat() * 2.0f) * scale
        val f2 = - 0.2f * sin(swingProgress * Math.PI.toFloat()) * scale
        GlStateManager.translate(f, f1, f2)
        return true
    }
}
