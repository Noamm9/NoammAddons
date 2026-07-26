package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.config.PogObject
import com.github.noamm9.features.Feature
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import kotlin.math.roundToInt

object SoundManager: Feature("Adjust volumes for every sound in the game") {
    val volumes = PogObject("noammaddons_sounds", mutableMapOf<String, Float>())
    private val recentSounds = LinkedHashMap<Identifier, Unit>(100, 0.75f, true)
    private var previewSound: SoundInstance? = null

    @Volatile
    private var latestSoundId: Identifier? = null

    @Volatile
    var recentSoundsVersion = 0L
        private set

    init {
        volumes.get().replaceAll { _, multiplier -> normalizePercent((multiplier * 100f).roundToInt()) / 100f }
    }

    @JvmStatic
    fun getMultiplier(id: String): Float {
        if (! enabled) return 1.0f
        return volumes.get().getOrDefault(id, 1f)
    }

    fun getVolumePercent(id: String): Int {
        return (volumes.get().getOrDefault(id, 1f) * 100f).roundToInt()
    }

    fun setVolumePercent(id: String, percent: Int) {
        val multiplier = normalizePercent(percent) / 100f
        if (volumes.get()[id] != multiplier) volumes.get()[id] = multiplier
    }

    fun playPreview(sound: SoundEvent) {
        val instance = SimpleSoundInstance.forUI(sound, 1f)
        previewSound = instance

        try {
            mc.soundManager.play(instance)
        }
        finally {
            previewSound = null
        }
    }

    @JvmStatic
    fun recordPlayedSound(sound: SoundInstance) {
        if (sound === previewSound) return

        val id = sound.identifier
        if (latestSoundId == id) return

        synchronized(recentSounds) {
            if (latestSoundId == id) return

            recentSounds[id] = Unit
            latestSoundId = id

            if (recentSounds.size > 100) {
                recentSounds.entries.iterator().run {
                    next()
                    remove()
                }
            }

            recentSoundsVersion++
        }
    }

    fun getRecentSoundIds(): List<String> {
        return synchronized(recentSounds) {
            recentSounds.keys.map(Identifier::toString).asReversed()
        }
    }

    private fun normalizePercent(percent: Int) =
        ((percent.toFloat() / 5).roundToInt() * 5).coerceIn(0, 200)
}