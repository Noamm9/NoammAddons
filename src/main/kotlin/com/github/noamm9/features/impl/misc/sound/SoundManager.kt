package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.IFeatureScreen
import com.github.noamm9.ui.gui.SoundManagerScreen
import com.github.noamm9.utils.SoundUtils
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import java.util.*
import kotlin.math.roundToInt

object SoundManager: Feature("Adjust volumes for every sound in the game"), IFeatureScreen {
    private val volumes = PogObject("noammaddons_sounds", mutableMapOf<String, Float>())
    private val recentSounds = Collections.synchronizedSet<Identifier>(mutableSetOf())
    @Volatile var recentSoundsVersion = 0L

    override fun createScreen() = SoundManagerScreen()

    override fun init() {
        volumes.get().replaceAll { _, multiplier -> normalizePercent((multiplier * 100f).roundToInt()) / 100f }

        register<WorldChangeEvent> { recentSounds.clear() }
    }

    @JvmStatic
    fun getMultiplier(id: String) = if (enabled) volumes.get().getOrDefault(id, 1f) else 1f

    @JvmStatic
    fun recordPlayedSound(sound: SoundInstance) {
        if (mc.gui.screen() is SoundManagerScreen) return
        recentSounds.add(sound.identifier)

        if (recentSounds.size > 100) recentSounds.remove(recentSounds.first())
        recentSoundsVersion ++
    }

    fun getVolumePercent(id: String) = (volumes.get().getOrDefault(id, 1f) * 100f).roundToInt()
    fun setVolumePercent(id: String, percent: Int) = volumes.get().set(id, normalizePercent(percent) / 100f)
    fun playPreview(sound: SoundEvent) {
        SoundUtils.playSound(sound, 0.25f, 1f)
    }

    fun getRecentSoundIds() = recentSounds.map(Identifier::toString).asReversed()

    private fun normalizePercent(percent: Int) = ((percent.toFloat() / 5).roundToInt() * 5).coerceIn(0, 200)
}