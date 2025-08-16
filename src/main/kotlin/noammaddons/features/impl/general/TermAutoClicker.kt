package noammaddons.features.impl.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.PlayerUtils.leftClick
import noammaddons.utils.ServerPlayer

object TermAutoClicker: Feature(name = "Term AutoClicker") {
    private val cps by SliderSetting("Clicks Per Second", 3f, 15f, .5f, 5f)
    private var nextClick = .0

    @SubscribeEvent
    fun onRenderWorldLast(event: ClientTickEvent) {
        if (event.phase != Phase.START) return
        if (mc.currentScreen != null) return
        if (! mc.gameSettings.keyBindUseItem.isKeyDown) return
        if (ServerPlayer.player.getHeldItem().skyblockID != "TERMINATOR") return
        val now = System.currentTimeMillis()

        if (now < nextClick) return
        nextClick = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
        leftClick()
    }
}
