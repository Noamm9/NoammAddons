package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.ClickEvent.*
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.isHoldingWitherImpact
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color


object WitherShieldTimer: Feature() {
    private object WitherShieldElement: GuiElement(hudData.getData().WitherShieldTimer) {
        override val enabled get() = WitherShieldTimer.enabled
        var text = "&e&l5"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale(), color)
        override fun exampleDraw() = drawText("&e&l5", getX(), getY(), getScale(), color)

        private val color
            get() = when {
                tickTimer < 33 -> Color.RED
                tickTimer < 66 -> Color.YELLOW
                else -> Color.GREEN
            }
    }

    private var tickTimer = 101

    init {
        onServerTick({ tickTimer <= 100 }) { tickTimer += 1 }
    }

    @SubscribeEvent
    fun onSentRightClick(event: RightClickEvent) {
        if (tickTimer < 100) return
        if (! isHoldingWitherImpact()) return
        tickTimer = 0
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (tickTimer >= 100) return // Only display when the timer is running
        WitherShieldElement.text = (((5000 - tickTimer * 50) / 100) / 10.0).toString()
        WitherShieldElement.draw()
    }
}
