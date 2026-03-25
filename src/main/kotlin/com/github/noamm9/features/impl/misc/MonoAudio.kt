package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import net.minecraft.world.phys.Vec3
import org.lwjgl.openal.AL10

/**
 * @see com.github.noamm9.mixin.MixinChannel
 * @see com.github.noamm9.mixin.MixinSoundEngine
 */
object MonoAudio: Feature("Plays all the game audio through a single channel.") {
    @JvmStatic
    fun distanceToListener(pos: Vec3): Double {
        val listenerPos = mc.soundManager.listenerTransform.position()
        return pos.distanceTo(listenerPos)
    }

    @JvmStatic
    fun applyCenteredPosition(source: Int, distance: Double) {
        AL10.alSourcefv(source, AL10.AL_POSITION, floatArrayOf(0f, 0f, - distance.toFloat()))
    }
}