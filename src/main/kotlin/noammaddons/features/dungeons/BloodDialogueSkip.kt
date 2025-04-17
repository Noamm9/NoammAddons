package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.core.map.RoomState
import noammaddons.features.dungeons.dmap.core.map.RoomType
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.SoundUtils


object BloodDialogueSkip: Feature() {
    private const val bloodTimer = 24_000L
    private var startTime = System.currentTimeMillis() - bloodTimer
    private var isRunning = false

    @SubscribeEvent
    fun onRoomStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (! config.BloodDialogueSkip) return
        if (isRunning) return
        if (thePlayer?.clazz != DungeonUtils.Classes.Mage) return
        if (event.room.data.type != RoomType.BLOOD) return
        if (event.newState != RoomState.DISCOVERED) return

        startTime = System.currentTimeMillis()
        isRunning = true
    }


    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! isRunning) return
        val currentTime = System.currentTimeMillis()
        val timeLeft = ((bloodTimer - (currentTime - startTime)) / 1000.0).toFloat()
        if (timeLeft <= 0) {
            isRunning = false
            showTitle("&bTime's up!", "&cKill the blood Mobs", 4f, false)
            SoundUtils.Pling()
            return
        }

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
    }
}
