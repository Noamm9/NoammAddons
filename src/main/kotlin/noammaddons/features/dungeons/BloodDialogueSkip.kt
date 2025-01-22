package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.SoundUtils.Pling


object BloodDialogueSkip: Feature() {
    private const val bloodTimer = 24_000L
    private var startTime = System.currentTimeMillis() - bloodTimer
    private var isRunning = false

    @SubscribeEvent
    fun startTimer(event: Chat) {
        if (! config.BloodDialogueSkip) return
        if (thePlayer?.clazz?.name != "Mage") return
        if (event.component.noFormatText != "The BLOOD DOOR has been opened!") return

        startTime = System.currentTimeMillis()
        isRunning = true
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (isRunning) {
            val currentTime = System.currentTimeMillis()
            val timeLeft = ((bloodTimer - (currentTime - startTime)) / 1000.0).toFloat()

            drawCenteredText(
                when {
                    timeLeft > 18 -> "&a${timeLeft.toFixed(2)}"
                    timeLeft > 10 -> "&e${timeLeft.toFixed(2)}"
                    timeLeft > 5 -> "&c${timeLeft.toFixed(2)}"
                    else -> "&4${timeLeft.toFixed(2)}"
                },
                mc.getWidth() / 2f,
                mc.getHeight() / 2 - mc.getHeight() / 4,
                5f
            )

            if (timeLeft <= 0) {
                isRunning = false
                showTitle("Time's up!", "Kill the blood Mobs", 4f, true)
                Pling.start()
            }
        }
    }
}
