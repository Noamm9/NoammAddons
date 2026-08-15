package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocationUtils.dungeonFloorNumber
import com.github.noamm9.utils.location.LocationUtils.inBoss
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object F7Titles: Feature(name = "F7 Titles", description = "Custom Titles for F7 boss fight.") {
    private val crystalTitles by ToggleSetting("Crystal Titles")
    private val witherTitles by ToggleSetting("Wither Titles")
    private val lightningTimer by ToggleSetting("Lightning Timer")
    private val terminalTitles by ToggleSetting("Terminal Titles")
        .withDescription("Reformats terminal, device, lever, and gate subtitles during phase 3")
        .onChange { terminalTitlesEnabled ->
            if (enabled && terminalTitlesEnabled) terminalTitleTickListener.register()
            else terminalTitleTickListener.unregister()
        }
    private val terminalTitleDuration by SliderSetting("Duration", 2.5, 0.5, 6, 0.5)
        .withDescription("Duration of the terminal title in seconds")
        .showIf { terminalTitles.value }
    private val terminalTitleMode by DropdownSetting("Mode", 0, listOf("Name + Term + Progress", "Term + Progress", "Progress"))
        .withDescription("Controls which information appears in terminal titles")
        .showIf { terminalTitles.value }
    private val terminalTitleBracket by DropdownSetting("Bracket Type", 0, listOf("()", "[]", "<>", "{}"))
        .withDescription("Changes the brackets around terminal progress")
        .showIf { terminalTitles.value }
    private val terminalPhaseDone by ToggleSetting("Phase Done")
        .withDescription("Renders Phase Done instead of 7/7 or 8/8")
        .showIf { terminalTitles.value }
    private val terminalGateTitles by ToggleSetting("Gate Titles")
        .withDescription("Also reformats gate-related subtitles")
        .showIf { terminalTitles.value }

    private val crystalRegex = Regex("^(\\d)/(\\d) Energy Crystals are now active!$")
    private val enragedRegex = Regex("^⚠ (\\w+) is enraged! ⚠$")
    private val terminalRegex = Regex("(.+) (?:activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")

    private var timerTime = 0L
    private var maxorDead = false
    private var goldorDead = false
    private var necronDead = false
    private var goldorStart = false
    private var necronStart = false
    private var terminalTitle = ""
    private var terminalTitleTimer = 0

    private val terminalTitleHud = object: HudElement() {
        override val name = "Terminal Titles"
        override val toggle get() = enabled && terminalTitles.value
        override val shouldDraw get() = terminalTitle.isNotBlank()

        override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float> {
            val str = if (example) formatTerminalTitle(mc.user.name, "terminal", 6, 7) else terminalTitle
            ctx.drawCenteredString(str, 0, 0)
            return str.width().toFloat() to 9f
        }

        override fun isHovered(mx: Int, my: Int): Boolean {
            val halfWidth = width * scale / 2
            return mx >= x - halfWidth && mx <= x + halfWidth && my >= y && my <= y + (height * scale)
        }

        override fun drawBackground(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
            val scaledW = width * scale
            val scaledH = height * scale
            val drawX = x - scaledW / 2
            val drawY = y

            val hovered = mx >= drawX && mx <= drawX + scaledW && my >= drawY && my <= drawY + scaledH
            val borderColor = if (isDragging || hovered) Style.accentColor else Color(255, 255, 255, 40)

            ctx.drawRect(drawX, drawY, scaledW.toDouble(), scaledH.toDouble(), Color(10, 10, 10, 150))
            ctx.drawRect(drawX, drawY, scaledW.toDouble(), 1.0, borderColor)
            ctx.drawRect(drawX, drawY + scaledH - 1, scaledW.toDouble(), 1.0, borderColor)
        }
    }

    override fun init() {
        hudElements.add(terminalTitleHud)

        register<WorldChangeEvent> {
            maxorDead = false
            goldorDead = false
            necronDead = false
            goldorStart = false
            necronStart = false
            timerTime = 0L
            timerRenderer.unregister()
        }

        register<ChatMessageEvent> {
            if (dungeonFloorNumber != 7 || ! inBoss || ! witherTitles.value) return@register
            when (event.unformattedText) {
                "[BOSS] Maxor: YOU TRICKED ME!", "[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!" -> showTitle("&dMaxor Stunned!")
                "[BOSS] Storm: Oof", "[BOSS] Storm: Ouch, that hurt!" -> showTitle("&bStorm Crushed!")
                "[BOSS] Storm: I should have known that I stood no chance." -> showTitle("&bStorm Dead!")
                "[BOSS] Necron: ARGH!" -> necronStart = true
                "The Core entrance is opening!" -> goldorStart = true
            }
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (dungeonFloorNumber != 7 || ! inBoss) return@register

            when (val packet = event.packet) {
                is ClientboundSetSubtitleTextPacket -> {
                    val text = packet.text.unformattedText
                    if (text.isBlank()) return@register

                    if (crystalTitles.value) {
                        if (text == "The Energy Laser is charging up!") {
                            event.isCanceled = true
                            return@register
                        }

                        crystalRegex.find(text)?.destructured?.let { (min, max) ->
                            val progress = formatProgress(min.toInt(), max.toInt())
                            ChatUtils.showTitle(subtitle = "&3Crystal&r($progress)")
                            event.isCanceled = true
                            return@register
                        }
                    }

                    enragedRegex.find(text)?.destructured?.component1()?.let { boss ->
                        val color = when (boss) {
                            "Storm" -> {
                                mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
                                "&b"
                            }

                            "Maxor" -> "&5"
                            else -> ""
                        }
                        showTitle("$color$text")
                        event.isCanceled = true
                    }

                    if (terminalTitles.value && LocationUtils.inDungeon && LocationUtils.F7Phase == 3 && handleTerminalTitle(text)) {
                        event.isCanceled = true
                    }
                }

                is ClientboundSetTitleTextPacket -> {
                    if (! lightningTimer.value) return@register

                    val text = packet.text.unformattedText
                    if (text.isBlank()) return@register
                    val number = text.toIntOrNull() ?: return@register
                    event.isCanceled = true

                    if (! timerRenderer.isActive && (number == 4 || number == 6)) {
                        timerTime = DungeonListener.currentTime + (number * 1.35 * 20.0).toLong()
                        timerRenderer.register()
                    }
                }
            }
        }

        register<BossBarUpdateEvent> {
            if (! witherTitles.value) return@register
            if (dungeonFloorNumber != 7 || ! inBoss) return@register
            if (event.progress > 0f) return@register
            val name = event.name.unformattedText
            val entry = DungeonListener.bossEntryTime?.ticks ?: return@register

            if (name.contains("Maxor") && ! maxorDead && DungeonListener.currentTime - entry > 120) {
                maxorDead = true
                showTitle("&dMaxor Dead!")
            }
            else if (name.contains("Goldor") && ! goldorDead && goldorStart) {
                goldorDead = true
                showTitle("&7Goldor Dead!")
            }
            else if (name.contains("Necron") && ! necronDead && necronStart) {
                necronDead = true
                showTitle("&cNecron Dead!!")
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        if (! terminalTitles.value) terminalTitleTickListener.unregister()
    }

    private val timerRenderer = EventBus.listener<RenderOverlayEvent> {
        if (! enabled) return@listener
        val timeLeft = (timerTime - DungeonListener.currentTime) / 20.0

        if (timeLeft <= 0) {
            this.listener.unregister()
            showTitle("&aStorm's Lightning Ended!")
            return@listener
        }

        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight

        event.context.drawCenteredString(
            "&l&c${timeLeft.toFixed(1)}",
            width / 2f,
            height / 2f - height / 13f,
            scale = 3f
        )
    }.unregister()

    private val terminalTitleTickListener = register<TickEvent.Start> {
        if (terminalTitleTimer <= 0) {
            this.listener.unregister()
            terminalTitle = ""
        }

        terminalTitleTimer -= 50
    }

    private fun handleTerminalTitle(title: String): Boolean {
        if (terminalGateTitles.value) when (title) {
            "The gate has been destroyed!" -> {
                showTerminalTitle("&cGate Destroyed!")
                return true
            }

            "The gate will open in 5 seconds!" -> {
                showTerminalTitle("&c&lGATE!")
                return true
            }
        }

        val (name, type, min, max) = terminalRegex.find(title)?.destructured ?: return false
        showTerminalTitle(formatTerminalTitle(name, type, min.toInt(), max.toInt()))
        return true
    }

    private fun showTerminalTitle(title: String) {
        terminalTitle = title
        terminalTitleTimer = terminalTitleDuration.value.toInt() * 1000
        terminalTitleTickListener.register()
    }

    private fun formatTerminalTitle(name: String, type: String, min: Int, max: Int): String {
        val color = ColorUtils.colorCodeByPercent(min, max)
        if (terminalPhaseDone.value && min == max) return "&a&lPhase Done!"
        val brackets = when (terminalTitleBracket.value) {
            0 -> listOf("(", ")")
            1 -> listOf("[", "]")
            2 -> listOf("<", ">")
            3 -> listOf("{", "}")
            else -> listOf("", "")
        }

        val formattedType = when (type) {
            "terminal" -> "&5Terminal"
            "device" -> "&bDevice"
            "lever" -> "&cLever"
            else -> ""
        }

        val formattedName = (DungeonListener.dungeonTeammates.find { it.name == name }?.clazz?.code ?: "&7") + name

        return when (terminalTitleMode.value) {
            0 -> "$formattedName $formattedType &f${brackets[0]}$color$min&8/&a$max&f${brackets[1]}"
            1 -> "$formattedType &f${brackets[0]}$color$min&f/&a$max&f${brackets[1]}"
            2 -> "&f${brackets[0]}$color$min&f/&a$max&f${brackets[1]}"
            else -> ""
        }
    }

    private fun showTitle(subtitle: String) {
        ChatUtils.showTitle(subtitle = subtitle)
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
    }

    private fun formatProgress(current: Int, max: Int): String {
        val minColor = if (current == max) "&b" else "&c"
        return "$minColor$current&r/&r&b$max&r"
    }
}