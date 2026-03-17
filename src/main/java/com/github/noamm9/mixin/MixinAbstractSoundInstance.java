package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.sound.SoundManager;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public class MixinAbstractSoundInstance {
    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void onGetVolume(CallbackInfoReturnable<Float> cir) {
        AbstractSoundInstance sound = (AbstractSoundInstance) (Object) this;
        String id = sound.getIdentifier().toString();
        float multiplier = SoundManager.getMultiplier(id);
        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }
}
