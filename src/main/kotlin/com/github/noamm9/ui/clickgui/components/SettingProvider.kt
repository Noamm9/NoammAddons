package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.clickgui.components.impl.*
import com.github.noamm9.utils.ThreadUtils
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvent
import kotlin.reflect.KProperty

interface SettingProvider {
    val configSettings: MutableSet<Setting<*>>

    fun createSoundSettings(name: String, sound: SoundEvent, showIf: () -> Boolean = { true }): ButtonSetting {
        val sound = SoundSetting(name, sound).withDescription("The internal Minecraft sound key to play.").showIf(showIf)
        val volume = SliderSetting("Volume", 0.5f, 0f, 1f, 0.1f).withDescription("The loudness of the sound.").showIf(showIf)
        val pitch = SliderSetting("Pitch", 1f, 0f, 2f, 0.1f).withDescription("The pitch/frequency of the sound.").showIf(showIf)
        val play = ButtonSetting("Play Sound", false) {
            ThreadUtils.runOnMcThread { repeat(5) { mc.soundManager.play(SimpleSoundInstance.forUI(sound.value, pitch.value, volume.value)) } }
        }.withDescription("Click to test the current sound configuration.").showIf(showIf)

        configSettings.add(sound)
        configSettings.add(volume)
        configSettings.add(pitch)
        configSettings.add(play)

        return play
    }

    operator fun <T, S: Setting<T>> S.provideDelegate(thisRef: SettingProvider, prop: KProperty<*>): S {
        this.headerName?.let { name ->
            val category = CategorySetting(name)
            val sectionVisibility = {
                thisRef.configSettings.asSequence()
                    .dropWhile { it !== category }
                    .drop(1)
                    .takeWhile { it !is SeparatorSetting && it !is CategorySetting }
                    .any { it.visibility() }
            }

            if (thisRef.configSettings.isNotEmpty()) {
                thisRef.configSettings.add(SeparatorSetting().also { it.visibility = sectionVisibility })
            }
            thisRef.configSettings.add(category.also { it.visibility = sectionVisibility })
        }

        thisRef.configSettings.add(this)
        return this
    }

    operator fun <T, S: Setting<T>> S.getValue(thisRef: SettingProvider, prop: KProperty<*>): S {
        return this
    }

    fun <T: Setting<*>> T.section(name: String): T {
        this.headerName = name
        return this
    }

    fun <T: Setting<*>> T.withDescription(desc: String): T {
        this.description = desc.let {
            return@let if (! it.endsWith('.')) "$it."
            else it
        }
        return this
    }

    fun <T, S: Setting<T>> S.onChange(listener: (T) -> Unit): S {
        this.changeListener = listener
        return this
    }

    fun <T: Setting<*>> T.showIf(condition: () -> Boolean): T {
        this.visibility = condition
        return this
    }

    fun <T: Setting<*>> T.hideIf(condition: () -> Boolean): T {
        this.visibility = { ! condition() }
        return this
    }
}