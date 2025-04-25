package noammaddons.ui.config

import noammaddons.noammaddons
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth

object Utils {
    fun getPX(value: Number) = (noammaddons.mc.getWidth()) * (value.toFloat() / 100.0)
    fun getPY(value: Number) = (noammaddons.mc.getHeight()) * (value.toFloat() / 100.0)
}