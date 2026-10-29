package com.github.noamm9.config

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.types.*
import com.github.noamm9.utils.SoundUtils
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
            mc.execute { repeat(5) { SoundUtils.playSound(sound.value, volume.value, pitch.value) } }
        }.withDescription("Click to test the current sound configuration.").showIf(showIf)

        configSettings.add(sound)
        configSettings.add(volume)
        configSettings.add(pitch)
        configSettings.add(play)

        return play
    }

    operator fun <T, S: ConfigHolder<T>> S.provideDelegate(thisRef: SettingProvider, prop: KProperty<*>): S = apply { thisRef.configSettings.add(this) }
    operator fun <T, S: ConfigHolder<T>> S.getValue(thisRef: SettingProvider, prop: KProperty<*>): S = this

    fun <T: ConfigHolder<*>> T.section(name: String): T = apply { section = name }
    fun <T: ConfigHolder<*>> T.withDescription(desc: String): T = apply { description = desc.let { if (! it.endsWith('.')) "$it." else it } }
    fun <T, S: ConfigHolder<T>> S.onChange(listener: (T) -> Unit): S = apply { changeListener = listener }
    fun <T: ConfigHolder<*>> T.showIf(condition: () -> Boolean): T = apply { visibility = condition }
    fun <T: ConfigHolder<*>> T.hideIf(condition: () -> Boolean): T = apply { visibility = { ! condition() } }
    fun <T: ConfigHolder<*>> T.hidden() = apply { visibility = { false } }
    fun <T: ConfigHolder<*>> T.jsonName(str: String): T = apply { jsonName = str }
}