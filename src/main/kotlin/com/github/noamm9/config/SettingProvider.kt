package com.github.noamm9.config

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.SoundSetting
import gg.essential.universal.USound
import net.minecraft.sounds.SoundEvent
import kotlin.reflect.KProperty

interface SettingProvider {
    val configSettings: MutableSet<ConfigHolder<*>>

    fun getSettingByName(key: String?) = configSettings.find { it.jsonName == key && it is Savable }

    fun createSoundSettings(name: String, sound: SoundEvent, showIf: () -> Boolean = { true }): ButtonSetting {
        val sound = SoundSetting(name, sound).withDescription("The internal Minecraft sound key to play.").showIf(showIf)
        val volume = SliderSetting("Volume", 0.5f, 0f, 1f, 0.1f).withDescription("The loudness of the sound.").showIf(showIf)
        val pitch = SliderSetting("Pitch", 1f, 0f, 2f, 0.1f).withDescription("The pitch/frequency of the sound.").showIf(showIf)
        val play = ButtonSetting("Play Sound", false) {
            mc.execute { repeat(5) { USound.playSoundStatic(sound.value, volume.value, pitch.value) } }
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