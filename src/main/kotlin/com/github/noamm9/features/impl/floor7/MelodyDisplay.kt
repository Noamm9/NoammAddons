package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.TextInputSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.containsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.sounds.SoundEvents

object MelodyDisplay: Feature("Displays the current progress someone for melody on screen.") {
    private val melodyFormat by TextInputSetting("Format", "{name} has {progress} melody").withDescription("replaces {name}, {class} & {progress} with the player name, dungeon class and melody progress. &bSupports code codes")
    private val alertDuration by SliderSetting("Alert Duration", 2.5f, 0f, 5f, 0.1f)
    private val soundEnabled by ToggleSetting("Play sound", true).withDescription("Should it play a sound when someone gets melody?")
    private val sound = createSoundSettings("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { soundEnabled.value }

    private data class MelodyState(val name: String, val clazz: String, val progress: Int, val timestamp: Long)
    data class MelodyMessage(val name: String, val progress: Int)

    private val melodyRegex = Regex("""Party > (?:\[[^]]+]\s)?(\w+):""")
    private var currentState: MelodyState? = null

    private val timer = EventBus.listener<TickEvent.Start> {
        val state = currentState ?: return@listener
        val durationMillis = (alertDuration.value * 1000).toLong()

        if (System.currentTimeMillis() - state.timestamp > durationMillis) {
            currentState = null
            listener.unregister()
        }
    }

    override fun init() {
        hudElement("Melody Display", centered = true, shouldDraw = { LocationUtils.F7Phase == 3 }) { ctx, example ->
            val text = if (example) formatMessage(MelodyState(mc.user.name, "&bArcher", 1, 1))
            else {
                val state = currentState ?: return@hudElement 0f to 0f
                formatMessage(state)
            }

            ctx.drawCenteredString(text, 0, 0)

            return@hudElement text.width().toFloat() to 9f
        }

        register<ChatMessageEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            val (name, progress) = parseMelodyMessage(event.unformattedText) ?: return@register
            val clazz = DungeonListener.dungeonTeammates.find { it.name == name }?.clazz
            val color = clazz?.code ?: "&7"
            val clazzName = clazz?.name?.let { "$color$it&r" } ?: "&7Unknown&r"

            currentState = MelodyState("$color$name&r", clazzName, progress, System.currentTimeMillis())
            if (soundEnabled.value) sound.action.invoke()
            timer.register()
        }
    }

    fun parseMelodyMessage(message: String): MelodyMessage? {
        if (! message.startsWith("Party > ")) return null
        val name = melodyRegex.find(message)?.groupValues?.get(1)?.takeUnless { it == mc.user.name } ?: return null
        val progress = (4 downTo 0).find { i -> message.containsOneOf("$i/4", "${i * 25}%") } ?: return null

        return MelodyMessage(name, progress)
    }

    private fun formatMessage(state: MelodyState): String {
        return melodyFormat.value
            .replace("{name}", state.name)
            .replace("{class}", state.clazz)
            .replace("{progress}", "${state.progress}/4")
    }
}