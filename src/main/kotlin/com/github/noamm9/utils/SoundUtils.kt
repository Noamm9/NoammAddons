package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object SoundUtils {
    val MAP = BuiltInRegistries.SOUND_EVENT.associate {
        it.location to prettyName(it.location)
    }

    private fun prettyName(loc: Identifier) = loc.path.split('.').flatMap { it.split("_") }.let { parts ->
        if (parts.size <= 3) parts.joinToString("_") { it.uppercase() }
        else parts.drop(parts.size - 3).joinToString("_") { it.uppercase() }
    }

    fun playSound(sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) = mc.execute {
        mc.soundManager.play(SimpleSoundInstance.forUI(sound, pitch, volume))
    }

    fun playSound(sound: Holder<SoundEvent>, volume: Float = 1f, pitch: Float = 1f) = playSound(sound.value(), volume, pitch)
}