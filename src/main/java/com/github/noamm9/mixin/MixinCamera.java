package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.features.impl.misc.Camera.*;

@Mixin(Camera.class)
public abstract class MixinCamera {
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
        cir.setReturnValue(INSTANCE.enabled && getCustomFOV().getValue() ? cir.getReturnValue() * noammaddons$getFOVRatio() : cir.getReturnValue());
    }

    @ModifyExpressionValue(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float modifyProjectionFov(float original) {
        return INSTANCE.enabled && getCustomFOV().getValue() ? original * noammaddons$getFOVRatio() : original;
    }

    @Unique
    private float noammaddons$getFOVRatio() {
        // essential zoom changes the fov directly so we divide it to get the scale amount
        return getCustomFOVSlider().getValue().floatValue() / NoammAddons.mc.options.fov().get().floatValue();
    }
}