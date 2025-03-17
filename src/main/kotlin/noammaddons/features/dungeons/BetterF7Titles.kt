package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.SoundUtils.Pling
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import kotlin.math.roundToInt

object BetterF7Titles: Feature() {
    private var currentTitle = ChatUtils.title("", "", 0L, false)
    private var startTickTimer = false
    private var tickTimer = 0
    private var timerTime = 0
    private var maxorDead = false
    private var goldorDead = false
    private var necronDead = false
    private var goldorStart = false
    private var necronStart = false
    private val progressRegex = Regex("\\d+/\\d+")
    private val replacements = mapOf(
        "1/7" to "&c1&r/&a7", "1/8" to "&c1&r/&a8", "2/8" to "&c2&r/&a8", "2/7" to "&c2&r/&a7",
        "1/2" to "&c1&r/&b2", "2/2" to "&b2&r/&b2", "3/" to "&c3&r/&a", "4/" to "&e4&r/&a",
        "5/" to "&e5&r/&a", "6/" to "&e6&r/&a", "7/8" to "&a7&r/&a8", "7/7" to "&6&l7&r&l/&6&l7",
        "8/8" to "&6&l8&r&l/&6&l8"
    )
    private val termCrystalRegex = Regex(
        "activated a terminal! \\((\\d+/\\d+)\\)|completed a device! \\((\\d+/\\d+)\\)|" +
                "activated a lever! \\((\\d+/\\d+)\\)|(\\d+/\\d+) Energy Crystals are now active!"
    )

    private fun colorTerminalInfo(progress: String): String {
        return replacements.entries.fold(progress) { acc, (key, value) ->
            acc.replace(key, value)
        } + "&r"
    }

    private fun showTitle(subtitle: String, time: Int = 3) {
        currentTitle = ChatUtils.title("", subtitle, time.toLong() * 1000, false)
        Pling.start()
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.BetterF7Titles || dungeonFloorNumber != 7 || ! inBoss) return
        val msg = event.component.noFormatText

        termCrystalRegex.find(msg)?.let { matchResult ->
            val progress = progressRegex.find(matchResult.value)?.value ?: return@let
            val type = when {
                "device" in matchResult.value -> "&bDev&r "
                "lever" in matchResult.value -> "&cLever&r "
                "terminal" in matchResult.value -> "&5Term&r "
                "Crystal" in matchResult.value -> "&3Crystal&r "
                else -> return@let
            }

            showTitle(subtitle = "$type(${colorTerminalInfo(progress)})", time = 4)
            return
        }

        when (msg) {
            "[BOSS] Maxor: YOU TRICKED ME!", "[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!" -> showTitle(subtitle = "&dMaxor Stunned!", time = 2)
            "[BOSS] Storm: Oof", "[BOSS] Storm: Ouch, that hurt!" -> showTitle(subtitle = "&bStorm Crushed!", time = 2)
            "[BOSS] Storm: I should have known that I stood no chance." -> showTitle(subtitle = "&bStorm Dead!", time = 2)
            "The gate has been destroyed!" -> showTitle(subtitle = "&cGate Destroyed!", time = 2)
            "[BOSS] Necron: ARGH!" -> necronStart = true
            "The Core entrance is opening!" -> goldorStart = true
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderOverlay) {
        if (startTickTimer) {
            val timeLeft = ((timerTime - tickTimer) / 20.0)
            if (timeLeft <= 0) {
                startTickTimer = false
                tickTimer = 0
                showTitle("&aStorm's Lightning Ended!")
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
        currentTitle.run { drawTitle("$title", "$subtitle") }
    }

    init {
        loop(100) {
            currentTitle.time -= 100
        }
    }

    @SubscribeEvent
    fun onTitle(event: RenderTitleEvent) {
        if (! config.BetterF7Titles) return
        if (dungeonFloorNumber != 7) return
        if (! inBoss) return
        event.isCanceled = true

        if (event.title.removeFormatting().equalsOneOf("6", "4") && ! startTickTimer) {
            startTickTimer = true
            timerTime = when (event.title.removeFormatting()) {
                // adding extra 1.35 seconds for the lightning effect
                "4" -> 5.35 * 20
                "6" -> 7.35 * 20
                else -> return
            }.roundToInt()
            tickTimer = 0
        }
    }

    @SubscribeEvent
    fun onTick(event: ServerTick) {
        if (! startTickTimer) return
        tickTimer ++
    }

    @SubscribeEvent
    fun onBossbar(event: BossbarUpdateEvent.Pre) {
        if (! config.BetterF7Titles) return
        if (event.healthPresent != 0.33333334f) return

        val bossDeaths = listOf(
            Triple("Maxor", ::maxorDead, "&dMaxor Dead!"),
            Triple("Goldor", ::goldorDead, "&7Goldor Dead!"),
            Triple("Necron", ::necronDead, "&cNecron Dead!!")
        )

        bossDeaths.forEach { (boss, deadFlag, message) ->
            if (event.bossName.contains(boss) && ! deadFlag.get()) {
                if (boss == "Goldor" && ! goldorStart) return
                if (boss == "Necron" && ! necronStart) return

                showTitle(subtitle = message, time = 2)
                deadFlag.set(true)
                return
            }
        }
    }

    @SubscribeEvent
    fun reset(event: WorldUnloadEvent) {
        if (! config.BetterF7Titles) return

        maxorDead = false
        goldorDead = false
        necronDead = false
        startTickTimer = false
        tickTimer = 0
        timerTime = 0
        goldorStart = false
        necronStart = false
    }
}
