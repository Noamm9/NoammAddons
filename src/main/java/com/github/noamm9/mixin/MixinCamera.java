package com.github.noamm9.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.features.impl.misc.Camera.*;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private float partialTickTime;
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    @Shadow
    protected abstract void setPosition(double d, double e, double f);


    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void redirectSetPosition(Camera instance, double x, double y, double z) {
        if (INSTANCE.enabled && getLegacySneakHeight().getValue()) {
            float standingHeight = 1.62f;
            float sneakingHeight = 1.27f;

            // 1.54f = Pure 1.8.9
            // 1.27f = Default Modern
            float targetHeight = 1.5f;

            float maxOffset = targetHeight - sneakingHeight;
            float totalCrouchDistance = standingHeight - sneakingHeight;

            float currentEyeHeight = Mth.lerp(this.partialTickTime, this.eyeHeightOld, this.eyeHeight);

            if (currentEyeHeight < standingHeight) {
                double crouchAmount = (standingHeight - currentEyeHeight) / totalCrouchDistance;
                crouchAmount = Math.max(0, Math.min(1, crouchAmount));
                double animatedOffset = crouchAmount * maxOffset;

                this.setPosition(x, y + animatedOffset, z);
                return;
            }
        }

        this.setPosition(x, y, z);
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double setCameraDistance(LivingEntity instance, Holder<Attribute> holder) {
        if (INSTANCE.enabled && getCustomCameraDistance().getValue()) {
            return getCameraDistance().getValue().doubleValue();
        }

        return instance.getAttributeValue(holder);
    }

    //#if CHEAT
    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void overrideCameraPos(Camera instance, double x, double y, double z, Operation<Void> original) {
        com.github.noamm9.features.impl.misc.NoRotate.cameraHook(instance, x, y, z, original);
    }
    //#endif

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float f, CallbackInfoReturnable<Float> cir) {
        if (INSTANCE.enabled && getNoCameraClip().getValue()) {
            cir.setReturnValue(f);
        }
    }
}