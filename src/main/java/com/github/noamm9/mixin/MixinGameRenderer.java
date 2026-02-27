package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.ScreenEvent;
import com.github.noamm9.features.impl.misc.Camera;
import com.github.noamm9.ui.notification.NotificationManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void onGetNightVisionScale(LivingEntity entity, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (Camera.INSTANCE.enabled && Camera.getFullBright().getValue()) {
            cir.setReturnValue(0.0f);
        }
    }

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

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
        )
    )
    private void warpScreenRender(Screen instance, GuiGraphics guiGraphics, int i, int j, float f, Operation<Void> original) {
        if (EventBus.post(new ScreenEvent.PreRender(instance, guiGraphics, i, j))) return;
        original.call(instance, guiGraphics, i, j, f);
        EventBus.post(new ScreenEvent.PostRender(instance, guiGraphics, i, j));
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
