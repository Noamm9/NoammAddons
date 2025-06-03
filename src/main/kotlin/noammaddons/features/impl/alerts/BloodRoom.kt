package noammaddons.features.impl.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.core.map.RoomState
import noammaddons.features.impl.dungeons.dmap.core.map.RoomType
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.SoundUtils


object BloodRoom: Feature() {
    private val bloodDialogSkip = ToggleSetting("Kill Mobs Timer", false)
    private val mage = ToggleSetting("Only as Mage", true).addDependency(bloodDialogSkip)
    private val bloodDone = ToggleSetting("Blood Done Alert", false)

    override fun init() = addSettings(bloodDialogSkip, mage, bloodDone)

    private val regex = Regex("^\\[BOSS] The Watcher: (That will be enough for now|You have proven yourself. You may pass\\.)$")
    private const val bloodTimer = 24_000L
    private var startTime = System.currentTimeMillis() - bloodTimer
    private var isRunning = false

    @SubscribeEvent
    fun onRoomStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (isRunning) return
        if (thePlayer?.clazz != DungeonUtils.Classes.Mage && mage.value) return
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

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! bloodDone.value) return
        if (! inDungeon) return
        if (! event.component.noFormatText.matches(regex)) return
        showTitle("§1[§6§kO§r§1] §dB§bl§do§bo§dd §bD§do§bn§de §1[§6§kO§r§1]")
        mc.thePlayer !!.playSound("random.orb", 1f, 0.5f)
    }
}
