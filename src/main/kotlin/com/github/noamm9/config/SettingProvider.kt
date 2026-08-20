package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.ActionConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.SoundConfig
import com.github.noamm9.utils.ThreadUtils
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvent
import kotlin.reflect.KProperty

interface SettingProvider {
    val configSettings: MutableSet<ConfigHolder<*>>

    fun createSoundSettings(name: String, sound: SoundEvent, showIf: () -> Boolean = { true }): ActionConfig {
        val sound = SoundConfig(name, sound).withDescription("The internal Minecraft sound key to play.").showIf(showIf)
        val volume = NumberConfig("Volume", 0.5f, 0f, 1f, 0.1f).withDescription("The loudness of the sound.").showIf(showIf)
        val pitch = NumberConfig("Pitch", 1f, 0f, 2f, 0.1f).withDescription("The pitch/frequency of the sound.").showIf(showIf)
        val play = ActionConfig("Play Sound", false) {
            ThreadUtils.runOnMcThread { repeat(5) { NoammAddons.mc.soundManager.play(SimpleSoundInstance.forUI(sound.value, pitch.value, volume.value)) } }
        }.withDescription("Click to test the current sound configuration.").showIf(showIf)

        configSettings.add(sound)
        configSettings.add(volume)
        configSettings.add(pitch)
        configSettings.add(play)

        return play
    }

    operator fun <T, S: ConfigHolder<T>> S.provideDelegate(thisRef: SettingProvider, prop: KProperty<*>): S {
        thisRef.configSettings.add(this)
        return this
    }

    operator fun <T, S: ConfigHolder<T>> S.getValue(thisRef: SettingProvider, prop: KProperty<*>): S {
        return this
    }

    fun <T: ConfigHolder<*>> T.section(name: String): T {
        section = name
        return this
    }

    fun <T: ConfigHolder<*>> T.withDescription(desc: String): T {
        description = desc.let {
            return@let if (! it.endsWith('.')) "$it."
            else it
        }
        return this
    }

    fun <T, S: ConfigHolder<T>> S.onChange(listener: (T) -> Unit): S {
        changeListener = listener
        return this
    }

    fun <T: ConfigHolder<*>> T.showIf(condition: () -> Boolean): T {
        visibility = condition
        return this
    }

    fun <T: ConfigHolder<*>> T.hideIf(condition: () -> Boolean): T {
        visibility = { ! condition() }
        return this
    }

    fun <T: ConfigHolder<*>> T.jsonName(str: String): T {
        jsonName = str
        return this
    }
}