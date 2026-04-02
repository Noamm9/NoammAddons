package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import com.github.noamm9.ui.notification.NotificationManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(PoseStack poseStack, float f, CallbackInfo ci) {
        if (this.minecraft.options.damageTiltStrength().get() == 0) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderDeferredSubtitles()V"))
    private void onRenderEnd(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci, @Local GuiGraphics context) {
        NotificationManager.render(context);
    }

    @WrapOperation(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I"))
    private int onFOVChange(Integer instance, Operation<Integer> original) {
        if (!Camera.INSTANCE.enabled) return original.call(instance);
        if (!Camera.getCustomFOV().getValue()) return original.call(instance);
        return Camera.getCustomFOVSlider().getValue();
    }

    @WrapOperation(method = "getProjectionMatrixForCulling", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I"))
    private int onFOVChange2(Integer instance, Operation<Integer> original) {
        if (!Camera.INSTANCE.enabled) return original.call(instance);
        if (!Camera.getCustomFOV().getValue()) return original.call(instance);
        return Camera.getCustomFOVSlider().getValue();
    }
}