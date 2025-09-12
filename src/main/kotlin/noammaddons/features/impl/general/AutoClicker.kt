package noammaddons.features.impl.general

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.ServerPlayer


object AutoClicker: Feature(name = "Auto Clicker") {
    private val cps = SliderSetting("Clicks Per Second", 3f, 15f, .5f, 5f)
    private val terminatorCheck = ToggleSetting("Terminator Only")
    private val leftClickToggle = ToggleSetting("Left Click").addDependency { terminatorCheck.value }
    private val leftClickKeybind = KeybindSetting("Left Click Keybind")
    private val rightClickToggle = ToggleSetting("Right Click").addDependency { terminatorCheck.value }
    private val rightClickKeybind = KeybindSetting("Right Click Keybind")

    private var nextLeftClick = 0L
    private var nextRightClick = 0L

    override fun init() {
        leftClickKeybind.addDependency(leftClickToggle).addDependency { terminatorCheck.value }
        rightClickKeybind.addDependency(rightClickToggle).addDependency { terminatorCheck.value }

        addSettings(
            cps, terminatorCheck,
            leftClickToggle, leftClickKeybind,
            rightClickToggle, rightClickKeybind,
        )
    }

    @SubscribeEvent
    fun onClientTickEvent(event: ClientTickEvent) {
        if (event.phase != Phase.START) return
        if (mc.currentScreen != null || mc.thePlayer == null) return
        if (mc.thePlayer.isUsingItem) return
        val now = System.currentTimeMillis()

        if (terminatorCheck.value) {
            if (! mc.gameSettings.keyBindUseItem.isKeyDown) return
            if (ServerPlayer.player.getHeldItem().skyblockID != "TERMINATOR") return
            if (now < nextLeftClick) return

            nextLeftClick = getNextClick(now)
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
        }
        else {
            if (leftClickToggle.value) {
                if (! leftClickKeybind.isDown()) return
                if (now < nextLeftClick) return

                nextLeftClick = getNextClick(now)
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            }

            if (rightClickToggle.value) {
                if (! leftClickKeybind.isDown()) return
                if (now < nextRightClick) return

                nextRightClick = getNextClick(now)
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            }
        }
    }

    private fun getNextClick(now: Long): Long {
        val delay = (1000.0 / cps.value).toLong()
        val randomOffset = (Math.random() * 60.0 - 30.0).toLong()
        return now + delay + randomOffset
    }
}
