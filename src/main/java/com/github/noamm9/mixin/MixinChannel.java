package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.MonoAudio;
import com.github.noamm9.interfaces.MonoAudioChannel;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public class MixinChannel implements MonoAudioChannel {
    @Shadow @Final private int source;
    @Unique private Vec3 monoAudio$lastPosition = new Vec3(0.0, 0.0, 0.0);
    @Unique private boolean monoAudio$relative;

    @Inject(method = "setSelfPosition", at = @At("HEAD"), cancellable = true)
    private void forceMonoPosition(Vec3 pos, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.enabled) return;
        monoAudio$lastPosition = pos;
        monoAudio$refreshPosition();
        ci.cancel();
    }

    @Inject(method = "setRelative", at = @At("HEAD"), cancellable = true)
    private void forceRelative(boolean relative, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.enabled) return;
        monoAudio$relative = relative;
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        monoAudio$refreshPosition();
        ci.cancel();
    }

    @Override
    public void monoAudio$refreshPosition() {
        if (!MonoAudio.INSTANCE.enabled) return;
        MonoAudio.applyCenteredPosition(
            source,
            monoAudio$relative ? monoAudio$lastPosition.length() : MonoAudio.distanceToListener(monoAudio$lastPosition)
        );
    }
}