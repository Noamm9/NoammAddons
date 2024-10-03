package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.RenderUtils.drawCenteredText


object BloodDialogueSkip {
    private const val bloodTimer = 24_000L
    private var startTime = System.currentTimeMillis() - bloodTimer
    private var isRunning = false

    private val bloodTitle = ChatUtils.Text(
	    "&a$bloodTimer",
	    mc.getWidth()/2f,
        mc.getHeight()/2f - mc.getHeight() / 4f,
	    5f
    )

    @SubscribeEvent
    fun startTimer(event: Chat) {
        if (!config.BloodDialogueSkip) return
        if (event.component.unformattedText.removeFormatting() != "The BLOOD DOOR has been opened!") return

        startTime = System.currentTimeMillis()
        isRunning = true
    }


    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (isRunning) {
            val currentTime = System.currentTimeMillis()
            val timeLeft = ((bloodTimer - (currentTime - startTime)) / 1000.0).toFloat()
            val timeString = when {
                timeLeft > 18 -> "&a${timeLeft.toFixed(2)}"
                timeLeft > 10 -> "&e${timeLeft.toFixed(2)}"
                timeLeft > 5 -> "&c${timeLeft.toFixed(2)}"
                else -> "&4${timeLeft.toFixed(2)}"
            }

            bloodTitle.text = timeString.addColor()
            bloodTitle.x = mc.getWidth()/2f

            drawCenteredText(bloodTitle.text, bloodTitle.x, bloodTitle.y, bloodTitle.scale)

            if (timeLeft <= 0) isRunning = false
        }
    }
}

