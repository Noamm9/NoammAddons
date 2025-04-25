package noammaddons.features.impl.general.teleport

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.PlayerUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.rightClick
import noammaddons.utils.PlayerUtils.swinghand
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.ServerPlayer.player


object LeftClickEtherwarp: Feature("Allows you to Etherwarp with left click") {
    private val swingHandToggle = ToggleSetting("Swing Hand", true)
    private val autoSneakToggle = ToggleSetting("Auto Sneak", false)
    private val autoSneakDelaySlider = SliderSetting("Delay", 50f, 150f, 50.0).addDependency(autoSneakToggle) { ! it.value }

    override fun init() = addSettings(
        swingHandToggle, autoSneakToggle, autoSneakDelaySlider
    )


    @SubscribeEvent
    fun onLeftClick(event: MouseEvent) {
        if (! event.buttonstate) return
        if (event.button != 0) return
        if (! player.sneaking && ! autoSneakToggle.value) return
        val item = player.getHeldItem() ?: return
        if (! isHoldingEtherwarpItem(item)) return
        event.isCanceled = true

        scope.launch {
            if (autoSneakToggle.value && ! player.sneaking) {
                val halfTime = autoSneakDelaySlider.value.toLong() / 2
                toggleSneak(true)
                delay(halfTime)
                if (swingHandToggle.value) swinghand()
                rightClick()
                delay(halfTime)
                toggleSneak(false)
            }
            else {
                if (swingHandToggle.value) swinghand()
                rightClick()
            }
        }
    }
}