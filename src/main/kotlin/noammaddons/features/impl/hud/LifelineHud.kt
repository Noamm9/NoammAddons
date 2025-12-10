package noammaddons.features.impl.hud

import net.minecraft.network.play.server.S06PacketUpdateHealth
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.MainThreadPacketRecivedEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderUtils.drawText

object LifelineHud: Feature("Shows on screen when u are below 20%") {
    private object LifelineHudElement: GuiElement(hudData.getData().lifelineHud) {
        override val enabled get() = LifelineHud.enabled
        private val text get() = "&4&lLifeLine!"
        override val width: Float get() = RenderHelper.getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private var lifelineActive = false

    @SubscribeEvent
    fun onPacket(event: MainThreadPacketRecivedEvent.Post) {
        val packet = event.packet as? S06PacketUpdateHealth ?: return
        lifelineActive = packet.health < (mc.thePlayer.maxHealth * 0.195)
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! LifelineHudElement.enabled) return
        if (! lifelineActive) return
        LifelineHudElement.draw()
    }
}