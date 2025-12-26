package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText


object TickTimers: Feature("Shows various types of server tick timers for F7 boss fight") {
    private val showPrefix = ToggleSetting("Prefix", true)
    private val showSuffix = ToggleSetting("Suffix", true)
    private val format = DropdownSetting("Format", listOf("Seconds", "Ticks"), 0)

    private val p1 = ToggleSetting("Maxor Start")
    private val p2 = ToggleSetting("Storm Start")
    private val p3 = ToggleSetting("Goldor Start")
    private val p4 = ToggleSetting("Necron Start")

    val goldorDeathTickTimer = ToggleSetting("Goldor Death Ticks")
    val padTimer = ToggleSetting("Storm Pad Timer")

    override fun init() = addSettings(
        showPrefix, showSuffix, format,
        SeperatorSetting("Phase Timers"),
        p1, p2, p3, p4,
        goldorDeathTickTimer,
        padTimer
    )

    private var startTickTime = - 1
    private var goldorTickTime = - 1
    private var padTickTime = - 1
    private var storm = false


    private fun formatTimer(time: Int, max: Int, _prefix: String): String {
        val color = when {
            time.toFloat() >= max * 0.66 -> "&a"
            time.toFloat() >= max * 0.33 -> "&6"
            else -> "&c"
        }
        val timeDisplay = if (format.value == 1) time else (time / 20f).toFixed(2)
        val prefix = if (showPrefix.value) "$_prefix " else ""
        val suffix = if (showSuffix.value) if (format.value == 1) "t" else "s" else ""
        return prefix + color + timeDisplay + suffix
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText

        when {
            msg == "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" && p1.value -> startTickTime = 150
            msg == "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!" && p2.value -> startTickTime = 120
            msg == "[BOSS] Storm: I should have known that I stood no chance." -> {
                if (p3.value) startTickTime = 104
                if (goldorDeathTickTimer.value) goldorTickTime = 104 + 60
                if (storm) {
                    storm = false
                    padTickTime = - 1
                }
            }

            msg == "[BOSS] Necron: I'm afraid, your journey ends now." && p4.value -> startTickTime = 60
            msg == "The Core entrance is opening!" -> goldorTickTime = - 1
            msg == "[BOSS] Storm: Pathetic Maxor, just like expected." && padTimer.value -> {
                padTickTime = 20
                storm = true
            }
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        padTickTime = - 1
        goldorTickTime = - 1
        startTickTime - 1
        storm = false
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (startTickTime != - 1) startTickTime --

        if (padTickTime != - 1 && storm) padTickTime --
        if (padTickTime == 0 && storm) padTickTime = 20


        if (goldorTickTime != - 1) goldorTickTime --
        if (goldorTickTime == 0) goldorTickTime = 60
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! inBoss) return
        if (dungeonFloorNumber != 7) return

        val str = when {
            startTickTime != - 1 -> formatTimer(startTickTime, 150, "&aStart:")
            goldorTickTime != - 1 -> formatTimer(goldorTickTime, 60, "&7Goldor:")
            padTickTime != - 1 -> formatTimer(padTickTime, 20, "&bPad:")
            else -> return
        }

        drawCenteredText(
            str,
            mc.getWidth() / 2f,
            mc.getHeight() / 2f + 30f,
            1.5f
        )
    }
}


