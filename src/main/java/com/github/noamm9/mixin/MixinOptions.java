package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class MixinOptions {
    @Shadow
    public abstract void setCameraType(CameraType cameraType);

    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void onChangeCameraType(CameraType cameraType, CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getNoFrontCamera().getValue() && cameraType == CameraType.THIRD_PERSON_FRONT) {
            setCameraType(CameraType.FIRST_PERSON);
            ci.cancel();
        }
    }
}