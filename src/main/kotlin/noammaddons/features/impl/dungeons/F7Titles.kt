package noammaddons.features.impl.dungeons

import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import kotlin.math.roundToInt


object F7Titles: Feature(name = "F7 Titles", desc = "Custom Titles for f7 boss fight") {
    private val termRegex = Regex("^(\\w+) (?:activated|completed) a (\\w+)! \\((\\d)\\/(\\d)\\)\$") // https://regex101.com/r/lnW03M/1
    private val crystalRegex = Regex("^(\\d)\\/(\\d) Energy Crystals are now active!\$") // https://regex101.com/r/HrYH7P/1
    private var currentTitle = ChatUtils.title("", "", 0L, false)
    private var startTickTimer = false
    private var tickTimer = 0
    private var timerTime = 0
    private var maxorDead = false
    private var goldorDead = false
    private var necronDead = false
    private var goldorStart = false
    private var necronStart = false

    private val bossDeaths = listOf(
        Triple("Maxor", ::maxorDead, "&dMaxor Dead!"),
        Triple("Goldor", ::goldorDead, "&7Goldor Dead!"),
        Triple("Necron", ::necronDead, "&cNecron Dead!!")
    )

    private fun showTitle(subtitle: String, time: Int = 3) {
        currentTitle = ChatUtils.title("", subtitle, time.toLong() * 1000, false)
    }

    private fun showTitleWithSound(subtitle: String, time: Int = 3) {
        this.showTitle(subtitle, time)
        SoundUtils.Pling()
    }

    fun formatProgress(current: Int, max: Int): String {
        val bold = current == max && max > 2
        val boldSeperator = if (bold) "&l" else ""

        val minColor = when {
            max == 2 && current < max -> "&c"
            max == 1 && current == max -> "&b"
            bold -> "&6&l"
            current >= max * 0.75 -> "&a"
            current >= max * 0.5 -> "&e"
            else -> "&c"
        }

        val maxColor = when {
            max == 2 -> "&b"
            bold -> "&6&l"
            else -> "&a"
        }

        return "$minColor$current&r$boldSeperator/&r$maxColor$max"
    }

    private val termTitles by ToggleSetting("Terminal Titles")
    private val crystalTitles by ToggleSetting("Crystal Tittles")
    private val witherTitles by ToggleSetting("Wither Titles")
    private val lightningTimer by ToggleSetting("Lightning Timer")

    override fun init() {
        loop(100) { if (currentTitle.time > 0) currentTitle.time -= 100 }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        maxorDead = false
        goldorDead = false
        necronDead = false
        startTickTimer = false
        tickTimer = 0
        timerTime = 0
        goldorStart = false
        necronStart = false
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! startTickTimer) return
        timerTime --
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (dungeonFloorNumber != 7 || ! inBoss || ! witherTitles) return
        when (event.component.noFormatText) {
            "[BOSS] Maxor: YOU TRICKED ME!", "[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!" -> showTitleWithSound(subtitle = "&dMaxor Stunned!", time = 2)
            "[BOSS] Storm: Oof", "[BOSS] Storm: Ouch, that hurt!" -> showTitleWithSound(subtitle = "&bStorm Crushed!", time = 2)
            "[BOSS] Storm: I should have known that I stood no chance." -> showTitleWithSound(subtitle = "&bStorm Dead!", time = 2)
            "[BOSS] Necron: ARGH!" -> necronStart = true
            "The Core entrance is opening!" -> goldorStart = true
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (startTickTimer) {
            val timeLeft = ((timerTime - tickTimer) / 20.0)
            if (timeLeft <= 0) {
                startTickTimer = false
                tickTimer = 0
                showTitleWithSound("&aStorm's Lightning Ended!")
                return
            }

            drawCenteredText(
                "&l&c${timeLeft.toFixed(1)}",
                mc.getWidth() / 2f,
                mc.getHeight() / 2f - mc.getHeight() / 13f,
                3f
            )
        }

        if (currentTitle.time <= 0) return
        currentTitle.run { drawTitle("", "$subtitle") }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (dungeonFloorNumber != 7) return
        if (! inBoss) return
        if (event.packet !is S45PacketTitle) return
        val title = event.packet.message?.noFormatText?.takeIf { it.isNotBlank() } ?: return
        val titleType = event.packet.type

        if (titleType == S45PacketTitle.Type.SUBTITLE) {
            if (termTitles) {
                termRegex.find(title)?.run {
                    val (name, type, min, max) = destructured
                    val progress = formatProgress(min.toInt(), max.toInt())

                    val formattedType = when (type) {
                        "device" -> "&bDev&r "
                        "lever" -> "&cLever&r "
                        "terminal" -> "&5Term&r "
                        else -> return@run
                    }

                    showTitle(subtitle = "$formattedType($progress)", time = 4)
                    event.isCanceled = true
                    return
                }

                if (title == "The gate has been destroyed!") {
                    showTitleWithSound(subtitle = "&cGate Destroyed!", time = 2)
                    event.isCanceled = true
                    return
                }

                if (title == "The gate will open in 5 seconds!") {
                    showTitleWithSound(subtitle = "&c&lGATE!", time = 2)
                    event.isCanceled = true
                }
            }

            if (crystalTitles) {
                if (title == "The Energy Laser is charging up!") {
                    event.isCanceled = true
                    return
                }
                crystalRegex.find(title)?.run {
                    val (min, max) = destructured
                    val progress = formatProgress(min.toInt(), max.toInt())
                    showTitle("&3Crystal&r($progress)")
                    event.isCanceled = true
                    return
                }
            }

            if (witherTitles) {
                if (title == "The Energy Laser is charging up!") {
                    event.isCanceled = true
                    return
                }
                Regex("^⚠ (\\w+) is enraged! ⚠\$").find(title)?.destructured?.component1()?.let {
                    val color = if (it == "Storm") "&b" else if (it == "Maxor") "&5" else ""
                    showTitleWithSound(color + title)
                    event.isCanceled = true
                    return
                }
            }
        }

        if (titleType == S45PacketTitle.Type.TITLE && lightningTimer) {
            if (! title[0].isDigit()) return
            event.isCanceled = true

            if (startTickTimer || ! title[0].equalsOneOf('4', '6')) return
            startTickTimer = true
            // adding an extra 1.35 seconds for the lightning effect
            timerTime = when (title) {
                "4" -> 5.35 * 20
                "6" -> 7.35 * 20
                else -> return
            }.roundToInt()

            tickTimer = 0
        }
    }

    @SubscribeEvent
    fun onBossbar(event: BossbarUpdateEvent.Pre) {
        if (! witherTitles) return
        if (event.healthPresent != 0.33333334f) return

        bossDeaths.forEach { (boss, deadFlag, message) ->
            if (boss !in event.bossName || deadFlag.get()) return@forEach
            if (boss == "Goldor" && ! goldorStart) return
            if (boss == "Necron" && ! necronStart) return

            showTitleWithSound(subtitle = message, time = 2)
            deadFlag.set(true)
            return
        }
    }
}
