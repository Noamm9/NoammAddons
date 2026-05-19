package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.sound.ArrowHitSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class MixinSoundManager {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (ArrowHitSound.onSoundPlay(sound)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}