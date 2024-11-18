package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.isNull

object TpsDisplay: Feature() {
    private val TpsDisplayElement = TextElement("TPS: 2O", dataObj = hudData.getData().TpsDisplay)
    private var tps = 0

    fun getTps() = TpsDisplayElement.getText()

    init {
        loop(3000) {
            if (mc.theWorld.isNull()) return@loop
            if (Player.isNull()) return@loop

            TpsDisplayElement.setText("TPS: ${if (tps > 100) 20 else tps / 3}")
            tps = 0
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        tps ++
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! config.TpsDisplay) return

        TpsDisplayElement.run {
            setColor(config.TpsDisplayColor)
            draw()
        }
    }
}