package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import com.github.noamm9.ui.notification.NotificationManager;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.options.damageTiltStrength().get() == 0) ci.cancel();
    }

    @Inject(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractDeferredSubtitles()V"))
    public void onExtractGui(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local GuiGraphicsExtractor graphics) {
        if (minecraft.screen == null) NotificationManager.render(graphics);
    }

    @ModifyVariable(method = "renderLevel", at = @At("STORE"), name = "nauseaIntensity")
    public float zeroNauseaIntensity(float nauseaIntensity) {
        return Camera.INSTANCE.enabled && Camera.getDisableNausea().getValue() ? 0F : nauseaIntensity;
    }
}