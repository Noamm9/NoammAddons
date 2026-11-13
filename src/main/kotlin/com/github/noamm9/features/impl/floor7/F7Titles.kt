package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.IFeatureWindow
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils.F7Phase
import com.github.noamm9.utils.location.LocationUtils.dungeonFloorNumber
import com.github.noamm9.utils.location.LocationUtils.inBoss
import com.github.noamm9.utils.render.Render2D.drawCenteredComponent
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

object F7Titles: Feature(name = "F7 Titles", description = "Custom Titles for F7 boss fight."), IFeatureWindow {
    private val crystalTitles by ToggleSetting("Crystal Titles").section("Boss")
    private val witherTitles by ToggleSetting("Wither Titles")
    private val lightningTimer by ToggleSetting("Lightning Timer")

    private val terminalTitles by ToggleSetting("Terminal Titles").withDescription("Reformats terminal, device, lever, and gate subtitles during phase 3").section("Terminals")
    private val terminalTitleDuration by SliderSetting("Duration", 2.5, 0.5, 6, 0.5).withDescription("Duration of the terminal title in seconds").showIf { terminalTitles.value }
    private val terminalTitleFormat by TextInputSetting("Format", "{name} {type} &f({progress}&f)").withDescription("Replaces {name}, {class}, {type}, and {progress} with the player name, dungeon class, terminal type, and phase progress. &bSupports color codes").showIf { terminalTitles.value }
    private val terminalPhaseDone by ToggleSetting("Phase Done").withDescription("Renders Phase Done instead of the completed progress").showIf { terminalTitles.value }
    private val terminalGateTitles by ToggleSetting("Gate Titles").withDescription("Also reformats gate-related subtitles").showIf { terminalTitles.value }

    private val crystalRegex = Regex("^(\\d)/(\\d) Energy Crystals are now active!$")
    private val enragedRegex = Regex("^⚠ (\\w+) is enraged! ⚠$")
    private val terminalRegex = Regex("(.+) (?:activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")

    private val terminalTitle = Title()
    private val crystalTitle = Title()
    private val witherTitle = Title()
    private val lightningTitle = Title()
    private var lightningTicks = 0L

    private var maxorDead = false
    private var goldorDead = false
    private var necronDead = false
    private var goldorStart = false
    private var necronStart = false

    override fun init() {
        hudElement(
            "Crystal Titles",
            { crystalTitles.value },
            { crystalTitle.text != Component.empty() },
            centered = { true }
        ) { ctx, example ->
            val text = if (example) Component.literal("&3Crystal&r(${formatProgress(1, 2)})".addColor()) else crystalTitle.text
            ctx.drawCenteredComponent(text, 0, 0)
            return@hudElement mc.font.width(text) to 9f
        } defaults {
            x = Resolution.width / 2f
            y = subtitleY()
            scale = 1.5f
        }

        hudElement(
            "Wither Titles",
            { witherTitles.value },
            { witherTitle.text != Component.empty() },
            centered = { true }
        ) { ctx, example ->
            val text = if (example) Component.literal("&dMaxor Stunned!".addColor()) else witherTitle.text
            ctx.drawCenteredComponent(text, 0, 0)
            return@hudElement mc.font.width(text) to 9f
        } defaults {
            x = Resolution.width / 2f
            y = subtitleY()
            scale = 1.5f
        }

        hudElement(
            "Lightning Timer",
            { lightningTimer.value },
            { lightningTicks > 0L },
            centered = { true }
        ) { ctx, example ->
            val text = if (example) "&l&c5.4" else "&l&c${((lightningTicks - DungeonListener.currentTime) / 20.0).toFixed(1)}"
            ctx.drawCenteredString(text, 0, 0)
            return@hudElement text.width() to 9f
        } defaults {
            x = Resolution.width / 2f
            y = Resolution.height / 2f - Resolution.height / 13f
            scale = 3f
        }

        hudElement(
            "Lightning Ended",
            { lightningTimer.value },
            { lightningTitle.text != Component.empty() },
            centered = { true }
        ) { ctx, example ->
            val text = if (example) Component.literal("&aStorm's Lightning Ended!".addColor()) else lightningTitle.text
            ctx.drawCenteredComponent(text, 0, 0)
            return@hudElement mc.font.width(text) to 9f
        } defaults {
            x = Resolution.width / 2f
            y = subtitleY()
            scale = 1.5f
        }

        hudElement(
            "Terminal Titles",
            { terminalTitles.value },
            { terminalTitle.text != Component.empty() },
            centered = { true }
        ) { ctx, example ->
            val comp = if (example) formatTerminalTitle(mc.user.name, "terminal", 6, 7) else terminalTitle.text
            ctx.drawCenteredComponent(comp, 0, 0)
            return@hudElement mc.font.width(comp) to 9f
        } defaults {
            x = Resolution.width / 2f
            y = Resolution.height * 0.45f
            scale = 1.5f
        }

        register<WorldChangeEvent> {
            maxorDead = false
            goldorDead = false
            necronDead = false
            goldorStart = false
            necronStart = false
            resetTitles()
        }

        register<TickEvent.Server> {
            if (lightningTicks <= 0L || DungeonListener.currentTime < lightningTicks) return@register
            lightningTicks = 0L
            if (! lightningTimer.value) return@register
            lightningTitle.show("&aStorm's Lightning Ended!")
            SoundUtils.playSound(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
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
                            crystalTitle.show("&3Crystal&r($progress)")
                            event.isCanceled = true
                            return@register
                        }
                    }

                    if (witherTitles.value) enragedRegex.find(text)?.destructured?.component1()?.let { boss ->
                        val color = when (boss) {
                            "Storm" -> "&b"
                            "Maxor" -> "&5"
                            else -> ""
                        }
                        showTitle("$color$text")
                        event.isCanceled = true
                    }

                    if (terminalTitles.value && F7Phase == 3 && handleTerminalTitle(text)) {
                        event.isCanceled = true
                    }
                }

