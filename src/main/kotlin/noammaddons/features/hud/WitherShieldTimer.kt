package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.ClickEvent.*
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.isHoldingWitherImpact
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils.potisPow
import java.awt.Color


object WitherShieldTimer: Feature() {
    private object WitherShieldElement: GuiElement(hudData.getData().WitherShieldTimer) {
        override val enabled get() = config.WitherShieldTimer
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


    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! config.WitherShieldTimer) return
        if (tickTimer > 100) return
        tickTimer += 1

        if (tickTimer != 100/* && !config.witherShieldSound*/) return
        potisPow.start()
    }

    @SubscribeEvent
    fun onSentRightClick(event: RightClickEvent) {
        if (! config.WitherShieldTimer) return
        if (tickTimer < 100) return
        if (! isHoldingWitherImpact()) return
        tickTimer = 0
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (! config.WitherShieldTimer) return
        if (tickTimer >= 100) return // Only display when the timer is running
        WitherShieldElement.text = (((5000 - tickTimer * 50) / 100) / 10.0).toString()
        WitherShieldElement.draw()
    }
}
