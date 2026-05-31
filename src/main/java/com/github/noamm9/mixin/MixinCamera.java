package com.github.noamm9.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void redirectSetPosition(Camera instance, double x, double y, double z, @Local(argsOnly = true) float partialTicks) {
        if (INSTANCE.enabled && getLegacySneakHeight().getValue()) {
            float standingHeight = 1.62f;
            float sneakingHeight = 1.27f;

            // 1.54f = Pure 1.8.9
            // 1.27f = Default Modern
            float targetHeight = 1.5f;

            float maxOffset = targetHeight - sneakingHeight;
            float totalCrouchDistance = standingHeight - sneakingHeight;

            float currentEyeHeight = Mth.lerp(partialTicks, eyeHeightOld, eyeHeight);

            if (currentEyeHeight < standingHeight) {
                double crouchAmount = (standingHeight - currentEyeHeight) / totalCrouchDistance;
                crouchAmount = Math.clamp(crouchAmount, 0, 1);
                double animatedOffset = crouchAmount * maxOffset;

                setPosition(x, y + animatedOffset, z);
                return;
            }
        }

        setPosition(x, y, z);
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double setCameraDistance(LivingEntity instance, Holder<Attribute> attribute) {
        if (INSTANCE.enabled && getCustomCameraDistance().getValue()) {
            return getCameraDistance().getValue().doubleValue();
        }

        return instance.getAttributeValue(attribute);
    }

    //#if CHEAT
    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void overrideCameraPos(Camera instance, double x, double y, double z, Operation<Void> original) {
        com.github.noamm9.features.impl.misc.NoRotate.cameraHook(instance, x, y, z, original);
    }
    //#endif

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float cameraDist, CallbackInfoReturnable<Float> cir) {
        if (INSTANCE.enabled && getNoCameraClip().getValue()) {
            cir.setReturnValue(cameraDist);
        }
    }

    @Inject(method = "calculateFov", at = @At(value = "RETURN"), cancellable = true)
    private void calculateFovHook(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(INSTANCE.enabled && getCustomFOV().getValue() ? getCustomFOVSlider().getValue().floatValue() : cir.getReturnValue());
        ;
    }

    @ModifyExpressionValue(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float getMaxFov(float original) {
        return INSTANCE.enabled && getCustomFOV().getValue() ? getCustomFOVSlider().getValue().floatValue() : original;
    }
}