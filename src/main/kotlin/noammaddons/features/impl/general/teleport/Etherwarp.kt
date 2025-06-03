package noammaddons.features.impl.general.teleport

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.PlayerUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.rightClick
import noammaddons.utils.PlayerUtils.swinghand
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.ServerPlayer.player


object Etherwarp: Feature("Various features for the Etherwarp Ability") {
    private val leftClickEtherwarp = ToggleSetting("Left Click Etherwarp ", true)
    private val swingHandToggle = ToggleSetting("Swing Hand", true).addDependency(leftClickEtherwarp)
    private val autoSneakToggle = ToggleSetting("Auto Sneak", false).addDependency(leftClickEtherwarp)
    private val autoSneakDelaySlider = SliderSetting("Delay", 50, 150, 1, 50).addDependency(autoSneakToggle).addDependency(leftClickEtherwarp)

    private val etherwarpSound = ToggleSetting("Etherwarp Sound ")
    private val soundName = TextInputSetting("Sound Name", "random.orb").addDependency(etherwarpSound)
    private val volume = SliderSetting("Volume", 0, 1, 0.1, 0.5).addDependency(etherwarpSound)
    private val pitch = SliderSetting("Pitch", 0, 2, 0.1, 1.0).addDependency(etherwarpSound)
    private val playSound = ButtonSetting("Play Sound") {
        repeat(5) {
            mc.thePlayer?.playSound(
                soundName.value,
                volume.value.toFloat(),
                pitch.value.toFloat()
            )
        }
    }.addDependency(etherwarpSound)

    override fun init() = addSettings(
        SeperatorSetting("Left Click Etherwarp"),
        leftClickEtherwarp, swingHandToggle, autoSneakToggle, autoSneakDelaySlider,
        SeperatorSetting("Etherwarp Sound"),
        etherwarpSound, soundName, volume, pitch, playSound
    )

    @SubscribeEvent
    fun onSound(event: SoundPlayEvent) {
        if (! etherwarpSound.value) return
        if (event.name != "mob.enderdragon.hit") return
        if (event.pitch != 0.53968257f) return
        event.isCanceled = true
        playSound.defaultValue.run()
    }

    @SubscribeEvent
    fun onLeftClick(event: MouseEvent) {
        if (! leftClickEtherwarp.value) return
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