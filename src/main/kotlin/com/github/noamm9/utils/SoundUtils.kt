package com.github.noamm9.utils

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

object SoundUtils {
    val MAP = BuiltInRegistries.SOUND_EVENT.associate {
        it.location to prettyName(it.location)
    }

    private fun prettyName(loc: Identifier) = loc.path.split('.').flatMap { it.split("_") }.let { parts ->
        if (parts.size <= 3) parts.joinToString("_") { it.uppercase() }
        else parts.drop(parts.size - 3).joinToString("_") { it.uppercase() }
    }
}