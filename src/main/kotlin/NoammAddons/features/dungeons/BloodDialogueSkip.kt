package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils.drawText
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import NoammAddons.utils.Utils.toFixed
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object BloodDialogueSkip {
    private const val bloodTimer = 24_000L
    private var startTime = System.currentTimeMillis() - bloodTimer
    private var isRunning = false

    private val bloodTitle = ChatUtils.Text(
        "&a$bloodTimer",
        (mc.getWidth() / 2 - (mc.fontRendererObj.getStringWidth("$bloodTimer") * 5) / 2).toDouble(),
        (mc.getHeight() / 2 - mc.getHeight() / 4).toDouble(),
        5.0
    )

    @SubscribeEvent
    fun startTimer(event: ClientChatReceivedEvent) {
        if (!config.BloodDialogueSkip) return
        if (event.type.toInt() != 0) return
        if (event.message.unformattedText.removeFormatting() != "The BLOOD DOOR has been opened!") return

        startTime = System.currentTimeMillis()
        isRunning = true
    }


    @SubscribeEvent
    fun onRenderOverlay(event: Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return
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
            bloodTitle.x = mc.getWidth()/2 - (mc.fontRendererObj.getStringWidth(bloodTitle.text.removeFormatting())) * 2.5

            drawText(
                bloodTitle.text.addColor(),
                bloodTitle.x,
                bloodTitle.y,
                bloodTitle.scale
            )

            if (timeLeft <= 0) isRunning = false
        }
    }
}