                is ClientboundSetTitleTextPacket -> {
                    if (! lightningTimer.value) return@register
                    val text = packet.text.unformattedText.takeUnless(String::isBlank) ?: return@register
                    val number = text.toIntOrNull() ?: return@register
                    event.isCanceled = true

                    if (lightningTicks == 0L && (number == 4 || number == 6)) {
                        lightningTicks = DungeonListener.currentTime + (number * 1.35 * 20.0).toLong()
                        lightningTitle.clear()
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

    override fun onDisable() {
        super.onDisable()
        resetTitles()
    }

    private fun resetTitles() {
        crystalTitle.clear()
        witherTitle.clear()
        lightningTitle.clear()
        terminalTitle.clear()
        lightningTicks = 0L
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

    private fun showTerminalTitle(text: MutableComponent) = terminalTitle.show(text, terminalTitleDuration.value.toDouble() * 20)
    private fun showTerminalTitle(text: String) = showTerminalTitle(Component.literal(text.addColor()))
    private fun formatTerminalTitle(name: String, type: String, min: Int, max: Int): MutableComponent {
        if (terminalPhaseDone.value && min == max) return Component.literal("&a&lPhase Done!".addColor())

        val formattedType = Component.literal(when (type) {
            "terminal" -> "&5Terminal"
            "device" -> "&bDevice"
            "lever" -> "&cLever"
            else -> ""
        }.addColor())

        val clazz = DungeonListener.dungeonTeammates.find { it.name == name }?.clazz
        val classColor = clazz?.color ?: DungeonClass.Empty.color
        val formattedName = Component.literal(name).withColor(classColor.rgb)
        val formattedClass = Component.literal(clazz?.name ?: "Unknown").withColor(classColor.rgb)
        val formattedProgress = Component.literal("${ColorUtils.colorCodeByPercent(min, max)}$min&8/&a$max".addColor())

        return terminalTitleFormat.value.formatComponents(mapOf(
            "{name}" to formattedName,
            "{class}" to formattedClass,
            "{type}" to formattedType,
            "{progress}" to formattedProgress
        ))
    }

    private fun showTitle(subtitle: String) {
        witherTitle.show(subtitle)
        SoundUtils.playSound(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
    }

    private fun subtitleY() = Resolution.height / 2f - (Resolution.height * 0.056f).roundToInt() + (Resolution.height / 15.42f).roundToInt()

    private fun formatProgress(current: Int, max: Int): String {
        val minColor = if (current == max) "&b" else "&c"
        return "$minColor$current&r/&r&b$max&r"
    }

    private class Title {
        var text = Component.empty()
            private set

        private var id = 0

        fun show(text: MutableComponent, ticks: Number = 40) {
            this.text = text
            val cid = ++ id
            ThreadUtils.scheduledTask(ticks) { if (id == cid) this.text = Component.empty() }
        }
        fun show(text: String, ticks: Number = 40) = show(Component.literal(text.addColor()), ticks)

        fun clear() {
            text = Component.empty()
            id ++
        }
    }
}