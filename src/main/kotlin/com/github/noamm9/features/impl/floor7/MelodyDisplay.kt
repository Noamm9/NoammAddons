package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.hud.getValue
import com.github.noamm9.ui.hud.provideDelegate
import com.github.noamm9.utils.containsOneOf
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.sounds.SoundEvents

object MelodyDisplay: Feature("Displays the current progress someone for melody on screen.") {
    private val melodyFormat by TextInputSetting("Format", "{name} has {progress} melody")
        .withDescription("replaces {name} with the player name and {progress} to the melody progress. &bSupports code codes (&a &e etc..)")
    private val alertDuration by SliderSetting("Alert Duration", 2.5f, 0f, 5f, 0.1f)
    private val soundEnabled by ToggleSetting("Play sound", true).withDescription("Should it play a sound when someone gets melody?")
    private val sound = createSoundSettings("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { soundEnabled.value }

    private data class MelodyState(val name: String, val progress: Int, val timestamp: Long)

    private val melodyRegex = Regex("""Party > (?:\[[^]]+]\s)?(\w+):""")

    private var currentState: MelodyState? = null

    private val hud by hudElement("Melody Display", centered = true, shouldDraw = { LocationUtils.F7Phase == 3 }) { ctx, example ->
        val text = if (example) formatMessage(mc.user.name, 1)
        else {
            val state = currentState ?: return@hudElement 0f to 0f
            formatMessage(state.name, state.progress)
        }

        Render2D.drawCenteredString(ctx, text, 0, 0)

        return@hudElement text.width().toFloat() to 9f
    }

    private val timer = EventBus.register<TickEvent.Start> {
        val state = currentState ?: return@register
        val durationMillis = (alertDuration.value * 1000).toLong()

        if (System.currentTimeMillis() - state.timestamp > durationMillis) {
            currentState = null
            listener.unregister()
        }
    }

    override fun init() {
        register<ChatMessageEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            val message = event.unformattedText.takeIf { it.startsWith("Party > ") } ?: return@register
            val name = melodyRegex.find(message)?.groupValues?.get(1)?.takeUnless { it == mc.user.name } ?: return@register
            val progress = (4 downTo 0).find { i -> message.containsOneOf("$i/4", "${i * 25}%") } ?: return@register
            val color = DungeonPlayer.get(name)?.clazz?.code ?: "&7"

            currentState = MelodyState("$color$name&r", progress, System.currentTimeMillis())
            if (soundEnabled.value) sound.play.action.invoke()
            timer.register()
        }
    }

    private fun formatMessage(name: String, progress: Int): String {
        return melodyFormat.value
            .replace("{name}", name)
            .replace("{progress}", "$progress/4")
    }
}