package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.SoundUtils
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

class SoundSetting(name: String, defaultValue: SoundEvent): ConfigHolder<SoundEvent>(name, defaultValue), Savable {
    constructor(name: String, value: Holder.Reference<SoundEvent>): this(name, value.value())

    companion object {
        val allSounds by lazy {
            BuiltInRegistries.SOUND_EVENT.sortedByDescending { SoundUtils.MAP[it.location] }
        }

        fun getSound(loc: Identifier): Holder.Reference<SoundEvent>? {
            return BuiltInRegistries.SOUND_EVENT.get(loc).orElse(null)
        }
    }

    override fun write() = JsonPrimitive(value.location.toString())
    override fun read(element: JsonElement) {
        val loc = Identifier.tryParse(element.asString) ?: error("Could not parse Identifier: $element")
        val sound = getSound(loc) ?: error("Could not find a sound for Identifier: $element")
        value = sound.value()
    }

    fun prettyName(sound: SoundEvent) = SoundUtils.MAP[sound.location]
}