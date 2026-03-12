package com.github.noamm9.features.impl.general.teleport

import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.items.EtherwarpHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lwjgl.glfw.GLFW

object LCEtherwarp: Feature(name = "LC Etherwarp", description = "Allows you to use the etherwarp ability with left-click") {
    private val swingHandToggle by ToggleSetting("Swing Hand", true)

    //#if CHEAT
    private val autoSneak by ToggleSetting("Auto Sneak", false)
    private val autoSneakDelay by SliderSetting("Auto Sneak Delay", 50, 50, 150, 1).showIf { autoSneak.value }
    //#endif

    override fun init() {
        register<MouseClickEvent> {
            if (event.button != 0) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register
            val player = mc.player ?: return@register
            //#if CHEAT
            if (! player.isCrouching && ! autoSneak.value) return@register
            //#else
            //$if (! player.isCrouching) return@register
            //#endif
            if (EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) == null) return@register

            event.isCanceled = true

            //#if CHEAT
            if (! player.isCrouching && autoSneak.value) {
                scope.launch {
                    val wait = autoSneakDelay.value.toLong() / 2
                    PlayerUtils.toggleSneak(true)
                    delay(wait)

                    PlayerUtils.rightClick()
                    if (swingHandToggle.value) PlayerUtils.swingArm()

                    delay(wait)
                    PlayerUtils.toggleSneak(false)
                }
            }
            else {
                PlayerUtils.rightClick()
                if (swingHandToggle.value) PlayerUtils.swingArm()
            }
            //#else
            //$PlayerUtils.rightClick()
            //$if (swingHandToggle.value) PlayerUtils.swingArm()
            //#endif
        }
    }
}