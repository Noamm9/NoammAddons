package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.MonoAudio;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public class ChannelMixin {
    @Shadow @Final private int source;

    @Inject(method = "setSelfPosition", at = @At("HEAD"), cancellable = true)
    private void forceMonoPosition(Vec3 pos, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.enabled) return;
        ci.cancel();
    }

    @Inject(method = "setRelative", at = @At("HEAD"), cancellable = true)
    private void forceRelative(boolean relative, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.enabled) return;
        AL10.alSourcei(this.source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        ci.cancel();
    }
}