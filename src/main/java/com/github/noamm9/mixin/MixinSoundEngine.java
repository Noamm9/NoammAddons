package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.MonoAudio;
import com.github.noamm9.interfaces.MonoAudioChannel;
import net.minecraft.client.Camera;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    @Shadow @Final private ChannelAccess channelAccess;

    @Inject(method = "updateSource", at = @At("TAIL"))
    private void refreshMonoChannels(Camera camera, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.enabled) return;

        channelAccess.executeOnChannels(stream ->
            stream.forEach(channel -> ((MonoAudioChannel) channel).monoAudio$refreshPosition())
        );
    }
}
