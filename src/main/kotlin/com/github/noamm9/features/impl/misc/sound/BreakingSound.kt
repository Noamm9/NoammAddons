package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.config.types.SoundSetting
import com.github.noamm9.features.Feature
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvents
import com.github.noamm9.utils.items.ItemUtils.skyblockId

/**
 * @see com.github.noamm9.mixin.MixinSoundManager
 */
object BreakingSound: Feature() {
    private val soundConfig = createSoundSettings("Sound", SoundEvents.NOTE_BLOCK_HARP.value())

    @JvmStatic
    fun onSoundPlay(soundInstance: SoundInstance): Boolean {
        if (!enabled) return false
        if (mc.player?.mainHandItem?.skyblockId != "DUNGEONBREAKER") return false

        val identifier = soundInstance.identifier

        if (identifier.namespace != "minecraft") return false
        if (!identifier.path.startsWith("block.") || !identifier.path.endsWith(".break")) return false
        if ((getSettingByName("Sound") as? SoundSetting)?.value?.location == identifier) return false

        soundConfig.invoke()
        return true
    }
}
