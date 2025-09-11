package noammaddons.features.impl.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.KeybindSetting
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.PlayerUtils.leftClick
import noammaddons.utils.PlayerUtils.rightClick
import noammaddons.utils.ServerPlayer
import org.lwjgl.input.Keyboard

object TermAutoClicker: Feature(name = "AutoClicker") {
    private val leftClickToggle = ToggleSetting("Left Click")
    private val leftClickKeybind = KeybindSetting("Left Click Keybind").addDependency { leftClickToggle.value }
    private val rightClickToggle = ToggleSetting("Right Click")
    private val rightClickKeybind = KeybindSetting("Right Click Keybind").addDependency { rightClickToggle.value }
    private val terminatorCheck = ToggleSetting("Terminator Only")
    private val cps by SliderSetting("Clicks Per Second", 3f, 15f, .5f, 5f)
    private var nextLeftClick = .0
    private var nextRightClick = .0

    @SubscribeEvent
    fun onClientTickEvent(event: ClientTickEvent) {
        if (event.phase != Phase.START) return
        if (mc.currentScreen != null) return
        val now = System.currentTimeMillis()

        if (terminatorCheck.value) {
            if (!mc.gameSettings.keyBindUseItem.isKeyDown) return
            if (ServerPlayer.player.getHeldItem().skyblockID != "TERMINATOR") return

            if (now < nextLeftClick) return
            nextLeftClick = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
            leftClick()
        } else {
            if (leftClickToggle.value) {
                if (!Keyboard.isKeyDown(leftClickKeybind.value)) return
                if (now < nextLeftClick) return

                nextLeftClick = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
                leftClick()
            }

            if (rightClickToggle.value) {
                if (!Keyboard.isKeyDown(rightClickKeybind.value)) return
                if (now < nextRightClick) return

                nextRightClick = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
                rightClick()
            }
        }
    }
}
